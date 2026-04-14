package builderb0y.bigglobe.dynamicRegistries;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.classes.ElementSpec;
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

	public static final ResourceKey<Registry<ElementSpec             >>           CUSTOM_CLASS_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("custom_class"));
	public static final ResourceKey<Registry<ExtraSpawn              >>        EXTRA_MOB_SPAWN_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("extra_mob_spawn"));
	public static final ResourceKey<Registry<Grid                    >>           NOISE_SOURCE_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("noise_source"));
	public static final ResourceKey<Registry<ScriptTemplate          >>        SCRIPT_TEMPLATE_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("script_template"));
	public static final ResourceKey<Registry<WoodPalette             >>           WOOD_PALETTE_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("wood_palette"));
	public static final ResourceKey<Registry<ColumnEntry             >>           COLUMN_VALUE_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/column_value"));
	public static final ResourceKey<Registry<DecisionTreeSettings    >>          DECISION_TREE_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/decision_tree"));
	public static final ResourceKey<Registry<FeatureDispatcher       >>     FEATURE_DISPATCHER_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/feature_dispatcher"));
	public static final ResourceKey<Registry<Overrider               >>              OVERRIDER_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/overrider"));
	public static final ResourceKey<Registry<CombinedStructureScripts>> SCRIPT_STRUCTURE_PIECE_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/script_structure_piece"));
	public static final ResourceKey<Registry<Layer                   >>          TERRAIN_LAYER_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/terrain_layer"));
	public static final ResourceKey<Registry<VoronoiSettings         >>       VORONOI_SETTINGS_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/voronoi_settings"));
	public static final ResourceKey<Registry<WorldTrait              >>            WORLD_TRAIT_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/world_trait"));

	public static void init() {
		DynamicRegistries.register(       EXTRA_MOB_SPAWN_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ExtraSpawn              .class));
		DynamicRegistries.register(          NOISE_SOURCE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Grid                    .class));
		DynamicRegistries.register(       SCRIPT_TEMPLATE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ScriptTemplate          .class));
		DynamicRegistries.register(          WOOD_PALETTE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(WoodPalette             .class));
		DynamicRegistries.register(          COLUMN_VALUE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ColumnEntry             .class));
		DynamicRegistries.register(         DECISION_TREE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(DecisionTreeSettings    .class));
		DynamicRegistries.register(    FEATURE_DISPATCHER_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(FeatureDispatcher       .class));
		DynamicRegistries.register(             OVERRIDER_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Overrider               .class));
		DynamicRegistries.register(SCRIPT_STRUCTURE_PIECE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(CombinedStructureScripts.class));
		DynamicRegistries.register(         TERRAIN_LAYER_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Layer                   .class));
		DynamicRegistries.register(      VORONOI_SETTINGS_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(VoronoiSettings         .class));
		DynamicRegistries.register(           WORLD_TRAIT_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(WorldTrait              .class));
	}
}