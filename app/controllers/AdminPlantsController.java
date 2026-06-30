package controllers;

// ==============================================================================
// Importations
// ==============================================================================

import com.fasterxml.jackson.databind.JsonNode;
import models.Plant;
import models.User;
import play.libs.Json;
import play.mvc.*;
import security.AuthAction;

import java.math.BigDecimal;
import java.util.Optional;

// ==============================================================================
// Controleur Admin Plants
// ==============================================================================

/**
 *	Routes admin pour la gestion des plantes (CRUD).
 *	Toutes les routes requierent un utilisateur admin connecte.
 */
public class AdminPlantsController extends Controller {

	// ------------------------------------------------------------------------------
	// Liste admin
	// ------------------------------------------------------------------------------

	/**
	 *	Liste toutes les plantes (y compris stock 0).
	 *	GET /api/admin/plants
	 *
	 *	@param request Requete HTTP
	 *	@return 200 ou 302 redirection
	 */
	public Result index(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		if (!userOpt.get().isAdmin()) return forbidden("Admin requis");
		return ok(PlantsController.plantsToJsonArray(
			Plant.find.query().orderBy("name asc").findList()
		));
	}

	// ------------------------------------------------------------------------------
	// Creation
	// ------------------------------------------------------------------------------

	/**
	 *	Cree une nouvelle plante.
	 *	POST /api/admin/plants
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
		Plant plant = buildPlantFromJson(body);
		plant.save();
		return created(PlantsController.plantToJson(plant));
	}

	// ------------------------------------------------------------------------------
	// Mise a jour
	// ------------------------------------------------------------------------------

	/**
	 *	Met a jour une plante existante.
	 *	PATCH /api/admin/plants/:id
	 *
	 *	@param request Requete HTTP avec body JSON
	 *	@param plantId Identifiant de la plante
	 *	@return 200 ou erreur
	 */
	public Result update(Http.Request request, Long plantId) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		if (!userOpt.get().isAdmin()) return forbidden("Admin requis");
		Plant plant = Plant.find.byId(plantId);
		if (plant == null) return notFound("Plante introuvable");
		JsonNode body = request.body().asJson();
		updatePlantFromJson(plant, body);
		plant.update();
		return ok(PlantsController.plantToJson(plant));
	}

	// ------------------------------------------------------------------------------
	// Suppression
	// ------------------------------------------------------------------------------

	/**
	 *	Supprime une plante.
	 *	DELETE /api/admin/plants/:id
	 *
	 *	@param request Requete HTTP
	 *	@param plantId Identifiant de la plante
	 *	@return 200 ou erreur
	 */
	public Result delete(Http.Request request, Long plantId) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		if (!userOpt.get().isAdmin()) return forbidden("Admin requis");
		Plant plant = Plant.find.byId(plantId);
		if (plant == null) return notFound("Plante introuvable");
		plant.delete();
		return ok("Plante supprimee");
	}

	// ------------------------------------------------------------------------------
	// Fonctions utilitaires
	// ------------------------------------------------------------------------------

	/**
	 *	Construit un objet Plant a partir du JSON.
	 *
	 *	@param body JsonNode du body
	 *	@return Plant pret a sauvegarder
	 */
	private Plant buildPlantFromJson(JsonNode body) {
		Plant plant = new Plant();
		plant.setName(body.path("name").asText(""));
		plant.setDescription(body.path("description").asText(""));
		plant.setPrice(BigDecimal.valueOf(body.path("price").asInt(0)));
		plant.setStock(body.path("stock").asInt(0));
		return plant;
	}

	/**
	 *	Met a jour les champs d'une plante depuis le JSON.
	 *
	 *	@param plant Plante a modifier
	 *	@param body JsonNode du body
	 */
	private void updatePlantFromJson(Plant plant, JsonNode body) {
		if (body.has("name")) plant.setName(body.path("name").asText());
		if (body.has("description")) plant.setDescription(body.path("description").asText());
		if (body.has("price")) plant.setPrice(BigDecimal.valueOf(body.path("price").asInt()));
		if (body.has("stock")) plant.setStock(body.path("stock").asInt());
	}
}