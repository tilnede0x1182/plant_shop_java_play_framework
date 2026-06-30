package controllers;

// ==============================================================================
// Importations
// ==============================================================================

import play.mvc.*;

// ==============================================================================
// Controleur Home
// ==============================================================================

/**
 *	Controleur de la page d'accueil.
 *	Redirige vers le catalogue des plantes.
 */
public class HomeController extends Controller {

	/**
	 *	Redirige la racine vers /plants.
	 *	GET /
	 *
	 *	@return Redirection 302 vers /plants
	 */
	public Result index() {
		return redirect("/plants");
	}
}
