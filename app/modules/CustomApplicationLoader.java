package modules;

// ==============================================================================
// Importations
// ==============================================================================

import play.ApplicationLoader;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;
import java.net.ServerSocket;
import java.net.BindException;
import java.io.IOException;

// ==============================================================================
// Application Loader personnalise
// ==============================================================================

/**
 *	Charge l application Play en verifiant le port avant Guice.
 *	S execute avant tout module, avant HikariCP, avant Ebean.
 */
public class CustomApplicationLoader extends GuiceApplicationLoader {

	/**
	 *	Construit le GuiceApplicationBuilder. Verifie le port avant Guice.
	 *	S execute avant la creation de l injecteur (avant HikariCP, Ebean).
	 *
	 *	@param context Contexte de chargement Play
	 *	@return GuiceApplicationBuilder configure
	 */
	@Override
	public GuiceApplicationBuilder builder(ApplicationLoader.Context context) {
		int port = context.initialConfig().getInt("play.server.http.port");
		checkPortAvailable(port);
		return super.builder(context);
	}

	/**
	 *	Verifie que le port est disponible.
	 *	Quitte le processus si le port est deja occupe.
	 *
	 *	@param port Port a verifier
	 */
	private void checkPortAvailable(int port) {
		try (ServerSocket testSocket = new ServerSocket(port)) {
			testSocket.close();
		} catch (BindException bindException) {
			System.err.println("❌ Le port " + port + " est deja utilise.");
			System.exit(1);
		} catch (IOException ioException) {
			System.err.println("❌ Erreur verification port : " + ioException.getMessage());
			System.exit(1);
		}
	}
}
