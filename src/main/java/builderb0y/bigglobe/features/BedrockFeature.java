package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PalettedContainer;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Async;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public class BedrockFeature extends DummyFeature<BedrockFeature.Config> implements RockReplacerFeature<BedrockFeature.Config> {

	public BedrockFeature(Codec<Config> codec) {
		super(codec);
	}

	public BedrockFeature() {
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
		long chunkSeed = Permuter.permute(generator.worldSeed ^ 0x6AF67A31DF787629L, chunk.getPos().x, chunk.getPos().z);
		//the Y level at empty_y has a 0% chance of bedrock, so we can skip it.
		int adjustedEmptyY = config.empty_y + Integer.signum(config.full_y - config.empty_y);
		int minY = Math.min(config.full_y, adjustedEmptyY);
		int maxY = Math.max(config.full_y, adjustedEmptyY);
		int clampedMinY = Math.max(minY, chunk.getBottomY());
		int clampedMaxY = Math.min(maxY, chunk.getTopY() - 1);
		int sectionMinY = clampedMinY >> 4;
		int sectionMaxY = clampedMaxY >> 4;
		Async.loop(BigGlobeThreadPool.INSTANCE.autoExecutor(), sectionMinY, sectionMaxY + 1, 1, (int yCoord) -> {
			int startY = yCoord << 4;
			long sectionSeed = Permuter.permute(chunkSeed, yCoord);
			PalettedContainer<BlockState> container = chunk.getSection(chunk.sectionCoordToIndex(yCoord)).getBlockStateContainer();
			int toID = SectionUtil.id(container, config.state);
			PaletteStorage storage = SectionUtil.storage(container);
			int minYRelative = Math.max(clampedMinY - startY,  0);
			int maxYRelative = Math.min(clampedMaxY - startY, 15);
			for (int index = minYRelative; index >>> 8 <= maxYRelative; index++) {
				int y = startY | (index >>> 8);
				long blockSeed = Permuter.permute(sectionSeed, index);
				double chance = BigGlobeMath.squareD(Interpolator.unmixLinear((double)(config.empty_y), (double)(config.full_y), (double)(y)));
				if (Permuter.nextChancedBoolean(blockSeed, chance)) {
					storage.set(index, toID);
				}
			}
		});
	}

	public static class Config extends DummyConfig {

		public final @VerifyNormal BlockState state;
		public final int full_y, empty_y;

		public Config(BlockState state, int full_y, int empty_y) {
			this.state = state;
			this.full_y = full_y;
			this.empty_y = empty_y;
		}
	}
}