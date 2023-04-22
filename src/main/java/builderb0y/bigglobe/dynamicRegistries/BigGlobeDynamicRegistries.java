package builderb0y.bigglobe.dynamicRegistries;

import com.mojang.serialization.Codec;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryLoader;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.structures.scripted.StructurePlacementScript;
import builderb0y.scripting.parsing.ScriptTemplate;

public class BigGlobeDynamicRegistries {

	public static final RegistryKey<Registry<ScriptTemplate>> SCRIPT_TEMPLATE_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_script_templates"));
	public static final RegistryKey<Registry<StructurePlacementScript.Holder>> SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_script_structure_placement"));
	public static final RegistryKey<Registry<WoodPalette>> WOOD_PALETTE_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_wood_palettes"));
	public static final RegistryKey<Registry<LocalNetherSettings>> LOCAL_NETHER_SETTINGS_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_nether_biome"));

	public static void init() {
		RegistryLoader.DYNAMIC_REGISTRIES.add(0, new RegistryLoader.Entry<>(SCRIPT_TEMPLATE_REGISTRY_KEY, ScriptTemplate.CODEC));
		addBefore(RegistryKeys.STRUCTURE, SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(StructurePlacementScript.Holder.class));
		addBefore(RegistryKeys.CONFIGURED_CARVER, WOOD_PALETTE_REGISTRY_KEY, WoodPalette.CODEC);
		addBefore(RegistryKeys.CHUNK_GENERATOR_SETTINGS, LOCAL_NETHER_SETTINGS_REGISTRY_KEY, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LocalNetherSettings.class));
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
}