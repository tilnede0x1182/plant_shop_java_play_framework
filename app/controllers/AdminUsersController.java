package controllers;

// ==============================================================================
// Importations
// ==============================================================================

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import models.User;
import play.libs.Json;
import play.mvc.*;
import security.AuthAction;

import java.util.List;
import java.util.Optional;

// ==============================================================================
// Controleur Admin Users
// ==============================================================================

/**
 *	Routes admin pour la gestion des utilisateurs.
 *	Toutes les routes requierent un utilisateur admin connecte.
 */
public class AdminUsersController extends Controller {

	// ------------------------------------------------------------------------------
	// Liste
	// ------------------------------------------------------------------------------

	/**
	 *	Liste tous les utilisateurs (admins en premier, puis par nom).
	 *	GET /api/admin/users
	 *
	 *	@param request Requete HTTP
	 *	@return 200 ou erreur
	 */
	public Result index(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		if (!userOpt.get().isAdmin()) return forbidden("Admin requis");
		List<User> users = User.find.query()
			.orderBy("is_admin desc, name asc")
			.findList();
		return ok(usersToJsonArray(users));
	}

	// ------------------------------------------------------------------------------
	// Creation
	// ------------------------------------------------------------------------------

	/**
	 *	Cree un nouvel utilisateur (admin peut creer des admins).
	 *	POST /api/users
	 *
	 *	@param request Requete HTTP avec body JSON
	 *	@return 201 ou erreur
	 */
	public Result create(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		if (!userOpt.get().isAdmin()) return forbidden("Admin requis");
		JsonNode body = request.body().asJson();
		if (body == null) return badRequest("Body JSON requis");
		User user = buildUserFromJson(body);
		user.save();
		return created(UsersController.userToJson(user));
	}

	// ------------------------------------------------------------------------------
	// Mise a jour
	// ------------------------------------------------------------------------------

	/**
	 *	Met a jour un utilisateur (admin peut modifier le role).
	 *	PATCH /api/admin/users/:id
	 *
	 *	@param request Requete HTTP avec body JSON
	 *	@param userId Identifiant utilisateur
	 *	@return 200 ou erreur
	 */
	public Result update(Http.Request request, Long userId) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		if (!userOpt.get().isAdmin()) return forbidden("Admin requis");
		User target = User.find.byId(userId);
		if (target == null) return notFound("Utilisateur introuvable");
		JsonNode body = request.body().asJson();
		updateUserFromJson(target, body);
		target.update();
		return ok(UsersController.userToJson(target));
	}

	// ------------------------------------------------------------------------------
	// Suppression
	// ------------------------------------------------------------------------------

	/**
	 *	Supprime un utilisateur.
	 *	DELETE /api/admin/users/:id
	 *
	 *	@param request Requete HTTP
	 *	@param userId Identifiant utilisateur
	 *	@return 200 ou erreur
	 */
	public Result delete(Http.Request request, Long userId) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		if (!userOpt.get().isAdmin()) return forbidden("Admin requis");
		User target = User.find.byId(userId);
		if (target == null) return notFound("Utilisateur introuvable");
		target.delete();
		return ok("Utilisateur supprime");
	}

	// ------------------------------------------------------------------------------
	// Fonctions utilitaires
	// ------------------------------------------------------------------------------

	/**
	 *	Construit un User depuis le JSON.
	 *
	 *	@param body JsonNode
	 *	@return User pret a sauvegarder
	 */
	private User buildUserFromJson(JsonNode body) {
		User user = new User();
		user.setEmail(body.path("email").asText(""));
		user.setName(body.path("name").asText(""));
		String password = body.path("password").asText("password");
		user.setPasswordHash(AuthAction.hashPassword(password));
		user.setIsAdmin(body.path("admin").asBoolean(false));
		return user;
	}

	/**
	 *	Met a jour les champs d'un utilisateur.
	 *
	 *	@param target Utilisateur cible
	 *	@param body JsonNode
	 */
	private void updateUserFromJson(User target, JsonNode body) {
		if (body.has("name")) target.setName(body.path("name").asText());
		if (body.has("email")) target.setEmail(body.path("email").asText());
		if (body.has("admin")) target.setIsAdmin(body.path("admin").asBoolean());
	}

	/**
	 *	Convertit une liste d'utilisateurs en ArrayNode JSON.
	 *
	 *	@param users Liste d'utilisateurs
	 *	@return ArrayNode JSON
	 */
	private ArrayNode usersToJsonArray(List<User> users) {
		ArrayNode array = Json.newArray();
		for (User user : users) {
			array.add(UsersController.userToJson(user));
		}
		return array;
	}
}