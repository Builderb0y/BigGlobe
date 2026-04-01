package builderb0y.bigglobe.dynamicRegistries;

import java.util.Arrays;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryValidator;
import net.minecraft.resources.ResourceKey;
import com.mojang.serialization.Codec;
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

	public static final ResourceKey<Registry<ElementSpec             >>               ELEMENT_SPEC_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("custom_class"));
	public static final ResourceKey<Registry<ScriptTemplate          >>            SCRIPT_TEMPLATE_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("script_template"));
	public static final ResourceKey<Registry<Grid                    >>              GRID_TEMPLATE_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("noise_source"));
	public static final ResourceKey<Registry<ColumnEntry             >>               COLUMN_ENTRY_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/column_value"));
	public static final ResourceKey<Registry<VoronoiSettings         >>           VORONOI_SETTINGS_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/voronoi_settings"));
	public static final ResourceKey<Registry<DecisionTreeSettings    >>     DECISION_TREE_SETTINGS_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/decision_tree"));
	public static final ResourceKey<Registry<Overrider               >>                  OVERRIDER_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/overrider"));
	public static final ResourceKey<Registry<CombinedStructureScripts>> SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/script_structure_piece"));
	public static final ResourceKey<Registry<WoodPalette             >>               WOOD_PALETTE_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("wood_palette"));
	public static final ResourceKey<Registry<FeatureDispatcher       >>         FEATURE_DISPATCHER_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/feature_dispatcher"));
	public static final ResourceKey<Registry<WorldTrait              >>                WORLD_TRAIT_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/world_trait"));
	public static final ResourceKey<Registry<Layer                   >>                      LAYER_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("worldgen/terrain_layer"));
	public static final ResourceKey<Registry<ExtraSpawn              >>                EXTRA_SPAWN_REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("extra_mob_spawn"));

	public static void init() {
		RegistryDataLoader.WORLDGEN_REGISTRIES.addAll(
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
		addBefore(Registries.STRUCTURE, SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(CombinedStructureScripts.class));
		addBefore(Registries.CONFIGURED_CARVER,       WOOD_PALETTE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(WoodPalette             .class));
		addAfter (Registries.PLACED_FEATURE,    FEATURE_DISPATCHER_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(FeatureDispatcher       .class));
		addAfter (Registries.BIOME,                    EXTRA_SPAWN_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ExtraSpawn              .class));
	}

	public static <T> RegistryDataLoader.RegistryData<T> entry(ResourceKey<Registry<T>> key, Codec<T> codec) {
		return new RegistryDataLoader.RegistryData<>(key, codec, RegistryValidator.none());
	}

	public static <T> void addBefore(ResourceKey<? extends Registry<?>> after, ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
		for (int index = 0, size = RegistryDataLoader.WORLDGEN_REGISTRIES.size(); index < size; index++) {
			if (RegistryDataLoader.WORLDGEN_REGISTRIES.get(index).key() == after) {
				RegistryDataLoader.WORLDGEN_REGISTRIES.add(index, entry(registryKey, codec));
				return;
			}
		}
		throw new IllegalStateException(after + " not in DYNAMIC_REGISTRIES");
	}

	public static <T> void addAfter(ResourceKey<? extends Registry<?>> before, ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
		for (int index = 0, size = RegistryDataLoader.WORLDGEN_REGISTRIES.size(); index < size; index++) {
			if (RegistryDataLoader.WORLDGEN_REGISTRIES.get(index).key() == before) {
				RegistryDataLoader.WORLDGEN_REGISTRIES.add(index + 1, entry(registryKey, codec));
				return;
			}
		}
		throw new IllegalStateException(before + " not in DYNAMIC_REGISTRIES");
	}
}