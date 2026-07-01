package test;

// ==============================================================================
// Importations
// ==============================================================================

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

// ==============================================================================
// Classe principale
// ==============================================================================

/**
 * Test e2e frontend Play Framework (Twirl + jsoup).
 * Verifie le HTML retourne par les pages Twirl.
 */
public final class E2EFrontend {

	// --------------------------------------------------------------------------
	// Donnees
	// --------------------------------------------------------------------------

	private static final Map<String, String> CFG;
	static {
		try { CFG = readEnvFile(); }
		catch (Exception envError) { throw new RuntimeException(envError); }
	}
	private static final String PORT = CFG.getOrDefault("SERVER_ADDRESS", "4500");
	private static final String BASE = "http://localhost:" + PORT;
	private static final String API = BASE + "/api";
	private static final String ADMIN_EMAIL = "admin1@planteshop.com";
	private static final String ADMIN_PWD = "password";
	private final Map<String, String> cookies = new HashMap<>();

	// --------------------------------------------------------------------------
	// Fonctions utilitaires - Env
	// --------------------------------------------------------------------------

	/**
	 * Lit les variables depuis .env.
	 * @return Map des cles-valeurs
	 * @throws IOException En cas d erreur lecture
	 */
	private static Map<String, String> readEnvFile() throws IOException {
		Map<String, String> envMap = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				int idx = line.indexOf('=');
				if (idx > 0) envMap.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
			}
		} catch (IOException ignored) { }
		return envMap;
	}

	/**
	 * Attend que le serveur soit disponible.
	 * @param host Hote
	 * @param port Port
	 * @param timeoutMs Timeout en ms
	 * @return true si disponible
	 */
	private static boolean waitForServer(String host, int port, int timeoutMs) {
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < timeoutMs) {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(host, port), 100);
				return true;
			} catch (IOException socketError) {
				try { Thread.sleep(100); }
				catch (InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------
	// Fonctions utilitaires - HTTP
	// --------------------------------------------------------------------------

	/**
	 * Login via API JSON et stocke le cookie.
	 * @param email Adresse email
	 * @param password Mot de passe
	 * @param who Identifiant session
	 * @throws Exception En cas d erreur
	 */
	private void loginApi(String email, String password, String who) throws Exception {
		JSONObject body = new JSONObject().put("email", email).put("password", password);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(API + "/auth/login"))
				.POST(HttpRequest.BodyPublishers.ofString(body.toString()))
				.header("Content-Type", "application/json").build();
		HttpResponse<String> response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
		storeCookies(response, who);
		System.out.printf("✅ Login API %s [%d]%n", who, response.statusCode());
	}

	/**
	 * Stocke les cookies Set-Cookie dans le jar.
	 * @param response Reponse HTTP
	 * @param who Identifiant session
	 */
	private void storeCookies(HttpResponse<String> response, String who) {
		List<String> setCookies = response.headers().allValues("Set-Cookie");
		String current = cookies.getOrDefault(who, "");
		for (String header : setCookies) {
			String cookie = header.split(";", 2)[0];
			current = current.isEmpty() ? cookie : current + "; " + cookie;
		}
		cookies.put(who, current);
	}

	/**
	 * GET une page HTML et retourne le Document jsoup.
	 * @param path Chemin relatif
	 * @param who Identifiant session
	 * @return Document jsoup
	 * @throws Exception En cas d erreur
	 */
	private Document getPage(String path, String who) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(BASE + path)).GET();
		if (cookies.get(who) != null) builder.header("Cookie", cookies.get(who));
		HttpResponse<String> response = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL).build()
				.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 500) throw new RuntimeException("Page " + path + " -> " + response.statusCode());
		return Jsoup.parse(response.body());
	}

	/**
	 * GET une page et verifie le code HTTP sans suivre les redirections.
	 * @param path Chemin relatif
	 * @param expectedCode Code HTTP attendu
	 * @param who Identifiant session
	 * @throws Exception En cas d erreur
	 */
	private void expectCode(String path, int expectedCode, String who) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(BASE + path)).GET();
		if (cookies.get(who) != null) builder.header("Cookie", cookies.get(who));
		HttpResponse<String> response = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NEVER).build()
				.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		int code = response.statusCode();
		System.out.printf("%s GET %s [%d]%n", code == expectedCode ? "✅" : "❌", path, code);
		if (code != expectedCode) throw new RuntimeException("GET " + path + " -> " + code + " (attendu " + expectedCode + ")");
	}

	// --------------------------------------------------------------------------
	// Fonctions utilitaires - Assertions HTML
	// --------------------------------------------------------------------------

	/**
	 * Verifie qu un element existe dans le document.
	 * @param doc Document jsoup
	 * @param selector Selecteur CSS
	 * @param label Description pour le log
	 */
	private void assertExists(Document doc, String selector, String label) {
		boolean found = !doc.select(selector).isEmpty();
		System.out.printf("%s   ↳ %s%n", found ? "✅" : "❌", label);
		if (!found) throw new RuntimeException("Element introuvable : " + selector);
	}

	/**
	 * Verifie qu un texte est present dans le document.
	 * @param doc Document jsoup
	 * @param text Texte a chercher
	 * @param label Description pour le log
	 */
	private void assertText(Document doc, String text, String label) {
		boolean found = doc.text().contains(text);
		System.out.printf("%s   ↳ %s%n", found ? "✅" : "❌", label);
		if (!found) throw new RuntimeException("Texte introuvable : " + text);
	}

	// ==========================================================================
	// Modules de test
	// ==========================================================================

	// --------------------------------------------------------------------------
	// Phase 1 - Visiteur
	// --------------------------------------------------------------------------

	/**
	 * Teste les pages en mode visiteur.
	 * @throws Exception En cas d erreur
	 */
	private void testVisiteur() throws Exception {
		System.out.println("\n📌 TEST FRONTEND: VISITEUR");
		Document accueil = getPage("/", "anon");
		assertExists(accueil, "nav", "Navbar presente");
		assertExists(accueil, ".custom-navbar", "Navbar custom-navbar");
		Document plantes = getPage("/plants", "anon");
		assertExists(plantes, ".card", "Cards plantes");
		assertText(plantes, "PlantShop", "Titre PlantShop");
		Document signin = getPage("/auth/signin", "anon");
		assertExists(signin, "form", "Formulaire connexion");
		assertExists(signin, "input[type=email]", "Champ email");
		assertExists(signin, "input[type=password]", "Champ password");
		Document register = getPage("/auth/register", "anon");
		assertExists(register, "form", "Formulaire inscription");
		expectCode("/orders", 302, "anon");
		System.out.println("✅   ↳ /orders redirige (302)");
		expectCode("/admin/plants", 302, "anon");
		System.out.println("✅   ↳ /admin/plants redirige (302)");
	}

	// --------------------------------------------------------------------------
	// Phase 2 - Utilisateur connecte
	// --------------------------------------------------------------------------

	/**
	 * Inscrit un utilisateur de test via l API register.
	 * @param email Email
	 * @param password Mot de passe
	 * @param who Identifiant session
	 * @throws Exception En cas d erreur
	 */
	private void registerApi(String email, String password, String who) throws Exception {
		JSONObject body = new JSONObject().put("email", email).put("password", password).put("name", "Test User");
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(API + "/auth/register"))
				.POST(HttpRequest.BodyPublishers.ofString(body.toString()))
				.header("Content-Type", "application/json").build();
		HttpResponse<String> response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
		storeCookies(response, who);
		System.out.printf("✅ Register API %s [%d]%n", who, response.statusCode());
	}

	/**
	 * Teste les pages en mode utilisateur connecte.
	 * @throws Exception En cas d erreur
	 */
	private void testUtilisateur() throws Exception {
		System.out.println("\n📌 TEST FRONTEND: UTILISATEUR CONNECTE");
		String userEmail = "frontend_test_" + System.currentTimeMillis() + "@example.com";
		registerApi(userEmail, "pass123", "user");
		loginApi(userEmail, "pass123", "user");
		Document plantes = getPage("/plants", "user");
		assertText(plantes, "PlantShop", "Page plantes accessible");
		Document orders = getPage("/orders", "user");
		assertExists(orders, "body", "Page orders accessible");
		System.out.println("✅   ↳ /orders accessible (200)");
		expectCode("/admin/plants", 302, "user");
		System.out.println("✅   ↳ /admin/plants redirige pour non-admin");
	}

	// --------------------------------------------------------------------------
	// Phase 3 - Admin
	// --------------------------------------------------------------------------

	/**
	 * Teste les pages en mode admin.
	 * @throws Exception En cas d erreur
	 */
	private void testAdmin() throws Exception {
		System.out.println("\n📌 TEST FRONTEND: ADMIN");
		loginApi(ADMIN_EMAIL, ADMIN_PWD, "admin");
		Document plantes = getPage("/plants", "admin");
		assertText(plantes, "Admin", "Dropdown Admin visible");
		Document adminPlants = getPage("/admin/plants", "admin");
		assertExists(adminPlants, "body", "Page admin plantes");
		System.out.println("✅   ↳ /admin/plants accessible (200)");
		Document adminUsers = getPage("/admin/users", "admin");
		assertExists(adminUsers, "body", "Page admin users");
		System.out.println("✅   ↳ /admin/users accessible (200)");
	}

	// --------------------------------------------------------------------------
	// Phase 4 - Zero erreur
	// --------------------------------------------------------------------------

	/**
	 * Verifie qu aucune page ne retourne 500.
	 * @throws Exception En cas d erreur
	 */
	private void testZeroErreur() throws Exception {
		System.out.println("\n📌 TEST FRONTEND: ZERO ERREUR");
		String[] pagesPubliques = {"/", "/plants", "/cart", "/auth/signin", "/auth/register"};
		for (String path : pagesPubliques) {
			getPage(path, "anon");
			System.out.printf("✅   ↳ %s -> OK%n", path);
		}
		String[] pagesAdmin = {"/orders", "/plants", "/admin/plants", "/admin/users"};
		for (String path : pagesAdmin) {
			getPage(path, "admin");
			System.out.printf("✅   ↳ %s (admin) -> OK%n", path);
		}
	}

	// ==========================================================================
	// Main
	// ==========================================================================

	/**
	 * Point d entree des tests frontend.
	 * @param args Arguments CLI
	 */
	public static void main(String[] args) {
		try {
			if (!waitForServer("127.0.0.1", Integer.parseInt(PORT), 5000)) {
				System.err.println("❌ Serveur " + BASE + " injoignable");
				System.exit(2);
			}
			E2EFrontend test = new E2EFrontend();
			System.out.println("🧪 Demarrage tests frontend: " + BASE + "\n");
			test.testVisiteur();
			test.testUtilisateur();
			test.testAdmin();
			test.testZeroErreur();
			System.out.println("\n🎉 Tous les tests frontend ont reussi!");
			System.exit(0);
		} catch (Exception testException) {
			System.err.println("\n❌ Tests frontend interrompus: " + testException.getMessage());
			System.exit(1);
		}
	}
}
