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
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.objects.data.DhApiChunk;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
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

		static {
			//this will throw a NoSuchMethodError if the DH version is too old,
			//which will then be wrapped in an ExceptionInInitializerError,
			//which extends LinkageError, and therefore will be caught by init().
			DhApiChunk.create(0, 0, 0, 128);
			DhApiTerrainDataPoint.create((byte)(0), 0, 0, 0, 128, null, null);
		}

		public static DhApiChunk newChunk(int chunkX, int chunkZ, int minY, int maxY) {
			return DhApiChunk.create(chunkX, chunkZ, minY, maxY);
		}

		public static DhApiTerrainDataPoint newDataPoint(
			byte detailLevel,
			int blockLight,
			int skyLight,
			int minY,
			int maxY,
			IDhApiBlockStateWrapper state,
			IDhApiBiomeWrapper biome
		) {
			return DhApiTerrainDataPoint.create(detailLevel, blockLight, skyLight, minY, maxY, state, biome);
		}

		public static void init() {
			DhApiEventRegister.on(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent() {

				@Override
				public void onLevelLoad(DhApiEventParam<EventParam> param) {
					IDhApiLevelWrapper levelWrapper = param.value.levelWrapper;
					if (levelWrapper.getWrappedMcObject() instanceof ServerWorld serverWorld) {
						if (serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
							if (BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.hyperspeedGeneration) {
								DhApi.worldGenOverrides.registerWorldGeneratorOverride(levelWrapper, new DhScriptedWorldGenerator(levelWrapper, serverWorld, generator));
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