package controllers;

// ==============================================================================
// Importations
// ==============================================================================

import models.Order;
import models.Plant;
import models.User;
import play.mvc.*;
import security.AuthAction;
import scala.jdk.javaapi.OptionConverters;

import java.util.List;
import java.util.Optional;

// ==============================================================================
// Controleur Pages Front (Twirl)
// ==============================================================================

/**
 *	Controleur des pages front HTML.
 *	Rend les templates Twirl avec les donnees necessaires.
 */
public class PagesController extends Controller {

	// --------------------------------------------------------------------------
	// Fonctions utilitaires
	// --------------------------------------------------------------------------

	/**
	 *	Convertit un Optional Java en Option Scala pour les templates Twirl.
	 *
	 *	@param optional Optional Java
	 *	@return Option Scala
	 */
	private scala.Option<User> toScalaOption(Optional<User> optional) {
		return OptionConverters.toScala(optional);
	}

	/**
	 *	Redirection 302 (Found) au lieu du 303 par defaut de Play.
	 *	Conforme au comportement des autres projets Plant Shop (Rails, etc.).
	 *
	 *	@param url URL de destination
	 *	@return Result avec code 302
	 */
	private Result redirectFound(String url) {
		return Results.found(url);
	}

	// --------------------------------------------------------------------------
	// Pages publiques
	// --------------------------------------------------------------------------

	/**
	 *	Catalogue des plantes en stock.
	 *	GET /plants
	 *
	 *	@param request Requete HTTP
	 *	@return Page HTML avec la liste des plantes
	 */
	public Result plants(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		List<Plant> plantList = Plant.find.query().where().ge("stock", 1).orderBy("name").findList();
		return ok(views.html.plants.index.render(plantList, toScalaOption(userOpt)));
	}

	/**
	 *	Fiche detail d une plante.
	 *	GET /plants/:id
	 *
	 *	@param request Requete HTTP
	 *	@param plantId Identifiant de la plante
	 *	@return Page HTML de la plante ou 404
	 */
	public Result plantShow(Http.Request request, Long plantId) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		Plant plant = Plant.find.byId(plantId);
		if (plant == null) return notFound("Plante introuvable");
		return ok(views.html.plants.show.render(plant, toScalaOption(userOpt)));
	}

	// --------------------------------------------------------------------------
	// Pages panier
	// --------------------------------------------------------------------------

	/**
	 *	Page panier (localStorage cote client).
	 *	GET /cart
	 *
	 *	@param request Requete HTTP
	 *	@return Page HTML du panier
	 */
	public Result cart(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		return ok(views.html.cart.index.render(toScalaOption(userOpt)));
	}

	// --------------------------------------------------------------------------
	// Pages authentification
	// --------------------------------------------------------------------------

	/**
	 *	Page de connexion.
	 *	GET /auth/signin
	 *
	 *	@return Page HTML du formulaire de connexion
	 */
	public Result signin() {
		return ok(views.html.auth.signin.render());
	}

	/**
	 *	Page d inscription.
	 *	GET /auth/register
	 *
	 *	@return Page HTML du formulaire d inscription
	 */
	public Result register() {
		return ok(views.html.auth.register.render());
	}

	// --------------------------------------------------------------------------
	// Pages protegees (utilisateur connecte)
	// --------------------------------------------------------------------------

	/**
	 *	Historique des commandes.
	 *	GET /orders
	 *
	 *	@param request Requete HTTP
	 *	@return Page HTML ou redirection vers /auth/signin
	 */
	public Result orders(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return redirectFound("/auth/signin");
		List<Order> orderList = Order.find.query()
			.fetch("orderItems").fetch("orderItems.plant")
			.where().eq("user", userOpt.get())
			.orderBy("createdAt desc").findList();
		return ok(views.html.orders.index.render(orderList, toScalaOption(userOpt)));
	}

	/**
	 *	Page de creation de commande.
	 *	GET /orders/new
	 *
	 *	@param request Requete HTTP
	 *	@return Page HTML ou redirection vers /auth/signin
	 */
	public Result newOrder(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return redirectFound("/auth/signin");
		return ok(views.html.orders.newOrder.render(toScalaOption(userOpt)));
	}

	/**
	 *	Profil utilisateur.
	 *	GET /users/:id
	 *
	 *	@param request Requete HTTP
	 *	@param userId Identifiant utilisateur
	 *	@return Page HTML ou redirection
	 */
	public Result userProfile(Http.Request request, Long userId) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return redirectFound("/auth/signin");
		User target = User.find.byId(userId);
		if (target == null) return notFound("Utilisateur introuvable");
		return ok(views.html.users.profile.render(target, toScalaOption(userOpt)));
	}

	/**
	 *	Page modification du profil.
	 *	GET /users/:id/edit
	 *
	 *	@param request Requete HTTP
	 *	@param userId Identifiant utilisateur
	 *	@return Page HTML ou redirection
	 */
	public Result editProfile(Http.Request request, Long userId) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return redirectFound("/auth/signin");
		if (!userOpt.get().getId().equals(userId) && !userOpt.get().isAdmin()) {
			return redirectFound("/plants");
		}
		User target = User.find.byId(userId);
		if (target == null) return notFound("Utilisateur introuvable");
		return ok(views.html.users.edit.render(target, toScalaOption(userOpt)));
	}

	// --------------------------------------------------------------------------
	// Deconnexion
	// --------------------------------------------------------------------------

	/**
	 *	Deconnecte l utilisateur via l API et redirige.
	 *	GET /auth/logout
	 *
	 *	@return Redirection 302 vers /plants avec cookie efface
	 */
	public Result logout() {
		return Results.found("/plants")
			.withCookies(AuthAction.clearCookie());
	}

	// --------------------------------------------------------------------------
	// Pages admin
	// --------------------------------------------------------------------------

	/**
	 *	Gestion des plantes (admin).
	 *	GET /admin/plants
	 *
	 *	@param request Requete HTTP
	 *	@return Page HTML ou redirection
	 */
	public Result adminPlants(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return redirectFound("/auth/signin");
		if (!userOpt.get().isAdmin()) return redirectFound("/plants");
		List<Plant> plantList = Plant.find.query().orderBy("name").findList();
		return ok(views.html.admin.plants.index.render(plantList, toScalaOption(userOpt)));
	}

	/**
	 *	Page creation d une nouvelle plante (admin).
	 *	GET /admin/plants/new
	 *
	 *	@param request Requete HTTP
	 *	@return Page HTML ou redirection
	 */
	public Result newPlant(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return redirectFound("/auth/signin");
		if (!userOpt.get().isAdmin()) return redirectFound("/plants");
		return ok(views.html.admin.plants.newPlant.render(toScalaOption(userOpt)));
	}

	/**
	 *	Page modification d une plante (admin).
	 *	GET /admin/plants/:id/edit
	 *
	 *	@param request Requete HTTP
	 *	@param plantId Identifiant de la plante
	 *	@return Page HTML ou redirection
	 */
	public Result editPlant(Http.Request request, Long plantId) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return redirectFound("/auth/signin");
		if (!userOpt.get().isAdmin()) return redirectFound("/plants");
		Plant plant = Plant.find.byId(plantId);
		if (plant == null) return notFound("Plante introuvable");
		return ok(views.html.admin.plants.edit.render(plant, toScalaOption(userOpt)));
	}

	/**
	 *	Gestion des utilisateurs (admin).
	 *	GET /admin/users
	 *
	 *	@param request Requete HTTP
	 *	@return Page HTML ou redirection
	 */
	public Result adminUsers(Http.Request request) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return redirectFound("/auth/signin");
		if (!userOpt.get().isAdmin()) return redirectFound("/plants");
		List<User> userList = User.find.query().orderBy("is_admin desc, name").findList();
		return ok(views.html.admin.users.index.render(userList, toScalaOption(userOpt)));
	}

	/**
	 *	Page modification d un utilisateur (admin).
	 *	GET /admin/users/:id/edit
	 *
	 *	@param request Requete HTTP
	 *	@param userId Identifiant utilisateur
	 *	@return Page HTML ou redirection
	 */
	public Result editUser(Http.Request request, Long userId) {
		Optional<User> userOpt = AuthAction.getUserFromRequest(request);
		if (userOpt.isEmpty()) return redirectFound("/auth/signin");
		if (!userOpt.get().isAdmin()) return redirectFound("/plants");
		User target = User.find.byId(userId);
		if (target == null) return notFound("Utilisateur introuvable");
		return ok(views.html.admin.users.edit.render(target, toScalaOption(userOpt)));
	}
}
