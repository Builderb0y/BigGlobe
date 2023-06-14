package builderb0y.bigglobe.dynamicRegistries;

import java.util.Comparator;

import com.mojang.serialization.Codec;

import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.randomLists.ConstantContainedRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.settings.BiomeLayout.EndBiomeLayout;
import builderb0y.bigglobe.settings.BiomeLayout.OverworldBiomeLayout;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;
import builderb0y.bigglobe.settings.OverworldCavernSettings.LocalCavernSettings;
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
	public static final RegistryKey<Registry<LocalCavernSettings>>             LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_caverns"));
	public static final RegistryKey<Registry<LocalSkylandSettings>>            LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY          = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_skylands"));

	public static void init() {
		RegistryLoader.DYNAMIC_REGISTRIES.add(0, new RegistryLoader.Entry<>(SCRIPT_TEMPLATE_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ScriptTemplate.class)));
		addBefore(RegistryKeys.STRUCTURE,                SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY,      BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(StructurePlacementScript.Holder.class));
		addBefore(RegistryKeys.CONFIGURED_CARVER,        WOOD_PALETTE_REGISTRY_KEY,                    BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(WoodPalette                    .class));
		addBefore(RegistryKeys.CHUNK_GENERATOR_SETTINGS, LOCAL_NETHER_SETTINGS_REGISTRY_KEY,           BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LocalNetherSettings            .class));
		addAfter (RegistryKeys.BIOME,                    END_BIOME_LAYOUT_REGISTRY_KEY,                BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(EndBiomeLayout                 .class));
		addAfter (RegistryKeys.BIOME,                    OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY,          BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(OverworldBiomeLayout           .class));
		addBefore(RegistryKeys.CHUNK_GENERATOR_SETTINGS, LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY,   BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LocalOverworldCaveSettings     .class));
		addBefore(RegistryKeys.CHUNK_GENERATOR_SETTINGS, LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LocalCavernSettings            .class));
		addBefore(RegistryKeys.CHUNK_GENERATOR_SETTINGS, LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY,          BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LocalSkylandSettings           .class));
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

	public static <T extends IWeightedListElement> IRandomList<T> sortAndCollect(RegistryWrapper<T> registry) {
		ConstantContainedRandomList<T> list = new ConstantContainedRandomList<>();
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
		.map(RegistryEntry::value)
		.forEachOrdered(list::add);
		if (list.isEmpty()) throw new IllegalStateException((registry instanceof RegistryWrapper.Impl<T> impl ? impl.getRegistryKey().getValue() : registry) + " is empty");
		return list;
	}
}