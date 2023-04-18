package builderb0y.bigglobe.trees;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;

public class SaplingGrowHandler {

	public static Map<Block, RegistryEntry<ConfiguredFeature<?, ?>>> SAPLING_FEATURES;

	public static boolean replaceSaplingGrowth(ServerWorld world, BlockPos origin, BlockState saplingState, Random random) {
		if (SAPLING_FEATURES == null) {
			throw new IllegalStateException("Sapling attempted to grow without a running server???");
		}
		if (
			world.getChunkManager().getChunkGenerator() instanceof BigGlobeChunkGenerator
			? BigGlobeConfig.INSTANCE.get().bigGlobeTreesInBigGlobeWorlds
			: BigGlobeConfig.INSTANCE.get().bigGlobeTreesInOtherWorlds
		) {
			RegistryEntry<ConfiguredFeature<?, ?>> entry = SAPLING_FEATURES.get(saplingState.getBlock());
			if (entry != null) {
				entry.value().generate(world, world.getChunkManager().getChunkGenerator(), random, origin);
				return true;
			}
		}
		return false;
	}

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Registry<WoodPalette> registry = server.getRegistryManager().get(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY);
			SAPLING_FEATURES = new HashMap<>(registry.size());
			for (WoodPalette palette : registry) {
				if (palette.sapling_grow_feature != null) {
					Block saplingBlock = palette.blocks.get(WoodPaletteType.SAPLING);
					if (saplingBlock != null) {
						SAPLING_FEATURES.put(saplingBlock, palette.sapling_grow_feature);
					}
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SAPLING_FEATURES = null);
	}
}