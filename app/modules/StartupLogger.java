package modules;

// ==============================================================================
// Importations
// ==============================================================================

import com.typesafe.config.Config;
import play.inject.ApplicationLifecycle;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

// ==============================================================================
// Module de demarrage
// ==============================================================================

/**
 *	Affiche le log de demarrage avec l URL du serveur.
 *	Charge en eager singleton par Guice au demarrage de l application.
 */
@Singleton
public class StartupLogger {

	/**
	 *	Constructeur injecte par Guice. Affiche le log de demarrage.
	 *
	 *	@param config Configuration Play
	 *	@param lifecycle Cycle de vie de l application
	 */
	@Inject
	public StartupLogger(Config config, ApplicationLifecycle lifecycle) {
		int port = config.getInt("play.server.http.port");
		System.out.println("🚀 http://localhost:" + port);
		lifecycle.addStopHook(() -> CompletableFuture.completedFuture(null));
	}
}
