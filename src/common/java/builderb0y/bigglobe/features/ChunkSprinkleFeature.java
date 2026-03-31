package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Async;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.util.BlockState2ObjectMap;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

public class ChunkSprinkleFeature extends DummyFeature<ChunkSprinkleFeature.Config> implements RockReplacerFeature<ChunkSprinkleFeature.Config> {

	public ChunkSprinkleFeature(Codec<Config> codec) {
		super(codec);
	}

	public ChunkSprinkleFeature() {
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
		long chunkSeed = Permuter.permute(generator.columnSeed ^ 0x86F84DE15D2E462BL, chunk.getPos().x(), chunk.getPos().z());
		Async.loop(
			BigGlobeThreadPool.autoExecutor(), HeightLimitViewVersions.getSectionMinY(chunk), HeightLimitViewVersions.getSectionMaxY(chunk), 1, (int yCoord) -> {
				LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(yCoord));
				SectionGenerationContext context = SectionGenerationContext.forSectionCoord(chunk, section, yCoord);
				PaletteIdReplacer replacer = PaletteIdReplacer.of(context, config.blocks);
				if (replacer != null) {
					PalettedContainer<BlockState> container = section.getStates();
					BitStorage storage = SectionUtil.storage(container);
					long sectionSeed = Permuter.permute(chunkSeed, yCoord);
					for (int attempt = config.count; --attempt >= 0; ) {
						int index = ((int)(Permuter.permute(sectionSeed, attempt))) & 4095;
						int oldID = storage.get(index);
						int newID = replacer.getReplacement(oldID);
						if (oldID != newID) {
							storage.set(index, newID);
						}
					}
				}
			}
		);
	}

	public static class Config extends DummyConfig {

		public final BlockState2ObjectMap<@VerifyNormal BlockState> blocks;
		public final @VerifyIntRange(min = 0, max = 4096) int count;

		public Config(BlockState2ObjectMap<@VerifyNormal BlockState> blocks, int count) {
			this.blocks = blocks;
			this.count = count;
		}
	}
}