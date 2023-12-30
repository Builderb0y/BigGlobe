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

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.chunkgen.BigGlobeEndChunkGenerator;
import builderb0y.bigglobe.chunkgen.BigGlobeNetherChunkGenerator;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.compat.dhChunkGen.DhEndWorldGenerator;
import builderb0y.bigglobe.compat.dhChunkGen.DhNetherWorldGenerator;
import builderb0y.bigglobe.compat.dhChunkGen.DhOverworldWorldGenerator;
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

		public static final Map<Class<? extends BigGlobeChunkGenerator>, DhWorldGeneratorFactory<?>> GENERATOR_FACTORIES = new HashMap<>(4);
		static {
			registerHyperspeedGenerator(BigGlobeOverworldChunkGenerator.class, DhOverworldWorldGenerator::new);
			registerHyperspeedGenerator(   BigGlobeNetherChunkGenerator.class,    DhNetherWorldGenerator::new);
			registerHyperspeedGenerator(      BigGlobeEndChunkGenerator.class,       DhEndWorldGenerator::new);
		}

		public static <G extends BigGlobeChunkGenerator> void registerHyperspeedGenerator(Class<G> generatorClass, DhWorldGeneratorFactory<G> hyperspeedFactory) {
			GENERATOR_FACTORIES.put(generatorClass, hyperspeedFactory);
		}

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
				@SuppressWarnings({ "unchecked", "rawtypes" })
				public void onLevelLoad(DhApiEventParam<EventParam> param) {
					IDhApiLevelWrapper levelWrapper = param.value.levelWrapper;
					if (
						levelWrapper.getWrappedMcObject() instanceof ServerWorld serverWorld &&
						serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeChunkGenerator chunkGenerator
					) {
						if (BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.hyperspeedGeneration) {
							DhWorldGeneratorFactory factory = GENERATOR_FACTORIES.get(chunkGenerator.getClass());
							if (factory != null) {
								BigGlobeMod.LOGGER.info("Initializing hyperspeed DH world generator.");
								DhApi.worldGenOverrides.registerWorldGeneratorOverride(levelWrapper, factory.create(levelWrapper, serverWorld, chunkGenerator));
							}
							else {
								BigGlobeMod.LOGGER.warn("Don't know how to initialize hyperspeed world generator for unknown chunk generator subclass: " + chunkGenerator.getClass());
							}
						}
						else {
							BigGlobeMod.LOGGER.info("Not using hyperspeed DH world generator, as it is disabled in Big Globe's config file.");
						}
					}
				}
			});
		}

		public static interface DhWorldGeneratorFactory<G extends BigGlobeChunkGenerator> {

			public abstract IDhApiWorldGenerator create(IDhApiLevelWrapper levelWrapper, ServerWorld serverWorld, G generator);
		}
	}
}