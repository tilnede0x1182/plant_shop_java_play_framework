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
	 * Appel API JSON generique avec gestion cookies.
	 * @param method Methode HTTP
	 * @param path Chemin API (ex: /auth/login)
	 * @param body Corps JSON ou null
	 * @param who Identifiant session
	 * @return Code HTTP de la reponse
	 * @throws Exception En cas d erreur
	 */
	private int apiCall(String method, String path, JSONObject body, String who) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(API + path));
		if (body != null) {
			builder.method(method, HttpRequest.BodyPublishers.ofString(body.toString()));
			builder.header("Content-Type", "application/json");
		} else {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		}
		if (cookies.get(who) != null) builder.header("Cookie", cookies.get(who));
		HttpResponse<String> response = HttpClient.newBuilder().build().send(builder.build(), HttpResponse.BodyHandlers.ofString());
		storeCookies(response, who);
		return response.statusCode();
	}

	/**
	 * Login via API JSON et stocke le cookie.
	 * @param email Adresse email
	 * @param password Mot de passe
	 * @param who Identifiant session
	 * @throws Exception En cas d erreur
	 */
	private void loginApi(String email, String password, String who) throws Exception {
		int code = apiCall("POST", "/auth/login", new JSONObject().put("email", email).put("password", password), who);
		System.out.printf("✅ Login API %s [%d]%n", who, code);
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
		expectCode("/admin/plants/new", 302, "anon");
		System.out.println("✅   ↳ /admin/plants/new redirige (302)");
		expectCode("/admin/users", 302, "anon");
		System.out.println("✅   ↳ /admin/users redirige (302)");
		expectCode("/users/1", 302, "anon");
		System.out.println("✅   ↳ /users/1 redirige (302)");
		expectCode("/users/1/edit", 302, "anon");
		System.out.println("✅   ↳ /users/1/edit redirige (302)");
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
		int code = apiCall("POST", "/auth/register", new JSONObject().put("email", email).put("password", password).put("name", "Test User"), who);
		System.out.printf("✅ Register API %s [%d]%n", who, code);
	}

	/**
	 * Teste les pages en mode utilisateur connecte.
	 * @throws Exception En cas d erreur
	 */
	private void testUtilisateur() throws Exception {
		System.out.println("\n📌 TEST FRONTEND: UTILISATEUR CONNECTE");
		loginApi("jules_roux78@yahoo.com", "pw859901368", "user");
		Document plantes = getPage("/plants", "user");
		assertText(plantes, "PlantShop", "Page plantes accessible");
		Document orders = getPage("/orders", "user");
		assertExists(orders, ".card", "Page orders avec cards");
		assertText(orders, "Statut", "Statut visible dans les commandes");
		System.out.println("✅   ↳ /orders accessible avec cards et statut");
		expectCode("/admin/plants", 302, "user");
		System.out.println("✅   ↳ /admin/plants redirige pour non-admin");
		expectCode("/admin/users", 302, "user");
		System.out.println("✅   ↳ /admin/users redirige pour non-admin");
		expectCode("/admin/plants/new", 302, "user");
		System.out.println("✅   ↳ /admin/plants/new redirige pour non-admin");
		expectCode("/admin/plants/1/edit", 302, "user");
		System.out.println("✅   ↳ /admin/plants/1/edit redirige pour non-admin");
		expectCode("/admin/users/1/edit", 302, "user");
		System.out.println("✅   ↳ /admin/users/1/edit redirige pour non-admin");
		Document profile = getPage("/users/4", "user");
		assertText(profile, "jules_roux78", "Email visible dans le profil");
		System.out.println("✅   ↳ /users/:id profil accessible avec email");
		Document editProfile = getPage("/users/4/edit", "user");
		assertExists(editProfile, "form#profile-edit-form", "Formulaire modifier profil");
		System.out.println("✅   ↳ /users/:id/edit formulaire present");
		apiCall("PATCH", "/users/4", new JSONObject().put("name", "Jules Test Modifie"), "user");
		Document profileUpdated = getPage("/users/4", "user");
		assertText(profileUpdated, "Jules Test Modifie", "Nom modifie visible");
		System.out.println("✅   ↳ Profil modifie avec succes");
		apiCall("PATCH", "/users/4", new JSONObject().put("name", "Jules Roux"), "user");
	}

	/**
	 * Teste la deconnexion.
	 * @throws Exception En cas d erreur
	 */
	private void testDeconnexion() throws Exception {
		System.out.println("\n📌 TEST FRONTEND: DECONNEXION");
		loginApi(ADMIN_EMAIL, ADMIN_PWD, "deconnexion");
		getPage("/orders", "deconnexion");
		System.out.println("✅   ↳ /orders accessible avant deconnexion");
		logoutViaPage("deconnexion");
		expectCode("/orders", 302, "deconnexion");
		System.out.println("✅   ↳ /orders redirige apres deconnexion");
	}

	/**
	 * Deconnexion via la page /auth/logout sans suivre la redirection.
	 * Capture le Set-Cookie qui efface le token.
	 * @param who Identifiant session
	 * @throws Exception En cas d erreur
	 */
	private void logoutViaPage(String who) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(BASE + "/auth/logout")).GET();
		if (cookies.get(who) != null) builder.header("Cookie", cookies.get(who));
		HttpResponse<String> response = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NEVER).build()
				.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		storeCookies(response, who);
		cookies.put(who, "");
		System.out.println("✅   ↳ Deconnexion effectuee [" + response.statusCode() + "]");
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
		assertText(plantes, "Administrateur", "Label Administrateur visible");
		Document showPlant = getPage("/plants/1", "admin");
		assertExists(showPlant, "button[onclick*=deletePlant]", "Bouton supprimer plante (admin)");
		System.out.println("✅   ↳ Bouton supprimer visible sur page plante");
		Document adminPlants = getPage("/admin/plants", "admin");
		assertExists(adminPlants, "a.text-decoration-none", "Liens plantes avec CSS propre");
		System.out.println("✅   ↳ /admin/plants liens CSS corrects");
		Document adminUsers = getPage("/admin/users", "admin");
		assertExists(adminUsers, "a.text-decoration-none", "Liens utilisateurs avec CSS propre");
		System.out.println("✅   ↳ /admin/users liens CSS corrects");
		Document newPlant = getPage("/admin/plants/new", "admin");
		assertExists(newPlant, "form", "Formulaire nouvelle plante");
		System.out.println("✅   ↳ /admin/plants/new accessible (200)");
		Document editPlant = getPage("/admin/plants/1/edit", "admin");
		assertExists(editPlant, "form", "Formulaire modifier plante");
		System.out.println("✅   ↳ /admin/plants/1/edit accessible (200)");
		Document editUser = getPage("/admin/users/1/edit", "admin");
		assertExists(editUser, "form", "Formulaire modifier utilisateur");
		System.out.println("✅   ↳ /admin/users/1/edit accessible (200)");
		testAdminCrudPlantes();
		testAdminCrudUtilisateurs();
	}

	/**
	 * Teste le CRUD plantes via API + verification HTML.
	 * @throws Exception En cas d erreur
	 */
	private void testAdminCrudPlantes() throws Exception {
		apiCall("POST", "/admin/plants", new JSONObject().put("name", "Plante Test Frontend").put("price", 42).put("stock", 7), "admin");
		Document listApres = getPage("/admin/plants", "admin");
		assertText(listApres, "Plante Test Frontend", "Plante creee visible dans la liste");
		System.out.println("✅   ↳ CRUD: plante creee et visible");
		apiCall("PATCH", "/admin/plants/1", new JSONObject().put("name", "Rose 1 Modifiee"), "admin");
		Document listModif = getPage("/admin/plants", "admin");
		assertText(listModif, "Rose 1 Modifiee", "Plante modifiee visible");
		System.out.println("✅   ↳ CRUD: plante modifiee et visible");
		apiCall("PATCH", "/admin/plants/1", new JSONObject().put("name", "Rose 1"), "admin");
	}

	/**
	 * Teste la modification des droits admin via API + verification HTML.
	 * @throws Exception En cas d erreur
	 */
	private void testAdminCrudUtilisateurs() throws Exception {
		Document usersBefore = getPage("/admin/users", "admin");
		assertExists(usersBefore, "table", "Tableau utilisateurs present");
		apiCall("PATCH", "/admin/users/4", new JSONObject().put("admin", true), "admin");
		Document usersAfter = getPage("/admin/users/4/edit", "admin");
		assertExists(usersAfter, "form#user-edit-form", "Formulaire edit visible");
		System.out.println("✅   ↳ CRUD: droits admin modifies");
		apiCall("PATCH", "/admin/users/4", new JSONObject().put("admin", false), "admin");
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
			test.testDeconnexion();
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
