package builderb0y.bigglobe.mixins;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class BigGlobeMixinPlugin implements IMixinConfigPlugin {

	public static final Logger LOGGER = LoggerFactory.getLogger("Big Globe/Mixins");

	public Map<String, Boolean> defaults, settings;

	@Override
	public void onLoad(String mixinPackage) {
		this.defaults = this.initDefaults(mixinPackage);
		this.settings = this.convertProperties(this.loadProperties());
		this.checkChanged();
	}

	public Map<String, Boolean> initDefaults(String mixinPackage) {
		Map<String, Boolean> defaults = new HashMap<>(64);
		defaults.put(mixinPackage + ".AzaleaBlock_GrowIntoBigGlobeTree",                           Boolean.TRUE);
		defaults.put(mixinPackage + ".BackgroundRenderer_SoulLavaFogColor",                        Boolean.TRUE);
		defaults.put(mixinPackage + ".BiomeColors_UseNoiseInBigGlobeWorlds",                       Boolean.TRUE);
		defaults.put(mixinPackage + ".BoneMealItem_SpreadChorusNylium",                            Boolean.TRUE);
		defaults.put(mixinPackage + ".BubbleColumnBlock_WorkWithSoulMagma",                        Boolean.TRUE);
		defaults.put(mixinPackage + ".CactusBlock_AllowPlacementOnOvergrownSand",                  Boolean.TRUE); //unnecessary in 1.20.1.
		defaults.put(mixinPackage + ".CatEntity_PetTheKitty",                                      Boolean.FALSE);
		defaults.put(mixinPackage + ".ChorusFlowerBlock_AllowPlacementOnOtherTypesOfEndStones",    Boolean.TRUE);
		defaults.put(mixinPackage + ".ChorusPlantBlock_AllowPlacementOnOtherTypesOfEndStones",     Boolean.TRUE);
		defaults.put(mixinPackage + ".ChorusPlantFeature_AllowPlacementOnOtherTypesOfEndStones",   Boolean.TRUE);
		defaults.put(mixinPackage + ".ClientWorldProperties_SetHorizonHeightToSeaLevel",           Boolean.TRUE);
		defaults.put(mixinPackage + ".CommandBlockExecutor_Optimize",                              Boolean.FALSE);
		defaults.put(mixinPackage + ".EndCityStructure_UnHardcodeMinimumY",                        Boolean.TRUE);
		defaults.put(mixinPackage + ".EnderDragonFight_SpawnGatewaysAtPreferredLocation",          Boolean.TRUE);
		defaults.put(mixinPackage + ".EnderDragonSpawnState_UseBigGlobeEndSpikesInBigGlobeWorlds", Boolean.TRUE);
		defaults.put(mixinPackage + ".EndGatewayBlockEntity_UseAlternateLogicInBigGlobeWorlds",    Boolean.TRUE);
		defaults.put(mixinPackage + ".Entity_SpawnAtPreferredLocationInTheEnd",                    Boolean.TRUE);
		defaults.put(mixinPackage + ".IglooGeneratorPiece_DontMoveInBigGlobeWorlds",               Boolean.TRUE);
		defaults.put(mixinPackage + ".Items_PlaceableFlint",                                       Boolean.TRUE);
		defaults.put(mixinPackage + ".Items_PlaceableSticks",                                      Boolean.TRUE);
		defaults.put(mixinPackage + ".MinecraftServer_LoadSmallerSpawnArea",                       Boolean.FALSE);
		defaults.put(mixinPackage + ".MobSpawnerLogic_SpawnLightning",                             Boolean.TRUE);
		defaults.put(mixinPackage + ".NetherrackBlock_GrowProperly",                               Boolean.TRUE);
		defaults.put(mixinPackage + ".NoiseChunkGenerator_DisplayVanillaColumnValues",             Boolean.TRUE);
		defaults.put(mixinPackage + ".OceanMonumentGeneratorBase_VanillaBugFixes",                 Boolean.TRUE);
		defaults.put(mixinPackage + ".OceanMonumentStructure_MovePiecesOnReCreate",                Boolean.TRUE);
		defaults.put(mixinPackage + ".OceanRuinGeneratorPiece_UseGeneratorHeight",                 Boolean.TRUE);
		defaults.put(mixinPackage + ".PlayerManager_InitializeSpawnPoint",                         Boolean.TRUE);
		defaults.put(mixinPackage + ".PortalForcer_PlaceInNetherCaverns",                          Boolean.TRUE);
		defaults.put(mixinPackage + ".SaplingBlock_GrowIntoBigGlobeTree",                          Boolean.TRUE);
		defaults.put(mixinPackage + ".ServerPlayerEntity_CreateEndSpawnPlatformOnlyIfPreferred",   Boolean.TRUE);
		defaults.put(mixinPackage + ".ServerWorld_CreateEnderDragonFightInBigGlobeWorlds",         Boolean.TRUE);
		defaults.put(mixinPackage + ".ShipwreckGeneratorPiece_UseGeneratorHeight",                 Boolean.TRUE);
		defaults.put(mixinPackage + ".SlimeEntity_AllowSpawningFromSpawner",                       Boolean.TRUE);
		defaults.put(mixinPackage + ".SpawnHelper_AllowSlimeSpawningInLakes",                      Boolean.TRUE);
		defaults.put(mixinPackage + ".StairsBlock_MirrorProperly",                                 Boolean.TRUE);
		defaults.put(mixinPackage + ".StructureStart_SaveBoundingBox",                             Boolean.TRUE);
		defaults.put(mixinPackage + ".ThrownEntity_CollisionHook",                                 Boolean.TRUE);
		defaults.put(mixinPackage + ".WoodlandMansionStructure_DontHardCodeSeaLevel",              Boolean.TRUE);
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

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.equals("builderb0y.bigglobe.mixins.BigGlobeConfig_ImplementConfigData")) {
			return FabricLoader.getInstance().isModLoaded("cloth-config");
		}
		Boolean enabled = this.settings.get(mixinClassName);
		return enabled != null ? enabled.booleanValue() : true;
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

	}
}