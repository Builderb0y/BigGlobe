package builderb0y.bigglobe.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.*;
import builderb0y.bigglobe.compat.dhChunkGen.DhScriptedWorldGenerator;
import builderb0y.bigglobe.config.BigGlobeConfig;

public class DistantHorizonsCompat {

	public static final MethodHandle isOnDistantHorizonThread;
	static {
		MethodHandle handle;
		got: {
			if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
				//try API
				try {
					Class<?> api = Class.forName("com.seibel.distanthorizons.api.DhApi");
					handle = MethodHandles.lookup().findStatic(api, "isDhThread", MethodType.methodType(boolean.class));
					BigGlobeMod.LOGGER.info("Distant horizons API compatibility enabled.");
					break got;
				}
				catch (Exception ignored) {}
				//try 2.0
				try {
					Class<?> environment = Class.forName("loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment", false, DistantHorizonsCompat.class.getClassLoader());
					handle = MethodHandles.lookup().findStatic(environment, "isCurrentThreadDistantGeneratorThread", MethodType.methodType(boolean.class));
					BigGlobeMod.LOGGER.info("Distant horizons 2.0 compatibility enabled.");
					break got;
				}
				catch (Exception ignored) {}
				//try 1.6
				try {
					Class<?> environment = Class.forName("fabric.com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment", false, DistantHorizonsCompat.class.getClassLoader());
					handle = MethodHandles.lookup().findStatic(environment, "isCurrentThreadDistantGeneratorThread", MethodType.methodType(boolean.class));
					BigGlobeMod.LOGGER.info("Distant horizons 1.6 compatibility enabled.");
					break got;
				}
				catch (Exception ignored) {}
			}
			BigGlobeMod.LOGGER.info("Distant horizons compatibility disabled.");
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

	public static void init() {
		if (FabricLoader.getInstance().isModLoaded("distanthorizons")) try {
			DHCode.init();
		}
		catch (LinkageError error) {
			BigGlobeMod.LOGGER.error("Failed to setup Distant Horizons integration: ", error);
		}
	}

	public static class DHCode {

		public static void init() {
			try {
				//make sure method we intend to override is present in the version of distant horizons the user has installed.
				//if the user is on an old version, then we don't want to register anything.
				IDhApiWorldGenerator.class.getDeclaredMethod("generateApiChunks", int.class, int.class, byte.class, byte.class, EDhApiDistantGeneratorMode.class, ExecutorService.class, Consumer.class);
				BigGlobeMod.LOGGER.info("Distant Horizons hyperspeed generators available.");
			}
			catch (NoSuchMethodException exception) {
				BigGlobeMod.LOGGER.info("Distant Horizons hyperspeed generators unavailable. Consider updating Distant Horizons.");
				return;
			}
			DhApiEventRegister.on(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent() {

				@Override
				public void onLevelLoad(DhApiEventParam<EventParam> param) {
					IDhApiLevelWrapper levelWrapper = param.value.levelWrapper;
					if (levelWrapper.getWrappedMcObject() instanceof ServerWorld serverWorld) {
						ChunkGenerator generator = serverWorld.getChunkManager().getChunkGenerator();
						if (generator instanceof BigGlobeScriptedChunkGenerator chunkGenerator) {
							if (BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.hyperspeedGeneration) {
								DhApi.worldGenOverrides.registerWorldGeneratorOverride(levelWrapper, new DhScriptedWorldGenerator(levelWrapper, serverWorld, chunkGenerator));
							}
							else {
								BigGlobeMod.LOGGER.info("Not using hyperspeed DH world generator, as it is disabled in Big Globe's config file.");
							}
						}
					}
				}
			});
		}
	}
}