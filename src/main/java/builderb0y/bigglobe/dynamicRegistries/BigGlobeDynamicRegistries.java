package builderb0y.bigglobe.dynamicRegistries;

import java.util.Arrays;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryLoader;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.traits.WorldTrait;
import builderb0y.bigglobe.features.dispatch.FeatureDispatcher;
import builderb0y.bigglobe.noise.Grid;
import builderb0y.bigglobe.overriders.Overrider;
import builderb0y.bigglobe.spawning.ExtraSpawn;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.CombinedStructureScripts;
import builderb0y.scripting.parsing.input.ScriptTemplate;

public class BigGlobeDynamicRegistries {

	public static final RegistryKey<Registry<ScriptTemplate>>                      SCRIPT_TEMPLATE_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_script_templates"));
	public static final RegistryKey<Registry<Grid>>                                  GRID_TEMPLATE_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_noise_sources"));
	public static final RegistryKey<Registry<ColumnEntry>>                            COLUMN_ENTRY_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_column_value"));
	public static final RegistryKey<Registry<VoronoiSettings>>                    VORONOI_SETTINGS_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_voronoi_settings"));
	public static final RegistryKey<Registry<DecisionTreeSettings>>         DECISION_TREE_SETTINGS_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_decision_tree"));
	public static final RegistryKey<Registry<Overrider>>                                 OVERRIDER_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overrider"));
	public static final RegistryKey<Registry<CombinedStructureScripts>> SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_script_structure_placement"));
	public static final RegistryKey<Registry<WoodPalette>>                            WOOD_PALETTE_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_wood_palettes"));
	public static final RegistryKey<Registry<FeatureDispatcher>>                FEATURE_DISPATCHER_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_feature_dispatchers"));
	public static final RegistryKey<Registry<WorldTrait>>                              WORLD_TRAIT_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_world_traits"));
	public static final RegistryKey<Registry<Layer>>                                         LAYER_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_layers"));
	public static final RegistryKey<Registry<ExtraSpawn>>                              EXTRA_SPAWN_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_extra_mob_spawns"));
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static final Set<RegistryKey<Registry<?>>> KEYS = (Set)(Set.of(
		SCRIPT_TEMPLATE_REGISTRY_KEY,
		GRID_TEMPLATE_REGISTRY_KEY,
		COLUMN_ENTRY_REGISTRY_KEY,
		VORONOI_SETTINGS_REGISTRY_KEY,
		DECISION_TREE_SETTINGS_REGISTRY_KEY,
		OVERRIDER_REGISTRY_KEY,
		SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY,
		WOOD_PALETTE_REGISTRY_KEY,
		FEATURE_DISPATCHER_REGISTRY_KEY,
		WORLD_TRAIT_REGISTRY_KEY,
		LAYER_REGISTRY_KEY,
		EXTRA_SPAWN_REGISTRY_KEY
	));

	public static void init() {
		RegistryLoader.DYNAMIC_REGISTRIES.addAll(
			0,
			Arrays.asList(
				entry(       SCRIPT_TEMPLATE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ScriptTemplate      .class)),
				entry(           WORLD_TRAIT_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(WorldTrait          .class)),
				entry(         GRID_TEMPLATE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Grid                .class)),
				entry(          COLUMN_ENTRY_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ColumnEntry         .class)),
				entry(      VORONOI_SETTINGS_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(VoronoiSettings     .class)),
				entry(DECISION_TREE_SETTINGS_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(DecisionTreeSettings.class)),
				entry(             OVERRIDER_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Overrider           .class)),
				entry(                 LAYER_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Layer               .class))
			)
		);
		addBefore(RegistryKeys.STRUCTURE,         SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(CombinedStructureScripts.class));
		addBefore(RegistryKeys.CONFIGURED_CARVER,               WOOD_PALETTE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(WoodPalette             .class));
		addAfter (RegistryKeys.PLACED_FEATURE,            FEATURE_DISPATCHER_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(FeatureDispatcher       .class));
		addAfter (RegistryKeys.BIOME,                            EXTRA_SPAWN_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ExtraSpawn              .class));
	}

	public static <T> RegistryLoader.Entry<T> entry(RegistryKey<Registry<T>> key, Codec<T> codec) {
		return new RegistryLoader.Entry<>(key, codec #if MC_VERSION >= MC_1_21_0 , false #endif);
	}

	public static <T> void addBefore(RegistryKey<? extends Registry<?>> after, RegistryKey<Registry<T>> registryKey, Codec<T> codec) {
		for (int index = 0, size = RegistryLoader.DYNAMIC_REGISTRIES.size(); index < size; index++) {
			if (RegistryLoader.DYNAMIC_REGISTRIES.get(index).key() == after) {
				RegistryLoader.DYNAMIC_REGISTRIES.add(index, entry(registryKey, codec));
				return;
			}
		}
		throw new IllegalStateException(after + " not in DYNAMIC_REGISTRIES");
	}

	public static <T> void addAfter(RegistryKey<? extends Registry<?>> before, RegistryKey<Registry<T>> registryKey, Codec<T> codec) {
		for (int index = 0, size = RegistryLoader.DYNAMIC_REGISTRIES.size(); index < size; index++) {
			if (RegistryLoader.DYNAMIC_REGISTRIES.get(index).key() == before) {
				RegistryLoader.DYNAMIC_REGISTRIES.add(index + 1, entry(registryKey, codec));
				return;
			}
		}
		throw new IllegalStateException(before + " not in DYNAMIC_REGISTRIES");
	}
}