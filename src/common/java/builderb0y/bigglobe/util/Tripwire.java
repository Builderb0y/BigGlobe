package builderb0y.bigglobe.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import builderb0y.bigglobe.BigGlobeMod;

public class Tripwire {

	public static final boolean ENABLED = Boolean.getBoolean("bigglobe.tripwire");
	public static final Logger LOGGER = LogManager.getLogger(BigGlobeMod.MODNAME + "/Tripwire");

	public static boolean isEnabled() {
		return ENABLED;
	}

	public static void log(String message) {
		LOGGER.warn(message);
	}

	public static void logWithStackTrace(String message) {
		LOGGER.warn(message, new Throwable("Stack trace"));
	}
}