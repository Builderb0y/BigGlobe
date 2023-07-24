package builderb0y.bigglobe.dynamicRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.*;
import net.minecraft.util.registry.DynamicRegistryManager.Info;

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
import builderb0y.bigglobe.structures.scripted.StructurePlacementScript;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.parsing.ScriptTemplate;

public class BigGlobeDynamicRegistries {

	public static final RegistryKey<Registry<ScriptTemplate>>                  SCRIPT_TEMPLATE_REGISTRY_KEY                 = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_script_templates"));
	public static final RegistryKey<Registry<StructurePlacementScript.Holder>> SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY      = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_script_structure_placement"));
	public static final RegistryKey<Registry<WoodPalette>>                     WOOD_PALETTE_REGISTRY_KEY                    = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_wood_palettes"));
	public static final RegistryKey<Registry<LocalNetherSettings>>             LOCAL_NETHER_SETTINGS_REGISTRY_KEY           = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_nether_biome"));
	public static final RegistryKey<Registry<OverworldBiomeLayout>>            OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY          = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_biome_layout"));
	public static final RegistryKey<Registry<EndBiomeLayout>>                  END_BIOME_LAYOUT_REGISTRY_KEY                = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_end_biome_layout"));
	public static final RegistryKey<Registry<LocalOverworldCaveSettings>>      LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY   = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_caves"));
	public static final RegistryKey<Registry<LocalOverworldCavernSettings>>    LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_caverns"));
	public static final RegistryKey<Registry<LocalSkylandSettings>>            LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY          = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_skylands"));

	public static final List<Info<?>> INFOS = new ArrayList<>(9);
	static {
		INFOS.add(info(SCRIPT_TEMPLATE_REGISTRY_KEY,                 ScriptTemplate                 .class, null));
		INFOS.add(info(SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY,      StructurePlacementScript.Holder.class, null));
		INFOS.add(info(WOOD_PALETTE_REGISTRY_KEY,                    WoodPalette                    .class, null));
		INFOS.add(info(OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY,          OverworldBiomeLayout           .class, null));
		INFOS.add(info(END_BIOME_LAYOUT_REGISTRY_KEY,                EndBiomeLayout                 .class, null));
		INFOS.add(info(LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY,   LocalOverworldCaveSettings     .class, null));
		INFOS.add(info(LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY, LocalOverworldCavernSettings   .class, null));
		INFOS.add(info(LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY,          LocalSkylandSettings           .class, null));
		INFOS.add(info(LOCAL_NETHER_SETTINGS_REGISTRY_KEY,           LocalNetherSettings            .class, null));
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

	public static <T extends IWeightedListElement> IRandomList<RegistryEntry<T>> sortAndCollect(Registry<T> registry) {
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