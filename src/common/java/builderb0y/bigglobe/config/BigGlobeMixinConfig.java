package builderb0y.bigglobe.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BigGlobeMixinConfig {

	public static final Logger
		LOGGER = LoggerFactory.getLogger("Big Globe/Mixin Config");
	public static final String
		MIXIN_PACKAGE = "builderb0y.bigglobe.mixins",
		CHUNK_PYRAMID_MIXIN_CLASS_NAME = MIXIN_PACKAGE + ".ChunkPyramid_ConfigurableFeatureRadius";
	public static final BigGlobeMixinConfig
		INSTANCE = new BigGlobeMixinConfig();

	public final Map<String, Object> defaults, settings;
	public final Set<String> unconfigurable;
	public final int featureChunkRadius;

	public BigGlobeMixinConfig() {
		this.defaults = this.initDefaults();
		this.unconfigurable = this.initUnconfigurable();
		for (String mixin : this.unconfigurable) {
			if (this.defaults.containsKey(mixin)) {
				String message = "Mixin " + mixin + " is both configurable and unconfigurable";
				if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
					throw new RuntimeException(message);
				}
				else {
					LOGGER.warn(message);
				}
			}
		}
		this.settings = this.convertProperties(this.loadProperties());
		this.checkChanged();
		this.featureChunkRadius = this.findInt(CHUNK_PYRAMID_MIXIN_CLASS_NAME);
	}

	public Map<String, Object> initDefaults() {
		Map<String, Object> defaults = new HashMap<>(64);
		defaults.put(MIXIN_PACKAGE + ".AzaleaBlock_GrowIntoBigGlobeTree",                                                         Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".BackgroundRenderer_NoFogWithLods",                                                         Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".BackgroundRenderer_SoulLavaFogColor",                                                      Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".Biome_DontFreezeRiverWater",                                                               Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".Biome_MakeTemperature2D",                                                                  Boolean.FALSE);
		defaults.put(MIXIN_PACKAGE + ".BiomeColors_UseNoiseInBigGlobeWorlds",                                                     Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".BoneMealItem_SpreadChorusNylium",                                                          Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".Camera_HandleSoulLavaSubmersion",                                                          Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".CatEntity_PetTheKitty",                                                                    Boolean.FALSE);
		defaults.put(MIXIN_PACKAGE + ".ChunkGeneratorStructureState_SkipSearchWhenFailureIsGuaranteed",                           Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".ClientWorldProperties_SetHorizonHeightToSeaLevel",                                         Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".CreakingHeartBlock_MakeWorkInTheNether",                                                   Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".CreakingHeartBlockEntity_MakeWorkInTheNether",                                             Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".CreateWorldScreen_MakeBigGlobeTheDefaultWorldType",                                        Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".CreateWorldScreen_MakeBigGlobeTheDefaultWorldType$WorldTab_HandleUnknownWorldTypesSanely", Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".Dev_CreateWorldScreen_DontCrashOnFailure",                                                 Boolean.FALSE);
		defaults.put(MIXIN_PACKAGE + ".Dev_NbtCompound_SanityCheckValues",                                                        Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".Dev_ServerPlayNetworkHandler_StopGeneratingChunksForSpectators",                           Boolean.FALSE);
		defaults.put(MIXIN_PACKAGE + ".DimensionOptions_CheckHeights",                                                            Boolean.FALSE);
		defaults.put(MIXIN_PACKAGE + ".EndCityStructure_UnHardcodeMinimumY",                                                      Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".EnderDragonFight_SpawnGatewaysAtPreferredLocation",                                        Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".EnderDragonSpawnState_UseBigGlobeEndSpikesInBigGlobeWorlds",                               Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".EnderPearlEntity_ReduceFallDamageWithVoidmetalArmor",                                      Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".EndGatewayBlockEntity_UseAlternateLogicInBigGlobeWorlds",                                  Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".EndPortalBlock_SpawnAtPreferredLocationInTheEnd",                                          Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".Entity_SpawnAtPreferredLocationInTheEnd",                                                  Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".EyeblossomBlock_MakeWorkInTheNether",                                                      Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".FlowableFluid_DontFlowInRivers",                                                           Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".FluidRenderer_DontHardCodeChunkSectionSizedAreas",                                         Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".FungusBlock_GrowIntoBigGlobeTree",                                                         Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".GrassBlock_UseCustomFeatureInBigGlobeWorlds",                                              Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".HuskEntity_AllowSpawningUndergroundInBigGlobeWorlds",                                      Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".IglooGeneratorPiece_DontMoveInBigGlobeWorlds",                                             Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".MinecraftClient_LoadingFinishedHook",                                                      Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".MinecraftServer_InitializeSpawnPoint",                                                     Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".MinecraftServer_LoadSmallerSpawnArea",                                                     Boolean.FALSE);
		defaults.put(MIXIN_PACKAGE + ".MobSpawnerLogic_SpawnLightning",                                                           Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".NetherrackBlock_GrowProperly",                                                             Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".OceanMonumentGeneratorBase_VanillaBugFixes",                                               Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".OceanMonumentStructure_MovePiecesOnReCreate",                                              Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".OceanRuinGeneratorPiece_UseGeneratorHeight",                                               Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".PlaceableFlintAndSticks",                                                                  Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".PlayerEntity_FlyInHyperspace",                                                             Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".PlayerEntity_TickHyperspaceCollapse",                                                      Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".PlayerManager_InitializeSpawnPoint",                                                       Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".PlayerManager_SyncWorldSettingsHook",                                                      Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".PolarBear_MakeSpawnableOnSnow",                                                            Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".PortalForcer_PlaceInNetherCaverns",                                                        Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".RailBlock_RotateProperly",                                                                 Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".SaplingBlock_GrowIntoBigGlobeTree",                                                        Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".Chunk_NotifyLodSystem",                                                                    Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".ServerPlayerEntity_CreateEndSpawnPlatformOnlyIfPreferred",                                 Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".ShipwreckGeneratorPiece_UseGeneratorHeight",                                               Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".SlimeEntity_AllowSpawningFromSpawner",                                                     Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".SoundEngine_UseSoundModifiers",                                                            Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".SpawnHelper_AllowSlimeSpawningInLakes",                                                    Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".SpawnHelper_MoreMobsInTallerWorlds",                                                       Boolean.FALSE);
		defaults.put(MIXIN_PACKAGE + ".StairsBlock_MirrorProperly",                                                               Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".StructureAccessor_UseStructureManagerInBigGlobeWorlds",                                    Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".SugarCaneBlock_MakePlaceableOnGravel",                                                     Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".TagGroupLoader_DontLoadMyF___ingTags",                                                     Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".WoodlandMansionStructure_DontHardCodeSeaLevel",                                            Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".World_UseCorrectSeaLevel",                                                                 Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".WorldGenProperties_LogLevelType",                                                          Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".WorldPresets_MakeBigGlobeTheDefaultWorldType2",                                            Boolean.TRUE);
		defaults.put(MIXIN_PACKAGE + ".WorldType_ChangeTranslation",                                                              Boolean.TRUE);
		defaults.put(CHUNK_PYRAMID_MIXIN_CLASS_NAME, 0);
		return defaults;
	}

	public Set<String> initUnconfigurable() {
		Set<String> unconfigurable = new HashSet<>();
		unconfigurable.add(MIXIN_PACKAGE + ".BigGlobeConfig_ImplementConfigData");
		unconfigurable.add(MIXIN_PACKAGE + ".Biome_DownfallAccessor");
		unconfigurable.add(MIXIN_PACKAGE + ".Blocks_RegisterVanillaBlocksEarly");
		unconfigurable.add(MIXIN_PACKAGE + ".ChunkRegion_UseCreateFlag");
		unconfigurable.add(MIXIN_PACKAGE + ".ConcentricRingsStructurePlacement_MakeSmart");
		unconfigurable.add(MIXIN_PACKAGE + ".DataPacks_StoreResourceManager");
		unconfigurable.add(MIXIN_PACKAGE + ".DebugHud_MakeSearchable");
		unconfigurable.add(MIXIN_PACKAGE + ".Entity_CurrentIdGetter");
		unconfigurable.add(MIXIN_PACKAGE + ".FallingBlockEntity_DestroyOnLandingAccess");
		unconfigurable.add(MIXIN_PACKAGE + ".GameRenderer_CaptureRealMatrices");
		unconfigurable.add(MIXIN_PACKAGE + ".Heightmap_StorageAccess");
		unconfigurable.add(MIXIN_PACKAGE + ".InGameHud_DebugHudGetter");
		unconfigurable.add(MIXIN_PACKAGE + ".ItemStack_DynamicMaxDamage");
		unconfigurable.add(MIXIN_PACKAGE + ".MinecraftClient_SetWorldEvent");
		unconfigurable.add(MIXIN_PACKAGE + ".MinecraftServer_SessionAccess");
		unconfigurable.add(MIXIN_PACKAGE + ".MobSpawnerLogic_GettersAndSettersForEverything");
		unconfigurable.add(MIXIN_PACKAGE + ".NbtCompound_ImplementExtensions");
		unconfigurable.add(MIXIN_PACKAGE + ".PalettedContainer_DataAccess");
		unconfigurable.add(MIXIN_PACKAGE + ".PlantBlock_CanPlantOnTopAccess");
		unconfigurable.add(MIXIN_PACKAGE + ".Items_PlaceableFlint");
		unconfigurable.add(MIXIN_PACKAGE + ".Items_PlaceableSticks");
		unconfigurable.add(MIXIN_PACKAGE + ".PlayerEntity_TrackWaypoints");
		unconfigurable.add(MIXIN_PACKAGE + ".RandomSpreadStructurePlacement_MakeSmart");
		unconfigurable.add(MIXIN_PACKAGE + ".RecipeManager_BackwardsCompatibleRecipes");
		unconfigurable.add(MIXIN_PACKAGE + ".RegistryLoader_LoadColumnEntryRegistry");
		unconfigurable.add(MIXIN_PACKAGE + ".RegistryOps_MakeAdjustable");
		unconfigurable.add(MIXIN_PACKAGE + ".SaveLoading_UnloadColumnEntryRegistry");
		unconfigurable.add(MIXIN_PACKAGE + ".ServerChunkLoadingManager_InitStructureManager");
		unconfigurable.add(MIXIN_PACKAGE + ".ServerPlayerEntity_FixNetherRoofGlitch");
		unconfigurable.add(MIXIN_PACKAGE + ".SingularPalette_EntryAccess");
		unconfigurable.add(MIXIN_PACKAGE + ".SpawnRestriction_BackingMapAccess");
		unconfigurable.add(MIXIN_PACKAGE + ".Structure_ImplementSizedStructure");
		unconfigurable.add(MIXIN_PACKAGE + ".StructureAccessor_WorldAccess");
		unconfigurable.add(MIXIN_PACKAGE + ".StructurePiece_DirectRotationSetter");
		unconfigurable.add(MIXIN_PACKAGE + ".StructurePlacement_MakeSmart");
		unconfigurable.add(MIXIN_PACKAGE + ".StructureStart_ChildrenGetter");
		unconfigurable.add(MIXIN_PACKAGE + ".WorldPreset_DimensionsAccess");
		unconfigurable.add(MIXIN_PACKAGE + ".WorldRenderer_HoldLodSystem");
		return unconfigurable;
	}

	public Properties loadProperties() {
		Path bigGlobeConfigFolder = FabricLoader.getInstance().getConfigDir().resolve("bigglobe");
		Path path = bigGlobeConfigFolder.resolve("mixins.properties");
		Path tmp = bigGlobeConfigFolder.resolve("mixins.tmp");
		Properties properties = new Properties();
		if (Files.exists(path)) try {
			//file exists, so try loading it.
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				properties.load(reader);
			}
			//ensure that the loaded properties file
			//contains ONLY keys that are in our defaults.
			//we don't want users to be able to toggle
			//options that we don't intentionally expose.
			int oldSize = properties.size();
			properties.keySet().retainAll(this.defaults.keySet());
			int newSize = properties.size();
			boolean changed = newSize != oldSize;

			//add any missing options.
			if (newSize != this.defaults.size()) {
				for (Map.Entry<String, Object> entry : this.defaults.entrySet()) {
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

			//if any error occurred while loading the file, use defaultEnabled.
			for (Map.Entry<String, Object> entry : this.defaults.entrySet()) {
				properties.setProperty(entry.getKey(), entry.getValue().toString());
			}

			//don't save the properties file, because we don't want
			//to overwrite user options when they are malformed.
		}
		else {
			//if the file does not exist, use defaultEnabled.
			for (Map.Entry<String, Object> entry : this.defaults.entrySet()) {
				properties.setProperty(entry.getKey(), entry.getValue().toString());
			}

			//and also save the defaultEnabled.
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

	public Map<String, Object> convertProperties(Properties properties) {
		Map<String, Object> map = new HashMap<>(properties.size());
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			if (CHUNK_PYRAMID_MIXIN_CLASS_NAME.equals(entry.getKey())) {
				try {
					map.put(entry.getKey().toString(), Integer.valueOf(entry.getValue().toString()));
				}
				catch (NumberFormatException exception) {
					LOGGER.warn(".minecraft/config/bigglobe/mixins.properties has an invalid value: " + entry.getKey() + " = " + entry.getValue() + "; expected integer.");
				}
			}
			else {
				if ("true".equalsIgnoreCase(entry.getValue().toString())) {
					map.put(entry.getKey().toString(), Boolean.TRUE);
				}
				else if ("false".equalsIgnoreCase(entry.getValue().toString())) {
					map.put(entry.getKey().toString(), Boolean.FALSE);
				}
				else {
					LOGGER.warn(".minecraft/config/bigglobe/mixins.properties has an invalid value: " + entry.getKey() + " = " + entry.getValue() + "; expected true or false.");
				}
			}
		}
		return map;
	}

	public void checkChanged() {
		for (Map.Entry<String, Object> entry : this.defaults.entrySet()) {
			Object enabled = this.settings.get(entry.getKey());
			if (!entry.getValue().equals(enabled)) {
				LOGGER.info(entry.getKey() + " has been changed from its default value: " + entry.getValue() + " -> " + enabled);
			}
		}
	}

	public boolean isEnabled(String mixinClassName) {
		Object enabled = this.settings.get(mixinClassName);
		if (enabled instanceof Boolean bool) {
			return bool.booleanValue();
		}
		else {
			if (!this.unconfigurable.contains(mixinClassName)) {
				LOGGER.warn("Mixin config option for " + mixinClassName + " not found or not a boolean: " + enabled);
			}
			return true;
		}
	}

	public int findInt(String mixinClassName) {
		Object radius = this.settings.get(mixinClassName);
		if (radius instanceof Number number) {
			return number.intValue();
		}
		else {
			LOGGER.warn("Mixin config option for " + mixinClassName + " not found or not a number: " + radius);
			return 0;
		}
	}

	public boolean knowsAbout(String mixinClassName) {
		return this.settings.containsKey(mixinClassName) || this.unconfigurable.contains(mixinClassName);
	}

	public static void init() {}
}