package builderb0y.bigglobe.features;

import java.util.Map;
import java.util.Set;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.DelegatingContainedRandomList.RandomAccessDelegatingContainedRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.settings.Seed;
import builderb0y.bigglobe.settings.Seed.SeedModes;
import builderb0y.bigglobe.settings.VariationsList;
import builderb0y.bigglobe.util.Async;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class RockLayerFeature extends DummyFeature<RockLayerFeature.Config> implements RockReplacerFeature<RockLayerFeature.Config> {

	public RockLayerFeature(Codec<Config> codec) {
		super(codec);
	}

	public RockLayerFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public void replaceRocks(BigGlobeScriptedChunkGenerator generator, WorldWrapper worldWrapper, ChunkAccess chunk, int minSection, int maxSection, Config config) {
		SectionReplacer replacer = this.prepare(generator, worldWrapper, chunk, minSection, maxSection, config);
		if (replacer != null) {
			Async.loop(BigGlobeThreadPool.autoExecutor(), minSection, maxSection, 1, replacer::replaceSection);
		}
	}

	@Override
	public @Nullable SectionReplacer prepare(BigGlobeScriptedChunkGenerator generator, WorldWrapper worldWrapper, ChunkAccess chunk, int minSection, int maxSection, Config config) {
		IRandomList<Entry> entries = new RandomAccessDelegatingContainedRandomList<>(config.entries.elements);
		long configSeed = config.seed.xor(generator.columnSeed);
		int startX = chunk.getPos().getMinBlockX();
		int startZ = chunk.getPos().getMinBlockZ();
		int minLayer = BigGlobeMath.ceilI(((minSection << 4) - config.maxWindow) / config.repeat);
		int maxLayer = BigGlobeMath.floorI(((maxSection << 4) - config.minWindow) / config.repeat);
		int layerCount = maxLayer - minLayer + 1;
		if (maxSection <= minSection || layerCount <= 0) return null;

		//lookupColumn() and lazily-computed column values are not thread-safe,
		//so the layers are computed up-front on one thread,
		//and only the block replacement is done in parallel.
		ScriptedColumn[] columns = new ScriptedColumn[256];
		for (int index = 0; index < 256; index++) {
			columns[index] = worldWrapper.lookupColumn(startX | (index & 15), startZ | (index >>> 4));
		}
		Entry[] layerEntries = new Entry[layerCount];
		int[] layerMinYs = new int[layerCount];
		int[] layerMaxYs = new int[layerCount];
		int[] columnMinYs = new int[layerCount << 8];
		int[] columnMaxYs = new int[layerCount << 8];
		try (
			NumberArray centerSamples = NumberArray.allocateDoublesDirect(16);
			NumberArray thicknessSamples = NumberArray.allocateDoublesDirect(16);
		) {
			for (int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
				int layer = minLayer + layerIndex;
				long layerSeed = Permuter.permute(configSeed, layer);
				Entry entry = entries.getRandomElement(layerSeed);
				layerEntries[layerIndex] = entry;
				double averageCenter = layer * config.repeat;
				int layerMinY = Integer.MAX_VALUE;
				int layerMaxY = Integer.MIN_VALUE;
				for (int relativeZ = 0; relativeZ < 16; relativeZ++) {
					entry.center().getBulkX(layerSeed, startX, startZ | relativeZ, centerSamples);
					entry.thickness().getBulkX(layerSeed, startX, startZ | relativeZ, thicknessSamples);
					for (int relativeX = 0; relativeX < 16; relativeX++) {
						int index = (relativeZ << 4) | relativeX;
						double center = centerSamples.implGetD(relativeX) + averageCenter;
						double thickness = thicknessSamples.implGetD(relativeX) - (1.0D - entry.restrictions().getRestriction(columns[index], BigGlobeMath.floorI(center))) * entry.thickness().maxValue();
						int columnMinY = BigGlobeMath.floorI(center - thickness);
						int columnMaxY = BigGlobeMath.floorI(center + thickness);
						columnMinYs[(layerIndex << 8) | index] = columnMinY;
						columnMaxYs[(layerIndex << 8) | index] = columnMaxY;
						layerMinY = Math.min(layerMinY, columnMinY);
						layerMaxY = Math.max(layerMaxY, columnMaxY);
					}
				}
				layerMinYs[layerIndex] = layerMinY;
				layerMaxYs[layerIndex] = layerMaxY;
			}
		}

		return new SectionReplacer() {

			public boolean intersects(int layerIndex, int sectionCoord) {
				int layerMinY = layerMinYs[layerIndex];
				int layerMaxY = layerMaxYs[layerIndex];
				return layerMaxY >= layerMinY && (layerMinY >> 4) <= sectionCoord && (layerMaxY >> 4) >= sectionCoord;
			}

			@Override
			public void replaceSection(int sectionCoord) {
				if (sectionCoord < minSection || sectionCoord >= maxSection) return;
				SectionGenerationContext context = null;
				for (int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
					if (!this.intersects(layerIndex, sectionCoord)) continue;
					if (context == null) {
						context = SectionGenerationContext.forSectionCoord(chunk, chunk.getSection(chunk.getSectionIndexFromSectionY(sectionCoord)), sectionCoord);
					}
					PaletteIdReplacer replacer = layerEntries[layerIndex].getReplacer(context);
					if (replacer != null) {
						BitStorage storage = context.storage();
						int sectionMinY = context.startY();
						int sectionMaxY = sectionMinY | 15;

						for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
							int columnMinY = Math.max(columnMinYs[(layerIndex << 8) | horizontalIndex], sectionMinY);
							int columnMaxY = Math.min(columnMaxYs[(layerIndex << 8) | horizontalIndex], sectionMaxY);
							for (int columnY = columnMinY; columnY <= columnMaxY; columnY++) {
								int relativeY = columnY & 15;
								int index = (relativeY << 8) | horizontalIndex;
								int oldID = storage.get(index);
								int newID = replacer.getReplacement(oldID);
								if (oldID != newID) {
									storage.set(index, newID);
								}
							}
						}
					}
				}
			}

			@Override
			public void addPossibleOutputs(int sectionCoord, Set<BlockState> present) {
				if (sectionCoord < minSection || sectionCoord >= maxSection) return;
				for (int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
					if (this.intersects(layerIndex, sectionCoord)) {
						addReplacements(layerEntries[layerIndex].blocks(), present);
					}
				}
			}
		};
	}

	public static void addReplacements(BlockState2ObjectMap<BlockState> blocks, Set<BlockState> present) {
		for (Map.Entry<BlockState, BlockState> replacement : blocks.runtimeStates.entrySet()) {
			if (present.contains(replacement.getKey())) {
				present.add(replacement.getValue());
			}
		}
	}

	public static class Config extends DummyConfig {

		public final @SeedModes(Seed.NUMBER | Seed.STRING) Seed seed;
		public final @VerifyFloatRange(min = 0.0D, minInclusive = false) double repeat;
		public final VariationsList<Entry> entries;
		public final transient double minWindow, maxWindow;

		public Config(Seed seed, double repeat, VariationsList<Entry> entries) {
			this.seed = seed;
			this.repeat = repeat;
			this.entries = entries;
			this.minWindow = entries.elements.stream().mapToDouble((Entry entry) -> entry.center.minValue() - entry.thickness.maxValue()).min().orElse(0.0D);
			this.maxWindow = entries.elements.stream().mapToDouble((Entry entry) -> entry.center.maxValue() + entry.thickness.maxValue()).max().orElse(0.0D);
		}
	}

	public static record Entry(
		double weight,
		Grid2D center,
		Grid2D thickness,
		BlockState2ObjectMap<@VerifyNormal BlockState> blocks,
		ColumnRestriction restrictions
	)
		implements IWeightedListElement {

		@Override
		public double getWeight() {
			return this.weight;
		}

		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			return PaletteIdReplacer.of(context, this.blocks);
		}
	}
}