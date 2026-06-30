package controllers;

// ==============================================================================
// Importations
// ==============================================================================

import models.Plant;
import play.libs.Json;
import play.mvc.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;

// ==============================================================================
// Controleur Plants (routes publiques)
// ==============================================================================

/**
 *	Routes publiques pour les plantes : liste et detail.
 *	Filtre les plantes dont le stock est >= 1.
 */
public class PlantsController extends Controller {

	// ------------------------------------------------------------------------------
	// Liste
	// ------------------------------------------------------------------------------

	/**
	 *	Liste toutes les plantes en stock (stock >= 1).
	 *	GET /api/plants
	 *
	 *	@return 200 avec tableau JSON des plantes
	 */
	public Result index() {
		List<Plant> plants = Plant.find.query()
			.where().ge("stock", 1)
			.orderBy("name asc")
			.findList();
		return ok(plantsToJsonArray(plants));
	}

	// ------------------------------------------------------------------------------
	// Detail
	// ------------------------------------------------------------------------------

	/**
	 *	Retourne le detail d'une plante par son id.
	 *	GET /api/plants/:id
	 *
	 *	@param plantId Identifiant de la plante
	 *	@return 200 avec la plante ou 404
	 */
	public Result show(Long plantId) {
		Plant plant = Plant.find.byId(plantId);
		if (plant == null) return notFound("Plante introuvable");
		return ok(plantToJson(plant));
	}

	// ------------------------------------------------------------------------------
	// Fonctions utilitaires
	// ------------------------------------------------------------------------------

	/**
	 *	Convertit une plante en JsonNode.
	 *
	 *	@param plant Plante a convertir
	 *	@return JsonNode
	 */
	static JsonNode plantToJson(Plant plant) {
		return Json.newObject()
			.put("id", plant.getId())
			.put("name", plant.getName())
			.put("description", plant.getDescription())
			.put("price", plant.getPrice())
			.put("stock", plant.getStock());
	}

	/**
	 *	Convertit une liste de plantes en ArrayNode JSON.
	 *
	 *	@param plants Liste de plantes
	 *	@return ArrayNode JSON
	 */
	static ArrayNode plantsToJsonArray(List<Plant> plants) {
		ArrayNode array = Json.newArray();
		for (Plant plant : plants) {
			array.add(plantToJson(plant));
		}
		return array;
	}
}