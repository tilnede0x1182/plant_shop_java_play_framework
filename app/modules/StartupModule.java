package modules;

// ==============================================================================
// Importations
// ==============================================================================

import com.google.inject.AbstractModule;

// ==============================================================================
// Module Guice de demarrage
// ==============================================================================

/**
 *	Module Guice qui charge StartupLogger en eager singleton.
 *	Configure dans application.conf via play.modules.enabled.
 */
public class StartupModule extends AbstractModule {

	/**
	 *	Enregistre StartupLogger comme eager singleton.
	 */
	@Override
	protected void configure() {
		bind(StartupLogger.class).asEagerSingleton();
	}
}
