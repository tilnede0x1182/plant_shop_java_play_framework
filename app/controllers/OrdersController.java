package controllers;

// ==============================================================================
// Importations
// ==============================================================================

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Order;
import models.OrderItem;
import models.Plant;
import models.User;
import play.libs.Json;
import play.mvc.*;
import security.AuthAction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// ==============================================================================
// Controleur Orders
// ==============================================================================

/**
 *	Routes pour les commandes : liste et creation.
 *	Toutes les routes requierent un utilisateur connecte.
 */
public class OrdersController extends Controller {

	// ------------------------------------------------------------------------------
	// Liste
	// ------------------------------------------------------------------------------

	/**
	 *	Liste les commandes de l'utilisateur connecte.
	 *	GET /api/orders
	 *
	 *	@param request Requete HTTP
	 *	@return 200 avec tableau JSON ou 401
	 */
	public Result index(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		List<Order> orders = Order.find.query()
			.fetch("orderItems")
			.fetch("orderItems.plant")
			.where().eq("user", userOpt.get())
			.findList();
		return ok(ordersToJsonArray(orders));
	}

	// ------------------------------------------------------------------------------
	// Creation
	// ------------------------------------------------------------------------------

	/**
	 *	Cree une commande avec les items du panier.
	 *	POST /api/orders
	 *
	 *	@param request Requete HTTP avec body JSON {items: [{plant_id, quantity}]}
	 *	@return 201 ou 400 si stock insuffisant
	 */
	public Result create(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return unauthorized("Non connecte");
		JsonNode body = request.body().asJson();
		if (body == null || !body.has("items")) {
			return badRequest("Donnees invalides");
		}
		return processOrderCreation(userOpt.get(), body.get("items"));
	}

	// ------------------------------------------------------------------------------
	// Fonctions utilitaires
	// ------------------------------------------------------------------------------

	/**
	 *	Traite la creation d'une commande.
	 *
	 *	@param user Utilisateur connecte
	 *	@param items JsonNode tableau des items
	 *	@return Result 201 ou 400
	 */
	private Result processOrderCreation(User user, JsonNode items) {
		Order order = new Order();
		order.setUser(user);
		order.setStatus("confirmed");
		order.save();
		BigDecimal total = BigDecimal.ZERO;
		for (JsonNode item : items) {
			Result errorResult = addItemToOrder(order, item);
			if (errorResult != null) return errorResult;
			Plant plant = Plant.find.byId(item.path("plant_id").asLong());
			int quantity = item.path("quantity").asInt();
			total = total.add(plant.getPrice().multiply(BigDecimal.valueOf(quantity)));
		}
		order.setTotal(total);
		order.update();
		return created(orderToJson(order));
	}

	/**
	 *	Ajoute un item a une commande avec verification du stock.
	 *
	 *	@param order Commande en cours
	 *	@param item JsonNode {plant_id, quantity}
	 *	@return null si OK, Result erreur si stock insuffisant
	 */
	private Result addItemToOrder(Order order, JsonNode item) {
		Long plantId = item.path("plant_id").asLong();
		int quantity = item.path("quantity").asInt();
		Plant plant = Plant.find.byId(plantId);
		if (plant == null || plant.getStock() < quantity) {
			return badRequest("Stock insuffisant pour " + plantId);
		}
		OrderItem orderItem = new OrderItem();
		orderItem.setOrder(order);
		orderItem.setPlant(plant);
		orderItem.setQuantity(quantity);
		orderItem.setPrice(plant.getPrice());
		orderItem.save();
		plant.setStock(plant.getStock() - quantity);
		plant.update();
		return null;
	}

	/**
	 *	Convertit une commande en JsonNode.
	 *
	 *	@param order Commande
	 *	@return JsonNode
	 */
	private JsonNode orderToJson(Order order) {
		ObjectNode node = Json.newObject();
		node.put("id", order.getId());
		node.put("total", order.getTotal());
		node.put("status", order.getStatus());
		return node;
	}

	/**
	 *	Convertit une liste de commandes en ArrayNode JSON.
	 *
	 *	@param orders Liste de commandes
	 *	@return ArrayNode JSON
	 */
	private ArrayNode ordersToJsonArray(List<Order> orders) {
		ArrayNode array = Json.newArray();
		for (Order order : orders) {
			array.add(orderToJson(order));
		}
		return array;
	}
}