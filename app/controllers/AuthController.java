package controllers;

// ==============================================================================
// Importations
// ==============================================================================

import com.fasterxml.jackson.databind.JsonNode;
import models.User;
import play.libs.Json;
import play.mvc.*;
import security.AuthAction;

import java.util.Optional;

// ==============================================================================
// Controleur Auth
// ==============================================================================

/**
 *	Controleur d'authentification : login, register, logout, me.
 */
public class AuthController extends Controller {

	// ------------------------------------------------------------------------------
	// Register
	// ------------------------------------------------------------------------------

	/**
	 *	Cree un nouveau compte utilisateur.
	 *	POST /api/auth/register
	 *
	 *	@param request Requete HTTP avec body JSON {email, password, name}
	 *	@return 201 avec le user cree ou 400 si erreur
	 */
	public Result register(Http.Request request) {
		JsonNode body = request.body().asJson();
		if (body == null) return badRequest("Body JSON requis");
		String email = body.path("email").asText("");
		String password = body.path("password").asText("");
		String name = body.path("name").asText("");
		if (email.isEmpty() || password.isEmpty()) {
			return badRequest("Email et password requis");
		}
		if (User.find.query().where().eq("email", email).findCount() > 0) {
			return badRequest("Email deja utilise");
		}
		User user = buildUser(email, password, name, false);
		user.save();
		String token = AuthAction.generateToken(user.getId(), false);
		return created(userToJson(user))
			.withCookies(AuthAction.buildCookie(token));
	}

	// ------------------------------------------------------------------------------
	// Login
	// ------------------------------------------------------------------------------

	/**
	 *	Authentifie un utilisateur existant.
	 *	POST /api/auth/login
	 *
	 *	@param request Requete HTTP avec body JSON {email, password}
	 *	@return 200 avec le user ou 401 si invalide
	 */
	public Result login(Http.Request request) {
		JsonNode body = request.body().asJson();
		if (body == null) return unauthorized("Body JSON requis");
		String email = body.path("email").asText("");
		String password = body.path("password").asText("");
		User user = User.find.query().where().eq("email", email).findOne();
		if (user == null) return unauthorized("Identifiants invalides");
		if (!AuthAction.checkPassword(password, user.getPasswordHash())) {
			return unauthorized("Identifiants invalides");
		}
		String token = AuthAction.generateToken(user.getId(), user.isAdmin());
		return created(userToJson(user))
			.withCookies(AuthAction.buildCookie(token));
	}

	// ------------------------------------------------------------------------------
	// Logout
	// ------------------------------------------------------------------------------

	/**
	 *	Deconnecte l'utilisateur en supprimant le cookie.
	 *	POST /api/auth/logout
	 *
	 *	@return 200 OK
	 */
	public Result logout() {
		return ok("Deconnecte")
			.withCookies(AuthAction.clearCookie());
	}

	// ------------------------------------------------------------------------------
	// Me
	// ------------------------------------------------------------------------------

	/**
	 *	Retourne les infos de l'utilisateur connecte.
	 *	GET /api/auth/me
	 *
	 *	@param request Requete HTTP
	 *	@return 200 avec le user ou 401
	 */
	public Result me(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		return ok(userToJson(userOpt.get()));
	}

	// ------------------------------------------------------------------------------
	// Fonctions utilitaires
	// ------------------------------------------------------------------------------

	/**
	 *	Construit un objet User a partir des parametres.
	 *
	 *	@param email Adresse email
	 *	@param password Mot de passe en clair
	 *	@param name Nom complet
	 *	@param isAdmin Role admin
	 *	@return User pret a etre sauvegarde
	 */
	private User buildUser(String email, String password, String name, boolean isAdmin) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(AuthAction.hashPassword(password));
		user.setName(name);
		user.setIsAdmin(isAdmin);
		return user;
	}

	/**
	 *	Convertit un User en JsonNode (sans le hash).
	 *
	 *	@param user Utilisateur a convertir
	 *	@return JsonNode avec id, email, name, admin
	 */
	private JsonNode userToJson(User user) {
		return Json.newObject()
			.put("id", user.getId())
			.put("email", user.getEmail())
			.put("name", user.getName())
			.put("admin", user.isAdmin());
	}
}