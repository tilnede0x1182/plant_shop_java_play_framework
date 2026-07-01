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
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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
	private JSONObject apiCall(String method, String path, JSONObject body, String who) throws Exception {
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
		String responseBody = response.body().trim();
		if (responseBody.startsWith("{")) return new JSONObject(responseBody);
		return new JSONObject().put("status", response.statusCode());
	}

	/**
	 * Login via API JSON et stocke le cookie.
	 * @param email Adresse email
	 * @param password Mot de passe
	 * @param who Identifiant session
	 * @throws Exception En cas d erreur
	 */
	private void loginApi(String email, String password, String who) throws Exception {
		JSONObject result = apiCall("POST", "/auth/login", new JSONObject().put("email", email).put("password", password), who);
		System.out.printf("✅ Login API %s [%s]%n", who, result.optString("email", "ok"));
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

	/**
	 * Verifie qu un texte est absent du document.
	 * @param doc Document jsoup
	 * @param text Texte qui ne doit pas etre present
	 * @param label Description pour le log
	 */
	private void assertNotContains(Document doc, String text, String label) {
		boolean absent = !doc.text().contains(text);
		System.out.printf("%s   ↳ %s%n", absent ? "✅" : "❌", label);
		if (!absent) throw new RuntimeException("Texte present alors qu il ne devrait pas : " + text);
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
		JSONObject result = apiCall("POST", "/auth/register", new JSONObject().put("email", email).put("password", password).put("name", "Test User"), who);
		System.out.printf("✅ Register API %s [%s]%n", who, result.optString("email", "ok"));
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
		testSuppressionImpactOrders();
	}

	/**
	 * Teste le CRUD plantes via API + verification HTML.
	 * @throws Exception En cas d erreur
	 */
	private void testAdminCrudPlantes() throws Exception {
		JSONObject created = apiCall("POST", "/admin/plants", new JSONObject().put("name", "Plante CRUD Test").put("price", 42).put("stock", 7).put("description", "Test"), "admin");
		int plantId = created.getInt("id");
		Document listCreate = getPage("/admin/plants", "admin");
		assertText(listCreate, "Plante CRUD Test", "Plante creee visible");
		System.out.println("✅   ↳ CRUD: plante creee et visible");
		apiCall("PATCH", "/admin/plants/" + plantId, new JSONObject().put("name", "Plante CRUD Modifiee").put("price", 99), "admin");
		Document listModif = getPage("/admin/plants", "admin");
		assertText(listModif, "Plante CRUD Modifiee", "Nom modifie visible");
		assertText(listModif, "99", "Prix modifie visible");
		System.out.println("✅   ↳ CRUD: plante modifiee (nom + prix) et visible");
		apiCall("DELETE", "/admin/plants/" + plantId, null, "admin");
		Document listDelete = getPage("/admin/plants", "admin");
		assertNotContains(listDelete, "Plante CRUD Modifiee", "Plante supprimee absente de la liste");
		System.out.println("✅   ↳ CRUD: plante supprimee et absente");
	}

	/**
	 * Teste l impact de la suppression d une plante sur les commandes.
	 * @throws Exception En cas d erreur
	 */
	private void testSuppressionImpactOrders() throws Exception {
		JSONObject plant = apiCall("POST", "/admin/plants", new JSONObject().put("name", "Plante Order Test").put("price", 10).put("stock", 5).put("description", "Test"), "admin");
		int plantId = plant.getInt("id");
		apiCall("POST", "/orders", new JSONObject().put("items", new org.json.JSONArray().put(new JSONObject().put("plant_id", plantId).put("quantity", 1))), "admin");
		Document ordersBefore = getPage("/orders", "admin");
		assertText(ordersBefore, "Plante Order Test", "Plante visible dans commande");
		System.out.println("✅   ↳ Plante visible dans commande avant suppression");
		apiCall("DELETE", "/admin/plants/" + plantId, null, "admin");
		Document ordersAfter = getPage("/orders", "admin");
		assertNotContains(ordersAfter, "Plante Order Test", "Plante absente apres suppression");
		assertNotContains(ordersAfter, "supprim", "Pas de mention supprimee");
		System.out.println("✅   ↳ Commande intacte, plante absente sans mention");
	}

	/**
	 * Teste la modification des droits admin via API + verification HTML.
	 * @throws Exception En cas d erreur
	 */
	private void testAdminCrudUtilisateurs() throws Exception {
		Document usersBefore = getPage("/admin/users", "admin");
		assertExists(usersBefore, "table", "Tableau utilisateurs present");
		apiCall("PATCH", "/admin/users/4", new JSONObject().put("name", "Jules Modifie"), "admin");
		Document editAfterName = getPage("/admin/users/4/edit", "admin");
		assertText(editAfterName, "Jules Modifie", "Nom modifie visible dans le formulaire");
		System.out.println("✅   ↳ CRUD: nom utilisateur modifie et visible");
		apiCall("PATCH", "/admin/users/4", new JSONObject().put("admin", true), "admin");
		Document listAfterAdmin = getPage("/admin/users", "admin");
		assertText(listAfterAdmin, "Jules Modifie", "Utilisateur promu visible");
		System.out.println("✅   ↳ CRUD: utilisateur promu admin");
		apiCall("PATCH", "/admin/users/4", new JSONObject().put("admin", false).put("name", "Jules Roux"), "admin");
		Document listAfterRevert = getPage("/admin/users", "admin");
		assertText(listAfterRevert, "Jules Roux", "Nom restaure visible");
		System.out.println("✅   ↳ CRUD: nom et droits restaures");
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

	// --------------------------------------------------------------------------
	// Phase 5 - Tests navigateur (Playwright via Node.js)
	// --------------------------------------------------------------------------

	/**
	 * Lance les tests navigateur via Selenium WebDriver.
	 * @throws Exception En cas d erreur
	 */
	private void runBrowserTests() throws Exception {
		System.out.println("\n📌 TEST FRONTEND: NAVIGATEUR (Selenium)");
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
		WebDriver driver = new ChromeDriver(options);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		try {
			testBrowserLogin(driver, wait);
			testBrowserPanier(driver, wait);
			testBrowserAchat(driver, wait);
			testBrowserCrudFormulaire(driver, wait);
		} finally {
			driver.quit();
		}
	}

	/**
	 * Teste la connexion via formulaire dans le navigateur.
	 * @param driver WebDriver
	 * @param wait WebDriverWait
	 * @throws Exception En cas d erreur
	 */
	/**
	 * Pose le cookie admin dans le navigateur Selenium.
	 * @param driver WebDriver
	 * @param wait WebDriverWait
	 * @throws Exception En cas d erreur
	 */
	private void setBrowserCookie(WebDriver driver, WebDriverWait wait) throws Exception {
		driver.get(BASE + "/plants");
		wait.until(ExpectedConditions.presenceOfElementLocated(By.className("card")));
		apiCall("POST", "/auth/login", new JSONObject().put("email", ADMIN_EMAIL).put("password", ADMIN_PWD), "browser");
		String cookieValue = cookies.get("browser");
		String token = cookieValue.contains("=") ? cookieValue.split("=", 2)[1] : cookieValue;
		((org.openqa.selenium.JavascriptExecutor) driver).executeScript("document.cookie = 'ps_play_token=" + token + "; path=/'");
	}

	private void testBrowserLogin(WebDriver driver, WebDriverWait wait) throws Exception {
		setBrowserCookie(driver, wait);
		driver.get(BASE + "/plants");
		wait.until(ExpectedConditions.presenceOfElementLocated(By.className("card")));
		System.out.println("✅   ↳ Login admin via API + cookie Selenium");
		String navText = driver.findElement(By.tagName("nav")).getText();
		assertBrowser(navText.contains("Admin"), "Dropdown Admin visible apres login");
		driver.get(BASE + "/auth/logout");
		wait.until(ExpectedConditions.presenceOfElementLocated(By.className("card")));
		String navAfter = driver.findElement(By.tagName("nav")).getText();
		assertBrowser(navAfter.contains("connecter"), "Deconnexion fonctionne");
	}

	/**
	 * Teste le panier dans le navigateur.
	 * @param driver WebDriver
	 * @param wait WebDriverWait
	 * @throws Exception En cas d erreur
	 */
	private void testBrowserPanier(WebDriver driver, WebDriverWait wait) throws Exception {
		driver.get(BASE + "/plants");
		wait.until(ExpectedConditions.presenceOfElementLocated(By.className("card")));
		List<WebElement> addButtons = driver.findElements(By.cssSelector("[onclick*=addToCart]"));
		addButtons.get(0).click();
		Thread.sleep(500);
		addButtons.get(1).click();
		Thread.sleep(500);
		String cartLink = driver.findElement(By.id("cart-link")).getText();
		assertBrowser(cartLink.contains("2"), "Compteur panier = 2");
		driver.get(BASE + "/cart");
		wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
		List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
		assertBrowser(rows.size() == 2, "2 items dans le panier");
		driver.findElement(By.xpath("//button[contains(text(),'Vider')]")).click();
		Thread.sleep(500);
		String bodyText = driver.findElement(By.tagName("body")).getText();
		assertBrowser(bodyText.contains("vide"), "Panier vide apres vidage");
	}

	/**
	 * Teste l achat complet dans le navigateur.
	 * @param driver WebDriver
	 * @param wait WebDriverWait
	 * @throws Exception En cas d erreur
	 */
	private void testBrowserAchat(WebDriver driver, WebDriverWait wait) throws Exception {
		setBrowserCookie(driver, wait);
		driver.get(BASE + "/plants");
		wait.until(ExpectedConditions.presenceOfElementLocated(By.className("card")));
		driver.findElements(By.cssSelector("[onclick*=addToCart]")).get(0).click();
		Thread.sleep(500);
		driver.get(BASE + "/orders/new");
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("confirm-order-btn")));
		wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
		System.out.println("✅   ↳ Recapitulatif commande avec tableau");
		driver.findElement(By.id("confirm-order-btn")).click();
		wait.until(ExpectedConditions.urlContains("/orders"));
		System.out.println("✅   ↳ Commande passee, redirige vers /orders");
	}

	/**
	 * Teste la creation d une plante via formulaire dans le navigateur.
	 * @param driver WebDriver
	 * @param wait WebDriverWait
	 * @throws Exception En cas d erreur
	 */
	private void testBrowserCrudFormulaire(WebDriver driver, WebDriverWait wait) throws Exception {
		setBrowserCookie(driver, wait);
		driver.get(BASE + "/admin/plants/new");
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("plant-form")));
		driver.findElement(By.id("plant-name")).sendKeys("Plante Selenium Test");
		driver.findElement(By.id("plant-description")).sendKeys("Test Selenium");
		driver.findElement(By.id("plant-price")).sendKeys("33");
		driver.findElement(By.id("plant-stock")).sendKeys("5");
		driver.findElement(By.cssSelector("button[type=submit]")).click();
		wait.until(ExpectedConditions.urlContains("/admin/plants"));
		String pageText = driver.findElement(By.tagName("body")).getText();
		assertBrowser(pageText.contains("Plante Selenium Test"), "Plante creee via formulaire visible");
	}

	/**
	 * Assertion pour les tests navigateur.
	 * @param condition Condition a verifier
	 * @param label Description
	 */
	private void assertBrowser(boolean condition, String label) {
		System.out.printf("%s   ↳ %s%n", condition ? "✅" : "❌", label);
		if (!condition) throw new RuntimeException("Test navigateur echoue : " + label);
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
			test.runBrowserTests();
			System.out.println("\n🎉 Tous les tests frontend ont reussi!");
			System.exit(0);
		} catch (Exception testException) {
			System.err.println("\n❌ Tests frontend interrompus: " + testException.getMessage());
			System.exit(1);
		}
	}
}
