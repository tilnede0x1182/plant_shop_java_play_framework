package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

// ==============================================================================
// Classe principale
// ==============================================================================

/**
 * Test end-to-end Java 21.
 * Dépendance : org.json.
 */
public final class TestE2E {

	// --------------------------------------------------------------------------
	// Configuration
	// --------------------------------------------------------------------------

	/**
	 * Lit les variables d environnement depuis .env.
	 * @return Map Dictionnaire clé-valeur
	 * @throws IOException En cas d erreur lecture
	 */
	private static Map<String, String> env() throws IOException {
		Map<String, String> m = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
			String l;
			while ((l = br.readLine()) != null) {
				int i = l.indexOf('=');
				if (i > 0)
					m.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
			}
		} catch (IOException e) {
			// Ignorer si .env n'existe pas, les valeurs par défaut seront utilisées
		}
		return m;
	}

	private static final Map<String, String> CFG;
	static {
		try {
			CFG = env();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	private static final String PORT = CFG.getOrDefault("SERVER_ADDRESS", "4500");
	private static final String BASE = "http://localhost:" + PORT + "/api";
	private static final String ADMIN_EMAIL = "admin1@planteshop.com";
	private static final String ADMIN_PWD = "password";

	// --------------------------------------------------------------------------
	// Donnees d'instance
	// --------------------------------------------------------------------------

	private final Map<String, String> cookie = new HashMap<>();
	private final String timestamp;

	// --------------------------------------------------------------------------
	// Fonctions utilitaires
	// --------------------------------------------------------------------------

	/**
	 * Constructeur initialisant le timestamp.
	 */
	public TestE2E() {
		this.timestamp = ts();
	}

	/**
	 * Génère un timestamp formaté.
	 * @return String Timestamp
	 */
	private static String ts() {
		return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
	}

	/**
	 * Génère une chaîne aléatoire.
	 * @param n int Longueur
	 * @return String Chaîne aléatoire
	 */
	private static String rand(int n) {
		String a = "abcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb = new StringBuilder();
		Random r = new Random();
		for (int i = 0; i < n; i++)
			sb.append(a.charAt(r.nextInt(a.length())));
		return sb.toString();
	}

	/**
	 * Attend que le serveur soit disponible.
	 * @param host String Hôte
	 * @param port int Port
	 * @param timeoutMs int Timeout en ms
	 * @return boolean True si disponible
	 */
	private static boolean waitForServer(String host, int port, int timeoutMs) {
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < timeoutMs) {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(host, port), 100); // Timeout de connexion de 100ms
				return true;
			} catch (IOException e) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * Effectue un appel HTTP et retourne un objet JSON.
	 * @param m String Méthode HTTP
	 * @param p String Path
	 * @param exp int Code attendu
	 * @param body JSONObject Corps de requête
	 * @param who String Identifiant session
	 * @return JSONObject Réponse
	 * @throws Exception En cas d erreur
	 */
	private JSONObject call(String m, String p, int exp, JSONObject body, String who) throws Exception {
		HttpClient client = HttpClient.newBuilder().build();
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(BASE + p))
				.method(
						m,
						body == null
								? HttpRequest.BodyPublishers.noBody()
								: HttpRequest.BodyPublishers.ofString(body.toString()))
				.header("Content-Type", "application/json");

		if (cookie.get(who) != null) {
			builder.header("Cookie", cookie.get(who));
		}

		HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		int code = response.statusCode();

		// Gestion améliorée des cookies, concatène les nouveaux cookies
		List<String> setCookies = response.headers().allValues("Set-Cookie");
		if (!setCookies.isEmpty()) {
			String currentCookies = cookie.getOrDefault(who, "");
			for (String setCookieHeader : setCookies) {
				String newCookie = setCookieHeader.split(";", 2)[0];
				if (currentCookies.isEmpty()) {
					currentCookies = newCookie;
				} else {
					currentCookies += "; " + newCookie;
				}
			}
			cookie.put(who, currentCookies);
		}

		System.out.printf("%s %-7s %s [%d]%n", code == exp ? "✅" : "❌", m, p, code);

		if (code != exp) {
			throw new RuntimeException("API " + m + " " + p + " -> " + code + " (attendu " + exp + ")\n" + response.body());
		}

		String contentType = response.headers().firstValue("Content-Type").orElse("");
		String txt = response.body().trim();
		if (contentType.startsWith("application/json") || txt.startsWith("{")) {
				return txt.isEmpty() ? new JSONObject() : new JSONObject(txt);
		}
		return new JSONObject();
	}

	/**
	 * Effectue un appel HTTP et retourne un tableau JSON.
	 * @param m String Méthode HTTP
	 * @param p String Path
	 * @param exp int Code attendu
	 * @param body JSONObject Corps de requête
	 * @param who String Identifiant session
	 * @return JSONArray Réponse tableau
	 * @throws Exception En cas d erreur
	 */
	private JSONArray callArray(String m, String p, int exp, JSONObject body, String who) throws Exception {
		// Wrapper pour les réponses qui sont des listes JSON
		HttpClient client = HttpClient.newBuilder().build();
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(BASE + p))
				.method(m, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body.toString()))
				.header("Content-Type", "application/json");

		if (cookie.get(who) != null) builder.header("Cookie", cookie.get(who));

		HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		int code = response.statusCode();

		System.out.printf("%s %-7s %s [%d]%n", code == exp ? "✅" : "❌", m, p, code);

		if (code != exp) {
			throw new RuntimeException("API " + m + " " + p + " -> " + code + " (attendu " + exp + ")\n" + response.body());
		}

		String contentType = response.headers().firstValue("Content-Type").orElse("");
		if (contentType.startsWith("application/json")) {
			String txt = response.body();
			return txt.trim().isEmpty() ? new JSONArray() : new JSONArray(txt);
		}
		return new JSONArray();
	}

	// --------------------------------------------------------------------------
	// Helpers authentification
	// --------------------------------------------------------------------------

	/**
	 * Effectue une connexion.
	 * @param mail String Email
	 * @param pw String Mot de passe
	 * @param who String Identifiant session
	 * @throws Exception En cas d erreur
	 */
	private void login(String mail, String pw, String who) throws Exception {
		JSONObject j = new JSONObject().put("email", mail).put("password", pw);
		call("POST", "/auth/login", 201, j, who);
	}

	/**
	 * Effectue une inscription.
	 * @param name String Nom
	 * @param mail String Email
	 * @param pw String Mot de passe
	 * @param who String Identifiant session
	 * @throws Exception En cas d erreur
	 */
	private void register(String name, String mail, String pw, String who) throws Exception {
		JSONObject j = new JSONObject().put("name", name).put("email", mail).put("password", pw);
		call("POST", "/auth/register", 201, j, who);
	}

	// --------------------------------------------------------------------------
	// Assertions
	// --------------------------------------------------------------------------

	/**
	 * Vérifie l'égalité d'une valeur JSON.
	 * @param o JSONObject Objet source
	 * @param k String Clé
	 * @param e Object Valeur attendue
	 */
	private static void assert_eq(JSONObject o, String k, Object e) {
		if (!o.has(k)) {
			System.out.printf("❌   ↳ Clé '%s' manquante dans l'objet JSON%n", k);
			throw new RuntimeException("Objet vide – clé " + k + " recherchée");
		}
		Object a = o.get(k);
		boolean ok;
		if (e instanceof Number && a instanceof Number) {
			ok = ((Number) e).doubleValue() == ((Number) a).doubleValue();
		} else {
			ok = e.equals(a);
		}

		System.out.printf("%s   ↳ %s=%s%n (attendu %s)%n",
				ok ? "✅" : "❌",
				k,
				new JSONObject().put("v", a).get("v").toString(), // Pour un affichage correct des strings
				new JSONObject().put("v", e).get("v").toString());

		if (!ok) {
			throw new RuntimeException("Assertion échouée pour la clé '" + k + "'");
		}
	}

	/**
	 * Vérifie qu une clé contient un nombre.
	 * @param o JSONObject Objet source
	 * @param k String Clé
	 */
	private static void assert_num(JSONObject o, String k) {
		if (!(o.opt(k) instanceof Number)) {
			throw new RuntimeException("Clé " + k + " n'est pas numérique ou absente");
		}
	}

	// ==========================================================================
	// Modules de test
	// ==========================================================================

	// --------------------------------------------------------------------------
	// Test Plants
	// --------------------------------------------------------------------------

	/**
	 * Module de test pour les plantes.
	 * @throws Exception En cas d erreur
	 */
	private void test_plants() throws Exception {
		System.out.println("\n📌 TEST MODULE: PLANTS (admin)");
		JSONObject plant_data = new JSONObject()
				.put("name", "Test Plant")
				.put("price", 10)
				.put("stock", 5);
		JSONObject plant = call("POST", "/admin/plants", 201, plant_data, "admin");
		assert_num(plant, "id");
		int id = plant.getInt("id");
		JSONObject get = call("GET", "/plants/" + id, 200, null, "admin");
		assert_eq(get, "name", plant_data.get("name"));
		JSONObject price_update = new JSONObject().put("price", 15);
		call("PATCH", "/admin/plants/" + id, 200, price_update, "admin");
		JSONObject check = call("GET", "/plants/" + id, 200, null, "admin");
		assert_eq(check, "price", 15);
		System.out.printf("   ↳ name=%s%n", check.getString("name"));
		call("DELETE", "/admin/plants/" + id, 200, null, "admin");
	}

	// --------------------------------------------------------------------------
	// Test Users
	// --------------------------------------------------------------------------

	/**
	 * Module de test pour les utilisateurs.
	 * @throws Exception En cas d erreur
	 */
	private void test_users() throws Exception {
		System.out.println("\n📌 TEST MODULE: USERS (admin)");
		String email = "utilisateur_test_" + this.timestamp + "@example.com";
		JSONObject user_data = new JSONObject()
				.put("email", email)
				.put("name", "Utilisateur de test")
				.put("password", "pass123");
		JSONObject user = call("POST", "/users", 201, user_data, "admin");
		int id = user.getInt("id");
		JSONObject name_update = new JSONObject().put("name", "Tester Update");
		call("PATCH", "/users/" + id, 200, name_update, "admin");
		JSONObject get = call("GET", "/users/" + id, 200, null, "admin");
		assert_eq(get, "name", "Tester Update");
		call("DELETE", "/users/" + id, 200, null, "admin");
	}

	// --------------------------------------------------------------------------
	// Test Orders
	// --------------------------------------------------------------------------

	/**
	 * Module de test pour les commandes.
	 * @throws Exception En cas d erreur
	 */
	private void test_orders() throws Exception {
		System.out.println("\n📌 TEST MODULE: ORDERS & ORDER ITEMS");
		String plantName = "Plante_de_test_" + this.timestamp;
		JSONObject plant_data = new JSONObject()
				.put("name", plantName)
				.put("price", 10)
				.put("stock", 5);
		JSONObject plant = call("POST", "/admin/plants", 201, plant_data, "admin");
		assert_num(plant, "id");
		int pid = plant.getInt("id");

		JSONObject item = new JSONObject().put("plantId", pid).put("quantity", 2);
		JSONObject order_data = new JSONObject().put("items", new JSONArray().put(item));
		JSONObject order = call("POST", "/orders", 201, order_data, "user");
		assert_num(order, "id");
		int oid = order.getInt("id");

		JSONObject status_update = new JSONObject().put("status", "shipped");
		call("PATCH", "/orders/" + oid, 200, status_update, "admin");

		JSONArray list = callArray("GET", "/orders", 200, null, "user");
		JSONObject found = null;
		for (int i = 0; i < list.length(); i++) {
			JSONObject o = list.getJSONObject(i);
			if (o.getInt("id") == oid) {
				found = o;
				break;
			}
		}
		if (found == null) throw new RuntimeException("Commande absente");

		assert_eq(found, "status", "shipped");
		if (!found.has("orderItems") || found.getJSONArray("orderItems").isEmpty()) {
			throw new RuntimeException("Items absents dans la commande");
		}
		JSONObject nestedPlant = found.getJSONArray("orderItems").getJSONObject(0).getJSONObject("plant");
		assert_eq(nestedPlant, "name", plantName);

		call("DELETE", "/orders/" + oid, 200, null, "admin");
		call("DELETE", "/admin/plants/" + pid, 200, null, "admin");
	}

	// --------------------------------------------------------------------------
	// Test User Profile
	// --------------------------------------------------------------------------

	/**
	 * Module de test pour le profil utilisateur.
	 * @param email String Email de l utilisateur
	 * @throws Exception En cas d erreur
	 */
	private void test_user_profile(String email) throws Exception {
		System.out.println("\n📌 TEST MODULE: USER PROFILE (user)");
		JSONArray users = callArray("GET", "/users", 200, null, "admin");
		JSONObject user_obj = null;
		for (int i = 0; i < users.length(); i++) {
			JSONObject u = users.getJSONObject(i);
			if (u.getString("email").equals(email)) {
				user_obj = u;
				break;
			}
		}
		if (user_obj == null) throw new RuntimeException("Utilisateur de test non trouvé");
		int uid = user_obj.getInt("id");

		JSONObject profile = call("GET", "/users/" + uid, 200, null, "user");
		assert_eq(profile, "id", uid);

		String new_name = "Utilisateur_de_test_" + this.timestamp;
		JSONObject name_update = new JSONObject().put("name", new_name);
		call("PATCH", "/users/" + uid, 200, name_update, "user");

		JSONObject updated = call("GET", "/users/" + uid, 200, null, "user");
		assert_eq(updated, "name", new_name);

		JSONObject admin_update = new JSONObject().put("admin", true);
		call("PATCH", "/users/" + uid, 200, admin_update, "user"); // L'API doit ignorer ce champ

		JSONObject check = call("GET", "/users/" + uid, 200, null, "admin");
		assert_eq(check, "admin", false); // Vérification que l'utilisateur n'est pas devenu admin
	}

	// --------------------------------------------------------------------------
	// Test Auth Roles
	// --------------------------------------------------------------------------

	/**
	 * Module de test pour les rôles et permissions.
	 * @throws Exception En cas d erreur
	 */
	private void test_auth_roles() throws Exception {
		System.out.println("\n📌 TEST MODULE: ROLES");
		JSONObject bad_plant = new JSONObject().put("name", "Bad").put("price", 1).put("stock", 1);
		call("POST", "/admin/plants", 403, bad_plant, "user");

		JSONObject good_plant = new JSONObject().put("name", "Good").put("price", 1).put("stock", 1);
		JSONObject plant = call("POST", "/admin/plants", 201, good_plant, "admin");
		int pid = plant.getInt("id");
		call("DELETE", "/admin/plants/" + pid, 200, null, "admin");

		call("GET", "/users", 403, null, "user");
	}

	// --------------------------------------------------------------------------
	// Test Admin Plants
	// --------------------------------------------------------------------------

	/**
	 * Module de test admin pour les plantes.
	 * @throws Exception En cas d erreur
	 */
	private void test_admin_plants() throws Exception {
		System.out.println("\n📌 TEST MODULE: ADMIN PLANTS");
		JSONArray plantes = callArray("GET", "/admin/plants", 200, null, "admin");
		System.out.printf("   ↳ %d plantes récupérées%n", plantes.length());

		JSONObject plant_data = new JSONObject()
				.put("name", "Plante_admin_" + this.timestamp)
				.put("price", 99)
				.put("stock", 12);
		JSONObject p = call("POST", "/admin/plants", 201, plant_data, "admin");
		int id = p.getInt("id");

		JSONObject price_update = new JSONObject().put("price", 123);
		call("PATCH", "/admin/plants/" + id, 200, price_update, "admin");
		call("DELETE", "/admin/plants/" + id, 200, null, "admin");
	}

	// --------------------------------------------------------------------------
	// Test Admin Users
	// --------------------------------------------------------------------------

	/**
	 * Module de test admin pour les utilisateurs.
	 * @throws Exception En cas d erreur
	 */
	private void test_admin_users() throws Exception {
		System.out.println("\n📌 TEST MODULE: ADMIN USERS");
		String email = "admin_temp_" + this.timestamp + "@example.com";
		String name = "Admin Temporaire " + this.timestamp;

		JSONObject temp_admin_data = new JSONObject()
				.put("email", email)
				.put("name", name)
				.put("password", "password")
				.put("admin", true);
		JSONObject temp = call("POST", "/users", 201, temp_admin_data, "admin");
		int id = temp.getInt("id");

		JSONArray list = callArray("GET", "/admin/users", 200, null, "admin");
		JSONObject cible = null;
		for (int i = 0; i < list.length(); i++) {
			JSONObject u = list.getJSONObject(i);
			if (u.getString("email").equals(email)) {
				cible = u;
				break;
			}
		}
		if (cible == null) throw new RuntimeException("L'admin temporaire n'a pas été trouvé dans la liste !");
		assert_eq(cible, "name", name);

		String nouveau_nom = "Admin_temp_modifié_" + this.timestamp;
		JSONObject name_update = new JSONObject().put("name", nouveau_nom);
		call("PATCH", "/users/" + id, 200, name_update, "admin");

		JSONObject user_get = call("GET", "/users/" + id, 200, null, "admin");
		assert_eq(user_get, "name", nouveau_nom);

		call("DELETE", "/users/" + id, 200, null, "admin");
	}

	// --------------------------------------------------------------------------
	// Test Auth Me
	// --------------------------------------------------------------------------

	/**
	 * Module de test pour endpoint /auth/me.
	 * @throws Exception En cas d erreur
	 */
	private void test_auth_me() throws Exception {
		System.out.println("\n📌 TEST MODULE: AUTH /me");
		JSONObject me = call("GET", "/auth/me", 200, null, "user");
		String mail = me.getString("email");
		String nom = me.getString("name");
		assert_eq(me, "email", mail);
		assert_eq(me, "name", nom);
		System.out.printf("   ↳ Utilisateur connecté: %s (%s)%n", mail, nom);
	}

	// ==========================================================================
	// Main
	// ==========================================================================

	/**
	 * Point d'entrée des tests.
	 * @param args String[] Arguments CLI
	 */
	public static void main(String[] args) {
		try {
			if (!waitForServer("127.0.0.1", Integer.parseInt(PORT), 5000)) {
				System.err.println("❌ Serveur http://localhost:" + PORT + " injoignable");
				System.exit(2);
			}

			TestE2E t = new TestE2E();

			String random_tag = rand(4);
			String userEmail = "utilisateur_de_test_" + t.timestamp + "_" + random_tag + "@example.com";
			String userPassword = "pass123";

			System.out.println("🧪 Démarrage des tests: http://localhost:" + PORT + "/api\n");

			// Connexion des utilisateurs de base pour les tests
			t.login(ADMIN_EMAIL, ADMIN_PWD, "admin");
			t.register("User", userEmail, userPassword, "user"); // Utilise un nom générique pour l'enregistrement
			t.login(userEmail, userPassword, "user");

			// Exécution des suites de tests
			t.test_plants();
			t.test_users();
			t.test_orders();
			t.test_user_profile(userEmail);
			t.test_auth_roles();
			t.test_admin_plants();
			t.test_admin_users();
			t.test_auth_me();

			System.out.println("\n🎉 Tous les tests ont réussi!");
			System.exit(0);

		} catch (Exception e) {
			System.err.println("\n❌ Tests interrompus: " + e.getMessage());
			// e.printStackTrace(); // Décommenter pour un débogage détaillé
			System.exit(1);
		}
	}
}
