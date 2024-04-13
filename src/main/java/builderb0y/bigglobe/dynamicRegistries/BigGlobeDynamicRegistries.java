package builderb0y.bigglobe.dynamicRegistries;

import java.util.Arrays;
import java.util.Comparator;

import com.mojang.serialization.Codec;

import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.features.dispatch.FeatureDispatcher;
import builderb0y.bigglobe.noise.Grid;
import builderb0y.bigglobe.overriders.Overrider;
import builderb0y.bigglobe.randomLists.ConstantComputedRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.CombinedStructureScripts;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.parsing.ScriptUsage.ScriptTemplate;

#if MC_VERSION <= MC_1_19_2
	import net.minecraft.registry.DynamicRegistryManager.Info;
#else
	import net.minecraft.registry.RegistryLoader;
#endif

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

	#if MC_VERSION == MC_1_19_2
		public static final List<Info<?>> INFOS = new ArrayList<>(9);
		static {
			INFOS.add(info(SCRIPT_TEMPLATE_REGISTRY_KEY,            ScriptTemplate          .class, null));
			INFOS.add(info(GRID_TEMPLATE_REGISTRY_KEY,              Grid                    .class, null));
			INFOS.add(info(COLUMN_ENTRY_REGISTRY_KEY,               ColumnEntry             .class, null));
			INFOS.add(info(VORONOI_SETTINGS_REGISTRY_KEY,           VoronoiSettings         .class, null));
			INFOS.add(info(DECISION_TREE_SETTINGS_REGISTRY_KEY,     DecisionTreeSettings    .class, null));
			INFOS.add(info(OVERRIDER_REGISTRY_KEY,                  Overrider               .class, null));
			INFOS.add(info(SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY, CombinedStructureScripts.class, null));
			INFOS.add(info(WOOD_PALETTE_REGISTRY_KEY,               WoodPalette             .class, null));
			INFOS.add(info(FEATURE_DISPATCHER_REGISTRY_KEY,         FeatureDispatcher       .class, null));
		}

		public static <E> DynamicRegistryManager.Info<E> info(RegistryKey<Registry<E>> key, Class<E> clazz, Codec<E> networkCodec) {
			Codec<E> codec = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(clazz);
			return new DynamicRegistryManager.Info<>(key, codec, networkCodec);
		}

		public static void registerBigGlobeDynamicRegistries(ImmutableMap.Builder<RegistryKey<? extends Registry<?>>, DynamicRegistryManager.Info<?>> builder) {
			for (DynamicRegistryManager.Info<?> info : INFOS) {
				builder.put(info.registry(), info);
			}
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void addBuiltin() {
			BigGlobeMod.LOGGER.debug("Adding " + BigGlobeMod.MODNAME + " objects to builtin registries...");
			for (DynamicRegistryManager.Info<?> info : INFOS) {
				((MutableRegistry)(BuiltinRegistries.REGISTRIES)).add(info.registry(), new SimpleRegistry<>(info.registry(), Lifecycle.experimental(), null), Lifecycle.experimental());
			}
			BigGlobeMod.LOGGER.debug("Done adding " + BigGlobeMod.MODNAME + " objects to builtin registries.");
		}
	#else
		public static void init() {
			RegistryLoader.DYNAMIC_REGISTRIES.addAll(
				0,
				Arrays.asList(
					new RegistryLoader.Entry<>(       SCRIPT_TEMPLATE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ScriptTemplate      .class)),
					new RegistryLoader.Entry<>(         GRID_TEMPLATE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Grid                .class)),
					new RegistryLoader.Entry<>(          COLUMN_ENTRY_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ColumnEntry         .class)),
					new RegistryLoader.Entry<>(      VORONOI_SETTINGS_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(VoronoiSettings     .class)),
					new RegistryLoader.Entry<>(DECISION_TREE_SETTINGS_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(DecisionTreeSettings.class)),
					new RegistryLoader.Entry<>(             OVERRIDER_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Overrider           .class))
				)
			);
			addBefore(RegistryKeyVersions.structure(), SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(CombinedStructureScripts.class));
			addBefore(RegistryKeyVersions.configuredCarver(),        WOOD_PALETTE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(WoodPalette             .class));
			addAfter (RegistryKeyVersions.placedFeature(),     FEATURE_DISPATCHER_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(FeatureDispatcher       .class));
		}

		public static <T> void addBefore(RegistryKey<? extends Registry<?>> after, RegistryKey<Registry<T>> registryKey, Codec<T> codec) {
			for (int index = 0, size = RegistryLoader.DYNAMIC_REGISTRIES.size(); index < size; index++) {
				if (RegistryLoader.DYNAMIC_REGISTRIES.get(index).key() == after) {
					RegistryLoader.DYNAMIC_REGISTRIES.add(index, new RegistryLoader.Entry<>(registryKey, codec));
					return;
				}
			}
			throw new IllegalStateException(after + " not in DYNAMIC_REGISTRIES");
		}

		public static <T> void addAfter(RegistryKey<? extends Registry<?>> before, RegistryKey<Registry<T>> registryKey, Codec<T> codec) {
			for (int index = 0, size = RegistryLoader.DYNAMIC_REGISTRIES.size(); index < size; index++) {
				if (RegistryLoader.DYNAMIC_REGISTRIES.get(index).key() == before) {
					RegistryLoader.DYNAMIC_REGISTRIES.add(index + 1, new RegistryLoader.Entry<>(registryKey, codec));
					return;
				}
			}
			throw new IllegalStateException(before + " not in DYNAMIC_REGISTRIES");
		}
	#endif


	public static <T extends IWeightedListElement> IRandomList<RegistryEntry<T>> sortAndCollect(BetterRegistry<T> registry) {
		ConstantComputedRandomList<RegistryEntry<T>> list = new ConstantComputedRandomList<>() {

			@Override
			public double getWeightOfElement(RegistryEntry<T> element) {
				return element.value().getWeight();
			}
		};
		registry
		.streamEntries()
		.sorted(
			Comparator.comparing(
				(RegistryEntry<T> entry) -> (
					UnregisteredObjectException.getKey(entry).getValue()
				),
				Comparator
				.comparing(Identifier::getNamespace)
				.thenComparing(Identifier::getPath)
			)
		)
		.forEachOrdered(list::add);
		if (list.isEmpty()) throw new IllegalStateException(registry.getKey().getValue() + " is empty");
		return list;
	}
}