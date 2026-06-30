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
// Controleur Users (owner ou admin)
// ==============================================================================

/**
 *	Routes utilisateur : lecture et modification de son propre profil.
 *	Accessible au proprietaire du compte ou a un admin.
 */
public class UsersController extends Controller {

	// ------------------------------------------------------------------------------
	// Detail
	// ------------------------------------------------------------------------------

	/**
	 *	Retourne le profil d'un utilisateur.
	 *	GET /api/users/:id (owner ou admin)
	 *
	 *	@param request Requete HTTP
	 *	@param userId Identifiant utilisateur
	 *	@return 200 ou 401/403
	 */
	public Result show(Http.Request request, Long userId) {
		Optional<User> currentUser = AuthAction.getUserFromRequest(request);
		if (currentUser.isEmpty()) return unauthorized("Non connecte");
		if (!isOwnerOrAdmin(currentUser.get(), userId)) {
			return forbidden("Acces interdit");
		}
		User target = User.find.byId(userId);
		if (target == null) return notFound("Utilisateur introuvable");
		return ok(userToJson(target));
	}

	// ------------------------------------------------------------------------------
	// Mise a jour
	// ------------------------------------------------------------------------------

	/**
	 *	Met a jour le profil d'un utilisateur.
	 *	PATCH /api/users/:id (owner ou admin)
	 *
	 *	@param request Requete HTTP avec body JSON
	 *	@param userId Identifiant utilisateur
	 *	@return 200 ou erreur
	 */
	public Result update(Http.Request request, Long userId) {
		Optional<User> currentUser = AuthAction.getUserFromRequest(request);
		if (currentUser.isEmpty()) return unauthorized("Non connecte");
		if (!isOwnerOrAdmin(currentUser.get(), userId)) {
			return forbidden("Acces interdit");
		}
		User target = User.find.byId(userId);
		if (target == null) return notFound("Utilisateur introuvable");
		JsonNode body = request.body().asJson();
		updateUserFromJson(target, body, currentUser.get());
		target.update();
		return ok(userToJson(target));
	}

	// ------------------------------------------------------------------------------
	// Fonctions utilitaires
	// ------------------------------------------------------------------------------

	/**
	 *	Verifie si l'utilisateur est le proprietaire ou admin.
	 *
	 *	@param currentUser Utilisateur connecte
	 *	@param targetId Id de la cible
	 *	@return True si owner ou admin
	 */
	private boolean isOwnerOrAdmin(User currentUser, Long targetId) {
		return currentUser.getId().equals(targetId) || currentUser.isAdmin();
	}

	/**
	 *	Met a jour les champs d'un utilisateur depuis le JSON.
	 *	Seul un admin peut modifier le champ admin.
	 *
	 *	@param target Utilisateur cible
	 *	@param body JsonNode du body
	 *	@param currentUser Utilisateur connecte
	 */
	private void updateUserFromJson(User target, JsonNode body, User currentUser) {
		if (body.has("name")) target.setName(body.path("name").asText());
		if (body.has("email")) target.setEmail(body.path("email").asText());
		if (body.has("admin") && currentUser.isAdmin()) {
			target.setIsAdmin(body.path("admin").asBoolean());
		}
	}

	/**
	 *	Convertit un User en JsonNode (sans le hash).
	 *
	 *	@param user Utilisateur
	 *	@return JsonNode
	 */
	static JsonNode userToJson(User user) {
		return Json.newObject()
			.put("id", user.getId())
			.put("email", user.getEmail())
			.put("name", user.getName())
			.put("admin", user.isAdmin());
	}
}