package seed;

// ==============================================================================
// Importations
// ==============================================================================

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import at.favre.lib.crypto.bcrypt.BCrypt;

// ==============================================================================
// Classe Seed
// ==============================================================================

/**
 *	Seed aligné sur la version C++ : noms réalistes, descriptions, prix cohérents,
 *	décrémentation du stock, génération users.txt.
 */
public final class Seed {

	// ------------------------------------------------------------------------------
	// Configuration
	// ------------------------------------------------------------------------------

	/**
	 *	Lit la configuration BDD depuis conf/application.conf.
	 *
	 *	@return Map avec DATABASE_URL, DATABASE_USER, DATABASE_PASS
	 *	@throws IOException En cas d erreur lecture
	 */
	private static Map<String, String> env() throws IOException {
		Map<String,String> out = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader("conf/application.conf"))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("db.default.url")) {
					out.put("DATABASE_URL", extractValue(line));
				} else if (line.startsWith("db.default.username")) {
					out.put("DATABASE_USER", extractValue(line));
				} else if (line.startsWith("db.default.password")) {
					out.put("DATABASE_PASS", extractValue(line));
				}
			}
		}
		return out;
	}

	/**
	 *	Extrait la valeur apres le signe egal, sans guillemets.
	 *
	 *	@param line Ligne de configuration
	 *	@return Valeur nettoyee
	 */
	private static String extractValue(String line) {
		int idx = line.indexOf('=');
		if (idx < 0) return "";
		return line.substring(idx + 1).trim().replace("\"", "");
	}

	// ------------------------------------------------------------------------------
	// Donnees
	// ------------------------------------------------------------------------------

	private static final int NB_ADMINS = 3;
	private static final int NB_USERS  = 30;
	private static final int NB_PLANTS = 60;
	private static final int MAX_ORDERS_PER_USER = 7;

	private static final String[] PLANT_NAMES = {
		"Rose","Tulipe","Lavande","Orchidée","Basilic","Menthe","Pivoine","Tournesol",
		"Cactus (Echinopsis)","Bambou","Camomille (Matricaria recutita)","Sauge (Salvia officinalis)",
		"Romarin (Rosmarinus officinalis)","Thym (Thymus vulgaris)","Laurier-rose (Nerium oleander)",
		"Aloe vera","Jasmin (Jasminum officinale)","Hortensia (Hydrangea macrophylla)",
		"Marguerite (Leucanthemum vulgare)","Géranium (Pelargonium graveolens)",
		"Fuchsia (Fuchsia magellanica)","Anémone (Anemone coronaria)","Azalée (Rhododendron simsii)",
		"Chrysanthème (Chrysanthemum morifolium)","Digitale pourpre (Digitalis purpurea)",
		"Glaïeul (Gladiolus hortulanus)","Lys (Lilium candidum)","Violette (Viola odorata)",
		"Muguet (Convallaria majalis)","Iris (Iris germanica)","Lavandin (Lavandula intermedia)",
		"Érable du Japon (Acer palmatum)","Citronnelle (Cymbopogon citratus)","Pin parasol (Pinus pinea)",
		"Cyprès (Cupressus sempervirens)","Olivier (Olea europaea)","Papyrus (Cyperus papyrus)",
		"Figuier (Ficus carica)","Eucalyptus (Eucalyptus globulus)","Acacia (Acacia dealbata)",
		"Bégonia (Begonia semperflorens)","Calathea (Calathea ornata)","Dieffenbachia (Dieffenbachia seguine)",
		"Ficus elastica","Sansevieria (Sansevieria trifasciata)","Philodendron (Philodendron scandens)",
		"Yucca (Yucca elephantipes)","Zamioculcas zamiifolia","Monstera deliciosa",
		"Pothos (Epipremnum aureum)","Agave (Agave americana)","Cactus raquette (Opuntia ficus-indica)"
	};

	private static final String[] FIRST = {
		"Alice","Bruno","Cathy","David","Emma","Franck",
		"Gwen","Hugo","Inès","Jules","Katia","Léo"
	};
	private static final String[] LAST = {
		"Dupont","Martin","Bernard","Petit","Robert","Richard","Durand","Moreau","Roux","Fournier"
	};
	private static final String[] EMAIL_DOMAINS = {"gmail.com","yahoo.com","hotmail.com"};

	private static final Random RNG = new Random();

	// ------------------------------------------------------------------------------
	// Fonctions utilitaires
	// ------------------------------------------------------------------------------

	/**
	 *	Génère un entier aléatoire dans un intervalle.
	 *
	 *	@param min Borne minimale
	 *	@param max Borne maximale
	 *	@return Entier aléatoire entre min et max inclus
	 */
	private static int rnd(int min, int max) {
		return min + RNG.nextInt(max - min + 1);
	}

	/**
	 *	Sélectionne un élément aléatoire dans un tableau.
	 *
	 *	@param arr Tableau source
	 *	@return Élément choisi aléatoirement
	 */
	private static <T> T pick(T[] arr) {
		return arr[rnd(0, arr.length - 1)];
	}

	/**
	 *	Génère un mot de passe aléatoire.
	 *
	 *	@return Mot de passe de 11 caractères
	 */
	private static String randPwd() {
		return "pw" + rnd(100000000, 999999999);
	}

	/**
	 *	Hash un mot de passe avec BCrypt.
	 *
	 *	@param pwd Mot de passe en clair
	 *	@return Hash BCrypt
	 */
	private static String hash(String pwd) {
		return BCrypt.withDefaults().hashToString(12, pwd.toCharArray());
	}

	/**
	 *	Génère une phrase lorem ipsum.
	 *
	 *	@return Phrase de 10 a 14 mots
	 */
	private static String loremSentence() {
		String[] words = {
			"lorem", "ipsum", "dolor", "sit", "amet", "consectetur",
			"adipiscing", "elit", "sed", "do", "eiusmod", "tempor",
			"incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua"
		};
		int count = rnd(10, 14);
		StringBuilder sb = new StringBuilder();
		for (int idx = 0; idx < count; idx++) {
			String word = words[rnd(0, words.length - 1)];
			if (idx == 0) {
				sb.append(Character.toUpperCase(word.charAt(0)));
				sb.append(word.substring(1));
			} else {
				sb.append(word);
			}
			sb.append(idx == count - 1 ? '.' : ' ');
		}
		return sb.toString();
	}

	// ------------------------------------------------------------------------------
	// Classe PlantInfo
	// ------------------------------------------------------------------------------

	/**
	 *	Classe interne pour stocker les infos de plante.
	 */
	private static class PlantInfo {
		int id, price, stock;
		PlantInfo(int id, int price, int stock) {
			this.id = id;
			this.price = price;
			this.stock = stock;
		}
	}

	// ------------------------------------------------------------------------------
	// Fonctions de seed
	// ------------------------------------------------------------------------------

	/**
	 *	Vide toutes les tables.
	 *
	 *	@param db Connexion BDD
	 *	@throws SQLException En cas d erreur SQL
	 */
	private static void resetDatabase(Connection db) throws SQLException {
		System.out.println("🧹 Nettoyage de la base de données…");
		try (Statement st = db.createStatement()) {
			st.execute("TRUNCATE order_items,orders,plants,users RESTART IDENTITY CASCADE");
		}
		System.out.println("✅ Base vidée.");
	}

	/**
	 *	Cree les administrateurs.
	 *
	 *	@param insUser PreparedStatement pour insertion
	 *	@param adminIds Liste des IDs admins
	 *	@param credsOut Liste des credentials
	 *	@throws SQLException En cas d erreur SQL
	 */
	private static void createAdmins(PreparedStatement insUser, List<Integer> adminIds, List<String> credsOut) throws SQLException {
		System.out.println("👑 Création des administrateurs…");
		for (int idx = 0; idx < NB_ADMINS; idx++) {
			String name = pick(FIRST) + " " + pick(LAST);
			String email = "admin" + (idx + 1) + "@planteshop.com";
			String pwd = "password";
			insUser.setString(1, name);
			insUser.setString(2, email);
			insUser.setString(3, hash(pwd));
			insUser.setBoolean(4, true);
			insUser.executeUpdate();
			try (ResultSet rs = insUser.getGeneratedKeys()) {
				rs.next();
				adminIds.add(rs.getInt(1));
			}
			credsOut.add(email + " " + pwd);
		}
		System.out.println("✅ " + adminIds.size() + " admins.");
	}

	/**
	 *	Cree les utilisateurs.
	 *
	 *	@param insUser PreparedStatement pour insertion
	 *	@param userIds Liste des IDs users
	 *	@param credsOut Liste des credentials
	 *	@throws SQLException En cas d erreur SQL
	 */
	private static void createUsers(PreparedStatement insUser, List<Integer> userIds, List<String> credsOut) throws SQLException {
		System.out.println("👥 Création des utilisateurs…");
		for (int idx = 0; idx < NB_USERS; idx++) {
			String first = pick(FIRST), last = pick(LAST);
			String email = first.toLowerCase() + "_" + last.toLowerCase() + rnd(20, 99) + "@" + pick(EMAIL_DOMAINS);
			String pwd = randPwd();
			String name = first + " " + last;
			insUser.setString(1, name);
			insUser.setString(2, email);
			insUser.setString(3, hash(pwd));
			insUser.setBoolean(4, false);
			insUser.executeUpdate();
			try (ResultSet rs = insUser.getGeneratedKeys()) {
				rs.next();
				userIds.add(rs.getInt(1));
			}
			credsOut.add(email + " " + pwd);
		}
		System.out.println("✅ " + userIds.size() + " utilisateurs.");
	}

	/**
	 *	Cree les plantes.
	 *
	 *	@param insPlant PreparedStatement pour insertion
	 *	@param plants Liste des plantes creees
	 *	@throws SQLException En cas d erreur SQL
	 */
	private static void createPlants(PreparedStatement insPlant, List<PlantInfo> plants) throws SQLException {
		System.out.println("🌱 Création des plantes…");
		for (int idx = 0; idx < NB_PLANTS; idx++) {
			String base = PLANT_NAMES[idx % PLANT_NAMES.length];
			String name = NB_PLANTS > PLANT_NAMES.length ? base + " " + (idx / PLANT_NAMES.length + 1) : base;
			int price = rnd(5, 50);
			int stock = rnd(5, 30);
			insPlant.setString(1, name);
			insPlant.setString(2, loremSentence());
			insPlant.setBigDecimal(3, new BigDecimal(price));
			insPlant.setInt(4, stock);
			insPlant.executeUpdate();
			try (ResultSet rs = insPlant.getGeneratedKeys()) {
				rs.next();
				plants.add(new PlantInfo(rs.getInt(1), price, stock));
			}
		}
		System.out.println("✅ " + plants.size() + " plantes.");
	}

	/**
	 *	Cree les commandes pour tous les utilisateurs.
	 *
	 *	@param db Connexion BDD
	 *	@param userIds Liste des IDs users
	 *	@param plants Liste des plantes
	 *	@return Nombre de commandes creees
	 *	@throws SQLException En cas d erreur SQL
	 */
	private static int createOrders(Connection db, List<Integer> userIds, List<PlantInfo> plants) throws SQLException {
		PreparedStatement insOrder = db.prepareStatement(
			"INSERT INTO orders(user_id,total,status) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
		PreparedStatement insItem = db.prepareStatement(
			"INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES (?,?,?,?)");
		PreparedStatement updStock = db.prepareStatement("UPDATE plants SET stock = stock - ? WHERE id = ?");
		String[] statusArr = {"confirmed", "pending", "shipped", "delivered"};
		int totalOrders = 0;
		System.out.println("🛒 Création des commandes…");
		for (Integer uid : userIds) {
			totalOrders += createOrdersForUser(db, uid, plants, insOrder, insItem, updStock, statusArr);
		}
		System.out.println("✅ " + totalOrders + " commandes.");
		return totalOrders;
	}

	/**
	 *	Cree les commandes pour un utilisateur.
	 *
	 *	@param db Connexion BDD
	 *	@param uid ID utilisateur
	 *	@param plants Liste des plantes
	 *	@param insOrder PreparedStatement pour insertion commande
	 *	@param insItem PreparedStatement pour insertion item
	 *	@param updStock PreparedStatement pour update stock
	 *	@param statusArr Tableau des statuts
	 *	@return Nombre de commandes creees
	 *	@throws SQLException En cas d erreur SQL
	 */
	private static int createOrdersForUser(Connection db, int uid, List<PlantInfo> plants,
			PreparedStatement insOrder, PreparedStatement insItem, PreparedStatement updStock, String[] statusArr) throws SQLException {
		int count = 0;
		int nb = rnd(0, MAX_ORDERS_PER_USER);
		for (int idx = 0; idx < nb; idx++) {
			insOrder.setInt(1, uid);
			insOrder.setBigDecimal(2, BigDecimal.ZERO);
			insOrder.setString(3, statusArr[rnd(0, 3)]);
			insOrder.executeUpdate();
			int orderId;
			try (ResultSet rs = insOrder.getGeneratedKeys()) {
				rs.next();
				orderId = rs.getInt(1);
			}
			BigDecimal total = addOrderItems(plants, orderId, insItem, updStock);
			try (PreparedStatement up = db.prepareStatement("UPDATE orders SET total=? WHERE id=?")) {
				up.setBigDecimal(1, total);
				up.setInt(2, orderId);
				up.executeUpdate();
			}
			count++;
		}
		return count;
	}

	/**
	 *	Ajoute des items a une commande.
	 *
	 *	@param plants Liste des plantes
	 *	@param orderId ID de la commande
	 *	@param insItem PreparedStatement pour insertion item
	 *	@param updStock PreparedStatement pour update stock
	 *	@return Total de la commande
	 *	@throws SQLException En cas d erreur SQL
	 */
	private static BigDecimal addOrderItems(List<PlantInfo> plants, int orderId,
			PreparedStatement insItem, PreparedStatement updStock) throws SQLException {
		BigDecimal total = BigDecimal.ZERO;
		for (int iter = 0; iter < 2; iter++) {
			List<PlantInfo> avail = plants.stream().filter(p -> p.stock > 0).toList();
			if (avail.isEmpty()) break;
			PlantInfo plant = avail.get(rnd(0, avail.size() - 1));
			int qty = Math.min(rnd(1, 5), plant.stock);
			insItem.setInt(1, orderId);
			insItem.setInt(2, plant.id);
			insItem.setInt(3, qty);
			insItem.setBigDecimal(4, new BigDecimal(plant.price));
			insItem.executeUpdate();
			plant.stock -= qty;
			updStock.setInt(1, qty);
			updStock.setInt(2, plant.id);
			updStock.executeUpdate();
			total = total.add(new BigDecimal(plant.price * qty));
		}
		return total;
	}

	/**
	 *	Ecrit le fichier users.txt.
	 *
	 *	@param credsOut Liste des credentials
	 *	@throws FileNotFoundException En cas d erreur fichier
	 */
	private static void writeUsersFile(List<String> credsOut) throws FileNotFoundException {
		try (PrintWriter pw = new PrintWriter("users.txt")) {
			credsOut.forEach(pw::println);
		}
		System.out.println("✍️ Fichier users.txt généré (" + credsOut.size() + " lignes).");
	}

	// ------------------------------------------------------------------------------
	// Main
	// ------------------------------------------------------------------------------

	/**
	 *	Point d entree du script de seed.
	 *
	 *	@param args Arguments CLI
	 *	@throws Exception En cas d erreur
	 */
	public static void main(String[] args) throws Exception {
		Map<String, String> cfg = env();
		Connection db = DriverManager.getConnection(cfg.get("DATABASE_URL"), cfg.get("DATABASE_USER"), cfg.get("DATABASE_PASS"));
		resetDatabase(db);
		PreparedStatement insUser = db.prepareStatement(
			"INSERT INTO users(name,email,password_hash,is_admin) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
		List<Integer> adminIds = new ArrayList<>();
		List<Integer> userIds = new ArrayList<>();
		List<String> credsOut = new ArrayList<>();
		credsOut.add("Administrateurs :\n");
		createAdmins(insUser, adminIds, credsOut);
		credsOut.add("");
		credsOut.add("Utilisateurs :\n");
		createUsers(insUser, userIds, credsOut);
		PreparedStatement insPlant = db.prepareStatement(
			"INSERT INTO plants(name,description,price,stock) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
		List<PlantInfo> plants = new ArrayList<>();
		createPlants(insPlant, plants);
		createOrders(db, userIds, plants);
		writeUsersFile(credsOut);
		db.close();
		System.out.println("🎉 Seed terminée !");
	}
}
