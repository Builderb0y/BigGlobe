package builderb0y.bigglobe.trees;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.config.BigGlobeConfig;

public class SaplingGrowHandler {

	public static Map<Block, RegistryKey<ConfiguredFeature<?, ?>>> SAPLING_FEATURES;

	public static boolean replaceSaplingGrowth(ServerWorld world, BlockPos origin, BlockState saplingState, Random random) {
		if (SAPLING_FEATURES == null) {
			throw new IllegalStateException("Sapling attempted to grow before game finished loading???");
		}
		if (
			world.getChunkManager().getChunkGenerator() instanceof BigGlobeChunkGenerator
			? BigGlobeConfig.INSTANCE.get().bigGlobeTreesInBigGlobeWorlds
			: BigGlobeConfig.INSTANCE.get().bigGlobeTreesInOtherWorlds
		) {
			RegistryKey<ConfiguredFeature<?, ?>> key = SAPLING_FEATURES.get(saplingState.getBlock());
			if (key != null) {
				ConfiguredFeature<?, ?> feature = world.getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE).get(key);
				if (feature != null) {
					feature.generate(world, world.getChunkManager().getChunkGenerator(), random, origin);
					return true;
				}
			}
		}
		return false;
	}

	public static void init() {
		Map<Block, RegistryKey<ConfiguredFeature<?, ?>>> saplingFeatures = new HashMap<>(TreeRegistry.REGISTRY.size());
		for (TreeRegistry.Entry entry : TreeRegistry.REGISTRY) {
			if (entry.feature != null) {
				Block saplingBlock = entry.getBlock(TreeRegistry.Type.SAPLING);
				if (saplingBlock != Blocks.AIR) {
					saplingFeatures.put(saplingBlock, entry.feature);
				}
			}
		}
		SAPLING_FEATURES = saplingFeatures;
	}
}