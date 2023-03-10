package builderb0y.bigglobe.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;

public class DistantHorizonsCompat {

	public static final MethodHandle isOnDistantHorizonThread;
	static {
		MethodHandle handle;
		got: {
			try {
				Class<?> environment = Class.forName("fabric.com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment");
				handle = MethodHandles.lookup().findStatic(environment, "isCurrentThreadDistantGeneratorThread", MethodType.methodType(boolean.class));
				BigGlobeMod.LOGGER.info("Distant horizon compatibility enabled.");
				break got;
			}
			catch (Exception ignored) {
				BigGlobeMod.LOGGER.info("Distant horizon compatibility disabled.");
			}
			handle = MethodHandles.constant(boolean.class, Boolean.FALSE);
		}
		isOnDistantHorizonThread = handle;
	}

	public static boolean isOnDistantHorizonThread() {
		try {
			return (boolean)(isOnDistantHorizonThread.invokeExact());
		}
		catch (Throwable throwable) {
			throw AutoCodecUtil.rethrow(throwable);
		}
	}
}