package builderb0y.bigglobe.features;

import java.util.Set;
import com.mojang.serialization.Codec;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainer;
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
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

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
		ChunkAccess chunk,
		int minSection,
		int maxSection,
		Config config
	) {
		Prepared prepared = this.prepare(generator, worldWrapper, chunk, minSection, maxSection, config);
		Async.loop(BigGlobeThreadPool.autoExecutor(), prepared.sectionMinY, prepared.sectionMaxY + 1, 1, prepared::replaceSection);
	}

	@Override
	public Prepared prepare(
		BigGlobeScriptedChunkGenerator generator,
		WorldWrapper worldWrapper,
		ChunkAccess chunk,
		int minSection,
		int maxSection,
		Config config
	) {
		return new Prepared(generator, chunk, config);
	}

	public static class Prepared implements SectionReplacer {

		public final ChunkAccess chunk;
		public final Config config;
		public final long chunkSeed;
		public final int clampedMinY, clampedMaxY, sectionMinY, sectionMaxY;

		public Prepared(BigGlobeScriptedChunkGenerator generator, ChunkAccess chunk, Config config) {
			this.chunk = chunk;
			this.config = config;
			this.chunkSeed = Permuter.permute(generator.columnSeed ^ 0x6AF67A31DF787629L, chunk.getPos().x(), chunk.getPos().z());
			//the Y level at empty_y has a 0% chance of bedrock, so we can skip it.
			int adjustedEmptyY = config.empty_y + Integer.signum(config.full_y - config.empty_y);
			int minY = Math.min(config.full_y, adjustedEmptyY);
			int maxY = Math.max(config.full_y, adjustedEmptyY);
			this.clampedMinY = Math.max(minY, HeightLimitViewVersions.getMinY(chunk));
			this.clampedMaxY = Math.min(maxY, HeightLimitViewVersions.getMaxY(chunk) - 1);
			this.sectionMinY = this.clampedMinY >> 4;
			this.sectionMaxY = this.clampedMaxY >> 4;
		}

		@Override
		public void replaceSection(int yCoord) {
			if (yCoord < this.sectionMinY || yCoord > this.sectionMaxY) return;
			Config config = this.config;
			int startY = yCoord << 4;
			long sectionSeed = Permuter.permute(this.chunkSeed, yCoord);
			PalettedContainer<BlockState> container = this.chunk.getSection(this.chunk.getSectionIndexFromSectionY(yCoord)).getStates();
			int toID = SectionUtil.id(container, config.state);
			BitStorage storage = SectionUtil.storage(container);
			int minYRelative = Math.max(this.clampedMinY - startY, 0);
			int maxYRelative = Math.min(this.clampedMaxY - startY, 15);
			for (int index = minYRelative; index >>> 8 <= maxYRelative; index++) {
				int y = startY | (index >>> 8);
				long blockSeed = Permuter.permute(sectionSeed, index);
				double chance = BigGlobeMath.squareD(Interpolator.unmixLinear((double)(config.empty_y), (double)(config.full_y), (double)(y)));
				if (Permuter.nextChancedBoolean(blockSeed, chance)) {
					storage.set(index, toID);
				}
			}
		}

		@Override
		public void addPossibleOutputs(int yCoord, Set<BlockState> present) {
			if (yCoord >= this.sectionMinY && yCoord <= this.sectionMaxY) {
				present.add(this.config.state);
			}
		}
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