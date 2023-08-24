package builderb0y.bigglobe.dynamicRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;

import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.randomLists.ConstantComputedRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.settings.BiomeLayout.EndBiomeLayout;
import builderb0y.bigglobe.settings.BiomeLayout.OverworldBiomeLayout;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;
import builderb0y.bigglobe.settings.OverworldCavernSettings.LocalOverworldCavernSettings;
import builderb0y.bigglobe.settings.OverworldSkylandSettings.LocalSkylandSettings;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.CombinedStructureScripts;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.parsing.ScriptTemplate;

#if MC_VERSION <= MC_1_19_2
	import net.minecraft.registry.DynamicRegistryManager.Info;
#else
	import net.minecraft.registry.RegistryLoader;
#endif

public class BigGlobeDynamicRegistries {

	public static final RegistryKey<Registry<ScriptTemplate>>                  SCRIPT_TEMPLATE_REGISTRY_KEY                 = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_script_templates"));
	public static final RegistryKey<Registry<CombinedStructureScripts>>        SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY      = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_script_structure_placement"));
	public static final RegistryKey<Registry<WoodPalette>>                     WOOD_PALETTE_REGISTRY_KEY                    = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_wood_palettes"));
	public static final RegistryKey<Registry<LocalNetherSettings>>             LOCAL_NETHER_SETTINGS_REGISTRY_KEY           = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_nether_biome"));
	public static final RegistryKey<Registry<OverworldBiomeLayout>>            OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY          = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_biome_layout"));
	public static final RegistryKey<Registry<EndBiomeLayout>>                  END_BIOME_LAYOUT_REGISTRY_KEY                = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_end_biome_layout"));
	public static final RegistryKey<Registry<LocalOverworldCaveSettings>>      LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY   = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_caves"));
	public static final RegistryKey<Registry<LocalOverworldCavernSettings>>    LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_caverns"));
	public static final RegistryKey<Registry<LocalSkylandSettings>>            LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY          = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_skylands"));

	#if MC_VERSION == MC_1_19_2
		public static final List<Info<?>> INFOS = new ArrayList<>(9);
		static {
			INFOS.add(info(SCRIPT_TEMPLATE_REGISTRY_KEY,                 ScriptTemplate              .class, null));
			INFOS.add(info(SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY,      CombinedStructureScripts    .class, null));
			INFOS.add(info(WOOD_PALETTE_REGISTRY_KEY,                    WoodPalette                 .class, null));
			INFOS.add(info(OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY,          OverworldBiomeLayout        .class, null));
			INFOS.add(info(END_BIOME_LAYOUT_REGISTRY_KEY,                EndBiomeLayout              .class, null));
			INFOS.add(info(LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY,   LocalOverworldCaveSettings  .class, null));
			INFOS.add(info(LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY, LocalOverworldCavernSettings.class, null));
			INFOS.add(info(LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY,          LocalSkylandSettings        .class, null));
			INFOS.add(info(LOCAL_NETHER_SETTINGS_REGISTRY_KEY,           LocalNetherSettings         .class, null));
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
			RegistryLoader.DYNAMIC_REGISTRIES.add(0, new RegistryLoader.Entry<>(SCRIPT_TEMPLATE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ScriptTemplate.class)));
			addBefore(RegistryKeyVersions.structure(),              SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY,      BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(CombinedStructureScripts    .class));
			addBefore(RegistryKeyVersions.configuredCarver(),       WOOD_PALETTE_REGISTRY_KEY,                    BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(WoodPalette                 .class));
			addBefore(RegistryKeyVersions.chunkGeneratorSettings(), LOCAL_NETHER_SETTINGS_REGISTRY_KEY,           BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LocalNetherSettings         .class));
			addAfter (RegistryKeyVersions.biome(),                  END_BIOME_LAYOUT_REGISTRY_KEY,                BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(EndBiomeLayout              .class));
			addAfter (RegistryKeyVersions.biome(),                  OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY,          BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(OverworldBiomeLayout        .class));
			addBefore(RegistryKeyVersions.chunkGeneratorSettings(), LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY,   BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LocalOverworldCaveSettings  .class));
			addBefore(RegistryKeyVersions.chunkGeneratorSettings(), LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LocalOverworldCavernSettings.class));
			addBefore(RegistryKeyVersions.chunkGeneratorSettings(), LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY,          BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LocalSkylandSettings        .class));
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