package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PalettedContainer;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Async;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public class MoltenRockReplacerFeature extends DummyFeature<MoltenRockReplacerFeature.Config> implements RockReplacerFeature<MoltenRockReplacerFeature.Config> {

	public static final BlockState[] STATES = new BlockState[9];
	static {
		STATES[0] = BlockStates.STONE;
		for (int index = 1; index <= 8; index++) {
			STATES[index] = BigGlobeBlocks.MOLTEN_ROCKS[index - 1].getDefaultState();
		}
	}

	public MoltenRockReplacerFeature(Codec<Config> codec) {
		super(codec);
	}

	public MoltenRockReplacerFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public void replaceRocks(
		BigGlobeScriptedChunkGenerator generator,
		WorldWrapper worldWrapper,
		Chunk chunk,
		int minSection,
		int maxSection,
		Config config
	) {
		long chunkSeed = Permuter.permute(generator.worldSeed ^ 0x4A53BB0CBE6FEB95L, chunk.getPos().x, chunk.getPos().z);
		int adjustedColdY = config.cold_y + Integer.signum(config.hot_y  - config.cold_y);
		int adjustedHotY  = config.hot_y  + Integer.signum(config.cold_y - config.hot_y );
		int minY = Math.min(adjustedColdY, adjustedHotY);
		int maxY = Math.max(adjustedColdY, adjustedHotY);
		int clampedMinY = Math.max(minY, chunk.getBottomY());
		int clampedMaxY = Math.min(maxY, chunk.getTopY() - 1);
		int sectionMinY = clampedMinY >> 4;
		int sectionMaxY = clampedMaxY >> 4;
		Async.loop(BigGlobeThreadPool.autoExecutor(), sectionMinY, sectionMaxY + 1, 1, (int yCoord) -> {
			int startY = yCoord << 4;
			long sectionSeed = Permuter.permute(chunkSeed, yCoord);
			PalettedContainer<BlockState> container = chunk.getSection(chunk.sectionCoordToIndex(yCoord)).getBlockStateContainer();
			int minRelativeY = Math.max(clampedMinY - startY,  0);
			int maxRelativeY = Math.min(clampedMaxY - startY, 15);
			for (int relativeY = minRelativeY; relativeY <= maxRelativeY; relativeY++) {
				int y = startY | relativeY;
				double heat = Interpolator.unmixLinear((double)(config.cold_y), (double)(config.hot_y), (double)(y)) * 8.0D;
				BlockState floorHeatState = STATES[BigGlobeMath.floorI(heat)];
				BlockState  ceilHeatState = STATES[BigGlobeMath. ceilI(heat)];
				heat = BigGlobeMath.modulus_BP(heat, 1.0D);
				PaletteStorage storage = SectionUtil.storage(container);
				int from = SectionUtil.id(container, config.replace);
				int to1 = SectionUtil.id(container, floorHeatState);
				int to2 = SectionUtil.id(container,  ceilHeatState);
				if (storage != (storage = SectionUtil.storage(container))) {
					from = SectionUtil.id(container, config.replace);
					to1 = SectionUtil.id(container, floorHeatState);
					to2 = SectionUtil.id(container,  ceilHeatState);
					assert storage == SectionUtil.storage(container);
				}
				for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
					int blockIndex = (relativeY << 8) | horizontalIndex;
					if (storage.get(blockIndex) == from) {
						long blockSeed = Permuter.permute(sectionSeed, blockIndex);
						storage.set(blockIndex, Permuter.nextChancedBoolean(blockSeed, heat) ? to2 : to1);
					}
				}
			}
		});
	}

	public static class Config extends DummyConfig {

		public final int cold_y, hot_y;
		public final BlockState replace;

		public Config(int cold_y, int hot_y, BlockState replace) {
			this.cold_y = cold_y;
			this.hot_y = hot_y;
			this.replace = replace;
		}
	}
}