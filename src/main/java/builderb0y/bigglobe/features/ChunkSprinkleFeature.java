package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PalettedContainer;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.Async;

public class ChunkSprinkleFeature extends DummyFeature<ChunkSprinkleFeature.Config> implements RockReplacerFeature<ChunkSprinkleFeature.Config> {

	public ChunkSprinkleFeature(Codec<Config> codec) {
		super(codec);
	}

	public ChunkSprinkleFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public void replaceRocks(BigGlobeScriptedChunkGenerator generator, Chunk chunk, Config config) {
		long chunkSeed = Permuter.permute(generator.worldSeed ^ 0x86F84DE15D2E462BL, chunk.getPos().x, chunk.getPos().z);
		Async.loop(chunk.getBottomSectionCoord(), chunk.getTopSectionCoord(), 1, (int yCoord) -> {
			PalettedContainer<BlockState> container = chunk.getSection(chunk.sectionCoordToIndex(yCoord)).getBlockStateContainer();
			if (container.hasAny(config.from::equals)) {
				long sectionSeed = Permuter.permute(chunkSeed, yCoord);
				PaletteStorage storage = SectionUtil.storage(container);
				int fromID = SectionUtil.id(container, config.from);
				int toID = SectionUtil.id(container, config.to);
				if (storage != (storage = SectionUtil.storage(container))) { //resize.
					fromID = SectionUtil.id(container, config.from);
					toID = SectionUtil.id(container, config.to);
					assert storage == SectionUtil.storage(container);
				}
				for (int attempt = config.count; --attempt >= 0;) {
					int index = ((int)(Permuter.permute(sectionSeed, attempt))) & 4095;
					int old = storage.get(index);
					if (old == fromID) {
						storage.set(index, toID);
					}
				}
			}
		});
	}

	public static class Config extends DummyConfig {

		public final @VerifyNormal BlockState from, to;
		public final @VerifyIntRange(min = 0, max = 4096) int count;

		public Config(BlockState from, BlockState to, int count) {
			this.from = from;
			this.to = to;
			this.count = count;
		}
	}
}