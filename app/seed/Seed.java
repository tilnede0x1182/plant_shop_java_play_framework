package seed;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import at.favre.lib.crypto.bcrypt.BCrypt;

/** Seed aligné sur la version C++ : noms réalistes, descriptions, prix cohérents,
    décrémentation du stock, génération users.txt                                     */
public final class Seed {

	/**
	 * Lit la configuration BDD depuis conf/application.conf.
	 * @return Map avec DATABASE_URL, DATABASE_USER, DATABASE_PASS
	 * @throws IOException En cas d erreur lecture
	 */
	private static Map<String,String> env() throws IOException {
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
	 * Extrait la valeur apres le signe egal, sans guillemets.
	 * @param line Ligne de configuration
	 * @return Valeur nettoyee
	 */
	private static String extractValue(String line) {
		int idx = line.indexOf('=');
		if (idx < 0) return "";
		return line.substring(idx + 1).trim().replace("\"", "");
	}

	/* ---------- Constantes ---------- */
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

	/**
	 * Génère un entier aléatoire dans un intervalle.
	 * @param min int Borne min
	 * @param max int Borne max
	 * @return int Entier aléatoire
	 */
	private static int rnd(int min,int max){ return min + RNG.nextInt(max - min + 1); }
	/**
	 * Sélectionne un élément aléatoire dans un tableau.
	 * @param arr T[] Tableau source
	 * @return T Élément choisi
	 */
	private static <T> T pick(T[] arr){ return arr[rnd(0,arr.length-1)]; }
	/**
	 * Génère un mot de passe aléatoire.
	 * @return String Mot de passe
	 */
	private static String randPwd(){ return "pw" + rnd(100000000,999999999); }
	/**
	 * Hash un mot de passe avec BCrypt.
	 * @param p String Mot de passe
	 * @return String Hash BCrypt
	 */
	private static String hash(String p){ return BCrypt.withDefaults().hashToString(12, p.toCharArray()); }

	/**
	 * Génère une phrase lorem ipsum.
	 * @return String Phrase générée
	 */
	private static String loremSentence() {
		String[] words = {"lorem","ipsum","dolor","sit","amet","consectetur","adipiscing","elit",
				"sed","do","eiusmod","tempor","incididunt","ut","labore","et","dolore","magna","aliqua"};
		int n = rnd(10,14);
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<n;i++){
			String w = words[rnd(0,words.length-1)];
			sb.append(i==0? Character.toUpperCase(w.charAt(0))+w.substring(1): w);
			sb.append(i==n-1?'.':' ');
		}
		return sb.toString();
	}

	/**
	 * Point d entrée du script de seed.
	 * @param args String[] Arguments CLI
	 * @throws Exception En cas d erreur
	 */
	public static void main(String[] args) throws Exception {

		Map<String,String> cfg = env();
		Connection db = DriverManager.getConnection(
				cfg.get("DATABASE_URL"), cfg.get("DATABASE_USER"), cfg.get("DATABASE_PASS")
		);

		/* Nettoyage */
		System.out.println("🧹 Nettoyage de la base de données…");
		try(Statement st=db.createStatement()){
			st.execute("TRUNCATE order_items,orders,plants,users RESTART IDENTITY CASCADE");
		}
		System.out.println("✅ Base vidée.");

		/* ---------- Users ---------- */
		PreparedStatement insUser = db.prepareStatement(
			"INSERT INTO users(name,email,password_hash,is_admin) VALUES (?,?,?,?)",
			Statement.RETURN_GENERATED_KEYS);

		List<Integer> adminIds = new ArrayList<>();
		List<Integer> userIds  = new ArrayList<>();
		List<String>  credsOut = new ArrayList<>();
		credsOut.add("Administrateurs :\n");

		// Admins
		System.out.println("👑 Création des administrateurs…");
		for(int i=0;i<NB_ADMINS;i++){
			String name = pick(FIRST)+" "+pick(LAST);
			String email = "admin"+(i+1)+"@planteshop.com";
			String pwd = "password";
			insUser.setString(1,name);
			insUser.setString(2,email);
			insUser.setString(3,hash(pwd));
			insUser.setBoolean(4,true);
			insUser.executeUpdate();
			try(ResultSet rs=insUser.getGeneratedKeys()){ rs.next(); adminIds.add(rs.getInt(1)); }
			credsOut.add(email+" "+pwd);
		}
		System.out.println("✅ "+adminIds.size()+" admins.");

		credsOut.add("");
		credsOut.add("Utilisateurs :\n");

		// Users
		System.out.println("👥 Création des utilisateurs…");
		for(int i=0;i<NB_USERS;i++){
			String first = pick(FIRST), last = pick(LAST);
			String email = first.toLowerCase()+"_"+last.toLowerCase()+rnd(20,99)+"@"+pick(EMAIL_DOMAINS);
			String pwd = randPwd();
			String name = first+" "+last;
			insUser.setString(1,name);
			insUser.setString(2,email);
			insUser.setString(3,hash(pwd));
			insUser.setBoolean(4,false);
			insUser.executeUpdate();
			try(ResultSet rs=insUser.getGeneratedKeys()){ rs.next(); userIds.add(rs.getInt(1)); }
			credsOut.add(email+" "+pwd);
		}
		System.out.println("✅ "+userIds.size()+" utilisateurs.");

		/* ---------- Plants ---------- */
		PreparedStatement insPlant = db.prepareStatement(
			"INSERT INTO plants(name,description,price,stock) VALUES (?,?,?,?)",
			Statement.RETURN_GENERATED_KEYS);

		System.out.println("🌱 Création des plantes…");
		/** Classe interne pour stocker les infos de plante. */
		class PlantInfo{ int id,price,stock; PlantInfo(int id,int p,int s){this.id=id;price=p;stock=s;} }
		List<PlantInfo> plants = new ArrayList<>();

		for(int i=0;i<NB_PLANTS;i++){
			String base = PLANT_NAMES[i % PLANT_NAMES.length];
			String name = NB_PLANTS>PLANT_NAMES.length ? base+" "+(i/PLANT_NAMES.length+1): base;
			int price = rnd(5,50);
			int stock = rnd(5,30);
			insPlant.setString(1,name);
			insPlant.setString(2,loremSentence());
			insPlant.setBigDecimal(3,new BigDecimal(price));
			insPlant.setInt(4,stock);
			insPlant.executeUpdate();
			try(ResultSet rs=insPlant.getGeneratedKeys()){
				rs.next();
				int id = rs.getInt(1);
				plants.add(new PlantInfo(id,price,stock));
			}
		}
		System.out.println("✅ "+plants.size()+" plantes.");

		/* ---------- Orders & items ---------- */
		PreparedStatement insOrder = db.prepareStatement(
				"INSERT INTO orders(user_id,total,status) VALUES (?,?,?)",
				Statement.RETURN_GENERATED_KEYS);
		PreparedStatement insItem = db.prepareStatement(
			"INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES (?,?,?,?)");
		PreparedStatement updPlantStock = db.prepareStatement(
			"UPDATE plants SET stock = stock - ? WHERE id = ?");

		String[] statusArr = {"confirmed","pending","shipped","delivered"};
		int totalOrders = 0;

		System.out.println("🛒 Création des commandes…");
		for(Integer uid : userIds){
			int nb = rnd(0,MAX_ORDERS_PER_USER);
			for(int k=0;k<nb;k++){
				insOrder.setInt(1,uid);
				insOrder.setBigDecimal(2,BigDecimal.ZERO); // placeholder
				insOrder.setString(3,statusArr[rnd(0,3)]);
				insOrder.executeUpdate();
				int orderId;
				try(ResultSet rs=insOrder.getGeneratedKeys()){ rs.next(); orderId = rs.getInt(1); }

				BigDecimal total = BigDecimal.ZERO;
				for(int it=0;it<2;it++){
					// sélection plante avec stock >0
					List<PlantInfo> avail = plants.stream().filter(p->p.stock>0).toList();
					if(avail.isEmpty()) break;
					PlantInfo p = avail.get(rnd(0,avail.size()-1));
					int qty = Math.min(rnd(1,5),p.stock);
					insItem.setInt(1,orderId);
					insItem.setInt(2,p.id);
					insItem.setInt(3,qty);
					insItem.setBigDecimal(4,new BigDecimal(p.price));
					insItem.executeUpdate();
					// stock --
					p.stock -= qty;
					updPlantStock.setInt(1,qty);
					updPlantStock.setInt(2,p.id);
					updPlantStock.executeUpdate();
					total = total.add(new BigDecimal(p.price*qty));
				}
				try(PreparedStatement up=db.prepareStatement("UPDATE orders SET total=? WHERE id=?")){
					up.setBigDecimal(1,total);
					up.setInt(2,orderId);
					up.executeUpdate();
				}
				totalOrders++;
			}
		}
		System.out.println("✅ "+totalOrders+" commandes.");

		/* ---------- users.txt ---------- */
		try(PrintWriter pw=new PrintWriter("users.txt")){
			credsOut.forEach(pw::println);
		}
		System.out.println("✍️ Fichier users.txt généré ("+credsOut.size()+" lignes).");

		db.close();
		System.out.println("🎉 Seed terminée !");
	}
}
