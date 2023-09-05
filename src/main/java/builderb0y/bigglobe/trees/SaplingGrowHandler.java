package builderb0y.bigglobe.trees;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.util.ServerValue;

public class SaplingGrowHandler {

	public static final ServerValue<Map<Block, List<RegistryEntry<ConfiguredFeature<?, ?>>>>>
		SAPLING_FEATURES = new ServerValue<>(SaplingGrowHandler::computeSaplingFeatures);

	public static boolean replaceSaplingGrowth(ServerWorld world, BlockPos origin, BlockState saplingState, Random random) {
		if (
			world.getChunkManager().getChunkGenerator() instanceof BigGlobeChunkGenerator
			? BigGlobeConfig.INSTANCE.get().bigGlobeTreesInBigGlobeWorlds
			: BigGlobeConfig.INSTANCE.get().bigGlobeTreesInOtherWorlds
		) {
			List<RegistryEntry<ConfiguredFeature<?, ?>>> list = SAPLING_FEATURES.get().get(saplingState.getBlock());
			if (list != null && !list.isEmpty()) {
				list
				.get(list.size() == 1 ? 0 : world.getRandom().nextInt(list.size()))
				.value()
				.generate(world, world.getChunkManager().getChunkGenerator(), random, origin);
				return true;
			}
		}
		return false;
	}

	public static Map<Block, List<RegistryEntry<ConfiguredFeature<?, ?>>>> computeSaplingFeatures() {
		Map<Block, List<RegistryEntry<ConfiguredFeature<?, ?>>>> map = new HashMap<>();
		for (
			WoodPalette palette
		:
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY)
		) {
			RegistryEntry<ConfiguredFeature<?, ?>> saplingGrowFeature = palette.getSaplingGrowFeature();
			if (saplingGrowFeature != null) {
				IRandomList<Block> blocks = palette.blocks.get(WoodPaletteType.SAPLING);
				if (blocks != null) {
					for (Block block : blocks) {
						map.computeIfAbsent(block, $ -> new ArrayList<>(1)).add(saplingGrowFeature);
					}
				}
			}
		}
		return map;
	}
}