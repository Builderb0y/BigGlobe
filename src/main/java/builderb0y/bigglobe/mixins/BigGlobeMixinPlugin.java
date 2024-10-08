package builderb0y.bigglobe.mixins;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeMixinPlugin implements IMixinConfigPlugin {

	public static final Logger LOGGER = LoggerFactory.getLogger("Big Globe/Mixins");
	public static BigGlobeMixinPlugin INSTANCE;

	public Map<String, Boolean> defaults, settings;

	@Override
	public void onLoad(String mixinPackage) {
		this.defaults = this.initDefaults(mixinPackage);
		this.settings = this.convertProperties(this.loadProperties());
		this.checkChanged();
		INSTANCE = this;
	}

	public Map<String, Boolean> initDefaults(String mixinPackage) {
		Map<String, Boolean> defaults = new HashMap<>(64);
		defaults.put(mixinPackage + ".AzaleaBlock_GrowIntoBigGlobeTree",                                       Boolean.TRUE);
		defaults.put(mixinPackage + ".BackgroundRenderer_SoulLavaFogColor",                                    Boolean.TRUE);
		defaults.put(mixinPackage + ".Biome_DontFreezeRiverWater",                                             Boolean.TRUE);
		defaults.put(mixinPackage + ".BiomeColors_UseNoiseInBigGlobeWorlds",                                   Boolean.TRUE);
		defaults.put(mixinPackage + ".BoneMealItem_SpreadChorusNylium",                                        Boolean.TRUE);
		defaults.put(mixinPackage + ".BubbleColumnBlock_WorkWithSoulMagma",                                    Boolean.TRUE);
		defaults.put(mixinPackage + ".CatEntity_PetTheKitty",                                                  Boolean.FALSE);
		defaults.put(mixinPackage + ".ChorusFlowerBlock_AllowPlacementOnOtherTypesOfEndStones",                Boolean.TRUE);
		defaults.put(mixinPackage + ".ChorusPlantBlock_AllowPlacementOnOtherTypesOfEndStones",                 Boolean.TRUE);
		defaults.put(mixinPackage + ".ChorusPlantFeature_AllowPlacementOnOtherTypesOfEndStones",               Boolean.TRUE);
		defaults.put(mixinPackage + ".ClientWorldProperties_SetHorizonHeightToSeaLevel",                       Boolean.TRUE);
		defaults.put(mixinPackage + ".CreateWorldScreen_MakeBigGlobeTheDefaultWorldType",                      Boolean.TRUE);
		defaults.put(mixinPackage + ".Dev_CreateWorldScreen_DontCrashOnFailure",                               Boolean.FALSE);
		defaults.put(mixinPackage + ".Dev_ServerPlayNetworkHandler_StopGeneratingChunksForSpectators",         Boolean.FALSE);
		defaults.put(mixinPackage + ".EndCityStructure_UnHardcodeMinimumY",                                    Boolean.TRUE);
		defaults.put(mixinPackage + ".EnderDragonFight_SpawnGatewaysAtPreferredLocation",                      Boolean.TRUE);
		defaults.put(mixinPackage + ".EnderDragonSpawnState_UseBigGlobeEndSpikesInBigGlobeWorlds",             Boolean.TRUE);
		defaults.put(mixinPackage + ".EndGatewayBlockEntity_UseAlternateLogicInBigGlobeWorlds",                Boolean.TRUE);
		defaults.put(mixinPackage + ".Entity_SpawnAtPreferredLocationInTheEnd",                                Boolean.TRUE);
		defaults.put(mixinPackage + ".FungusBlock_GrowIntoBigGlobeTree",                                       Boolean.TRUE);
		defaults.put(mixinPackage + ".IglooGeneratorPiece_DontMoveInBigGlobeWorlds",                           Boolean.TRUE);
		defaults.put(mixinPackage + ".ImmersivePortals_NetherPortalMatcher_PlacePortalHigherInBigGlobeWorlds", Boolean.TRUE);
		defaults.put(mixinPackage + ".Items_PlaceableFlint",                                                   Boolean.TRUE);
		defaults.put(mixinPackage + ".Items_PlaceableSticks",                                                  Boolean.TRUE);
		#if MC_VERSION < MC_1_20_5
		defaults.put(mixinPackage + ".MinecraftServer_LoadSmallerSpawnArea",                                   Boolean.FALSE);
		#endif
		defaults.put(mixinPackage + ".MobSpawnerLogic_SpawnLightning",                                         Boolean.TRUE);
		defaults.put(mixinPackage + ".NetherrackBlock_GrowProperly",                                           Boolean.TRUE);
		defaults.put(mixinPackage + ".OceanMonumentGeneratorBase_VanillaBugFixes",                             Boolean.TRUE);
		defaults.put(mixinPackage + ".OceanMonumentStructure_MovePiecesOnReCreate",                            Boolean.TRUE);
		defaults.put(mixinPackage + ".OceanRuinGeneratorPiece_UseGeneratorHeight",                             Boolean.TRUE);
		defaults.put(mixinPackage + ".PlayerManager_InitializeSpawnPoint",                                     Boolean.TRUE);
		defaults.put(mixinPackage + ".PortalForcer_PlaceInNetherCaverns",                                      Boolean.TRUE);
		defaults.put(mixinPackage + ".RailBlock_RotateProperly",                                               Boolean.TRUE);
		defaults.put(mixinPackage + ".SaplingBlock_GrowIntoBigGlobeTree",                                      Boolean.TRUE);
		defaults.put(mixinPackage + ".ServerPlayerEntity_CreateEndSpawnPlatformOnlyIfPreferred",               Boolean.TRUE);
		defaults.put(mixinPackage + ".ServerWorld_SpawnEnderDragonInBigGlobeWorlds",                           Boolean.TRUE);
		defaults.put(mixinPackage + ".ShipwreckGeneratorPiece_UseGeneratorHeight",                             Boolean.TRUE);
		defaults.put(mixinPackage + ".SlimeEntity_AllowSpawningFromSpawner",                                   Boolean.TRUE);
		defaults.put(mixinPackage + ".Sodium_WorldSlice_UseNoiseInBigGlobeWorlds",                             Boolean.TRUE);
		defaults.put(mixinPackage + ".SpawnHelper_AllowSlimeSpawningInLakes",                                  Boolean.TRUE);
		defaults.put(mixinPackage + ".StairsBlock_MirrorProperly",                                             Boolean.TRUE);
		defaults.put(mixinPackage + ".StructureStart_SaveBoundingBox",                                         Boolean.TRUE);
		defaults.put(mixinPackage + ".ThrownEntity_CollisionHook",                                             Boolean.TRUE);
		defaults.put(mixinPackage + ".VoxyIntegration",                                                        Boolean.TRUE);
		defaults.put(mixinPackage + ".WoodlandMansionStructure_DontHardCodeSeaLevel",                          Boolean.TRUE);
		defaults.put(mixinPackage + ".WorldPresets_MakeBigGlobeTheDefaultWorldType2",                          Boolean.TRUE);
		return defaults;
	}

	public Properties loadProperties() {
		Path bigGlobeConfigFolder = FabricLoader.getInstance().getConfigDir().resolve("bigglobe");
		Path path = bigGlobeConfigFolder.resolve("mixins.properties");
		Path tmp  = bigGlobeConfigFolder.resolve("mixins.tmp");
		Properties properties = new Properties();
		if (Files.exists(path)) try {
			//file exists, so try loading it.
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				properties.load(reader);
			}
			//ensure that the loaded properties file
			//contains ONLY //keys that are in our defaults.
			//we don't want users to be able to toggle
			//options that we don't intentionally expose.
			int oldSize = properties.size();
			properties.keySet().retainAll(this.defaults.keySet());
			int newSize = properties.size();
			boolean changed = newSize != oldSize;

			//add any missing options.
			if (newSize != this.defaults.size()) {
				for (Map.Entry<String, Boolean> entry : this.defaults.entrySet()) {
					properties.putIfAbsent(entry.getKey(), entry.getValue().toString());
				}
				changed = true;
			}

			//if the properties changed as a result of retaining
			//or adding missing options, save it again.
			if (changed) {
				this.saveProperties(properties, path, tmp);
			}
		}
		catch (IOException exception) {
			LOGGER.error("", exception);

			//if we were successful in loading some entries,
			//but not others, then we won't've done retaining,
			//and therefore these entries should not be trusted.
			if (!properties.isEmpty()) properties.clear();

			//if any error occurred while loading the file, use defaults.
			for (Map.Entry<String, Boolean> entry : this.defaults.entrySet()) {
				properties.setProperty(entry.getKey(), entry.getValue().toString());
			}

			//don't save the properties file, because we don't want
			//to overwrite user options when they are malformed.
		}
		else {
			//if the file does not exist, use defaults.
			for (Map.Entry<String, Boolean> entry : this.defaults.entrySet()) {
				properties.setProperty(entry.getKey(), entry.getValue().toString());
			}

			//and also save the defaults.
			this.saveProperties(properties, path, tmp);
		}
		return properties;
	}

	public void saveProperties(Properties properties, Path path, Path tmp) {
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				properties.store(writer, null);
			}
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public Map<String, Boolean> convertProperties(Properties properties) {
		Map<String, Boolean> map = new HashMap<>(properties.size());
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			if ("true".equalsIgnoreCase(entry.getValue().toString())) {
				map.put(entry.getKey().toString(), Boolean.TRUE);
			}
			else if ("false".equalsIgnoreCase(entry.getValue().toString())) {
				map.put(entry.getKey().toString(), Boolean.FALSE);
			}
			else {
				LOGGER.warn(".minecraft/config/bigglobe/mixins.properties has an invalid key " + entry.getKey() + " = " + entry.getValue() + "; expected true or false.");
			}
		}
		return map;
	}

	public void checkChanged() {
		for (Map.Entry<String, Boolean> entry : this.defaults.entrySet()) {
			Boolean enabled = this.settings.get(entry.getKey());
			if (entry.getValue() != enabled) {
				LOGGER.info(entry.getKey() + " has been changed from its default value: " + entry.getValue() + " -> " + enabled);
			}
		}
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	public static boolean checkMod(String mixinName, String modName) {
		if (FabricLoader.getInstance().isModLoaded(modName)) {
			LOGGER.info("Applying mixin " + mixinName + " because required mod " + modName + " is present.");
			return true;
		}
		else {
			LOGGER.info("Not applying mixin " + mixinName + " because required mod " + modName + " is absent.");
			return false;
		}
	}

	public static Version version(String version) {
		try {
			return Version.parse(version);
		}
		catch (VersionParsingException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public static boolean checkMod(String mixinName, String modName, Predicate<Version> versionPredicate) {
		if (
			FabricLoader
			.getInstance()
			.getModContainer(modName)
			.filter((ModContainer container) -> versionPredicate.test(container.getMetadata().getVersion()))
			.isPresent()
		) {
			LOGGER.info("Applying mixin " + mixinName + " because a known version of required mod " + modName + " is present.");
			return true;
		}
		else {
			LOGGER.info("Not applying mixin " + mixinName + " because a known version of required mod " + modName + " is absent.");
			return false;
		}
	}

	public boolean isEnabledInConfig(String mixinClassName) {
		Boolean enabled = this.settings.get(mixinClassName);
		return enabled != null ? enabled.booleanValue() : true;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return switch (mixinClassName) {
			case "builderb0y.bigglobe.mixins.BigGlobeConfig_ImplementConfigData" -> {
				yield checkMod(mixinClassName, "cloth-config");
			}
			case "builderb0y.bigglobe.mixins.Sodium_WorldSlice_UseNoiseInBigGlobeWorlds" -> {
				yield this.isEnabledInConfig(mixinClassName) && checkMod(mixinClassName, "sodium", (Version version) -> version.compareTo(version("0.5.0")) >= 0 && version.compareTo(version("0.6.0")) < 0);
			}
			case "builderb0y.bigglobe.mixins.ImmersivePortals_NetherPortalMatcher_PlacePortalHigherInBigGlobeWorlds" -> {
				yield this.isEnabledInConfig(mixinClassName) && checkMod(mixinClassName, "imm_ptl_core");
			}
			case
				"builderb0y.bigglobe.mixins.Voxy_WorldEngine_UseBigGlobeGenerator",
				"builderb0y.bigglobe.mixins.Voxy_ContextSelectionSystem_UseMemoryStorageBackendForDebugging"
			-> {
				yield this.isEnabledInConfig("builderb0y.bigglobe.mixins.VoxyIntegration") && checkMod(mixinClassName, "voxy");
			}
			case "builderb0y.bigglobe.mixins.Voxy_WorldSection_DataGetter" -> {
				yield checkMod(mixinClassName, "voxy");
			}
			default -> {
				yield this.isEnabledInConfig(mixinClassName);
			}
		};
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return Collections.emptyList();
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if (targetClassName.equals("builderb0y.bigglobe.compat.dhChunkGen.DhScriptedWorldGenerator")) {
			BigGlobeMod.LOGGER.info("DhScriptedWorldGenerator class loaded");
			try {
				Class<?> returnTypeClass = Class.forName("com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType");
				returnTypeClass.getDeclaredField("API_DATA_SOURCES");
				BigGlobeMod.LOGGER.info("API_DATA_SOURCES available. No class modifications necessary.");
			}
			catch (Exception ignored) {
				BigGlobeMod.LOGGER.info("API_DATA_SOURCES not available. Attempting modifications...");
				for (Iterator<MethodNode> iterator = targetClass.methods.iterator(); iterator.hasNext();) {
					MethodNode method = iterator.next();
					if (method.name.equals("generateLod")) {
						BigGlobeMod.LOGGER.info("Found generateLod(). Removing.");
						iterator.remove();
					}
					else if (method.name.equals("getReturnType")) {
						BigGlobeMod.LOGGER.info("Found getReturnType(). Attempting to change to API_CHUNKS...");
						for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
							if (instruction instanceof FieldInsnNode field) {
								BigGlobeMod.LOGGER.info("Found field. Changing to API_CHUNKS.");
								field.name = "API_CHUNKS";
								break;
							}
						}
					}
				}
			}
		}
	}
}