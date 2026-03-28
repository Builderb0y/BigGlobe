package builderb0y.bigglobe.trees;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.util.ServerValue;

public class SaplingGrowHandler {

	public static final ServerValue<Map<Block, List<Holder<ConfiguredFeature<?, ?>>>>>
		SAPLING_FEATURES = new ServerValue<>(SaplingGrowHandler::computeSaplingFeatures);

	public static boolean replaceSaplingGrowth(ServerLevel world, BlockPos origin, BlockState saplingState, RandomSource random) {
		if (BigGlobeConfig.INSTANCE.get().bigGlobeTreesInBigGlobeWorlds && world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			List<Holder<ConfiguredFeature<?, ?>>> list = SAPLING_FEATURES.get().get(saplingState.getBlock());
			if (list != null && !list.isEmpty()) {
				list
					.get(list.size() == 1 ? 0 : world.getRandom().nextInt(list.size()))
					.value()
					.place(world, generator, random, origin);
				return true;
			}
		}
		return false;
	}

	public static Map<Block, List<Holder<ConfiguredFeature<?, ?>>>> computeSaplingFeatures() {
		Map<Block, List<Holder<ConfiguredFeature<?, ?>>>> map = new HashMap<>();
		for (
			WoodPalette palette
			:
			BigGlobeMod
				.getRegistry(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY)
				.values()
		) {
			Holder<ConfiguredFeature<?, ?>> saplingGrowFeature = palette.getSaplingGrowFeature();
			if (saplingGrowFeature != null) {
				IRandomList<Holder<Block>> blocks = palette.blocks.get(WoodPaletteType.SAPLING);
				if (blocks != null) {
					for (Holder<Block> block : blocks) {
						map.computeIfAbsent(block.value(), $ -> new ArrayList<>(1)).add(saplingGrowFeature);
					}
				}
			}
		}
		return map;
	}
}