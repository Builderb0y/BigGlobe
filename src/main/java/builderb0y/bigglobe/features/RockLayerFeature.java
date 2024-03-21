package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixins.SingularPalette_EntryAccess;
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

	public static boolean isEmpty(ChunkSection section) {
		return SectionUtil.palette(section.getBlockStateContainer()) instanceof SingularPalette_EntryAccess palette && palette.bigglobe_getEntry() == BlockStates.AIR;
	}

	@Override
	public void replaceRocks(BigGlobeScriptedChunkGenerator generator, WorldWrapper worldWrapper, Chunk chunk, int minSection, int maxSection, Config config) {
		int totalSections = maxSection - minSection;
		int threads = Math.min(Runtime.getRuntime().availableProcessors(), totalSections);
		//floorDivide(a + b - 1, b) == ceilDivide(a, b)
		int sectionsPerThread = (totalSections + threads - 1) / threads;
		IRandomList<Entry> entries = new RandomAccessDelegatingContainedRandomList<>(config.entries.elements);
		Async.loop(BigGlobeThreadPool.INSTANCE.autoExecutor(), threads, (int thread) -> {
			try (
				NumberArray centerSamples    = NumberArray.allocateDoublesDirect(16);
				NumberArray thicknessSamples = NumberArray.allocateDoublesDirect(16);
				NumberArray columnMinYs      = NumberArray.allocateIntsDirect(256);
				NumberArray columnMaxYs      = NumberArray.allocateIntsDirect(256);
			) {
				long configSeed   = config.seed.xor(generator.worldSeed);
				int  startX       = chunk.getPos().getStartX();
				int  startZ       = chunk.getPos().getStartZ();
				int  startSection =          sectionsPerThread *  thread      + minSection             ;
				int  endSection   = Math.min(sectionsPerThread * (thread + 1) + minSection, maxSection);
				int  minThreadY   = startSection << 4;
				int  maxThreadY   =   endSection << 4;
				int  minLayer     = BigGlobeMath. ceilI((minThreadY - config.maxWindow) / config.repeat);
				int  maxLayer     = BigGlobeMath.floorI((maxThreadY - config.minWindow) / config.repeat);
				for (int layer = minLayer; layer <= maxLayer; layer++) {
					long layerSeed = Permuter.permute(configSeed, layer);
					RockLayerFeature.Entry entry = entries.getRandomElement(layerSeed);
					double averageCenter = layer * config.repeat;
					int layerMinY = Integer.MAX_VALUE;
					int layerMaxY = Integer.MIN_VALUE;
					for (int relativeZ = 0; relativeZ < 16; relativeZ++) {
						entry.center().getBulkX(layerSeed, startX, startZ | relativeZ, centerSamples);
						entry.thickness().getBulkX(layerSeed, startX, startZ | relativeZ, thicknessSamples);
						for (int relativeX = 0; relativeX < 16; relativeX++) {
							int index = (relativeZ << 4) | relativeX;
							double center = centerSamples.getD(relativeX) + averageCenter;
							double thickness = thicknessSamples.getD(relativeX) - (1.0D - entry.restrictions().getRestriction(worldWrapper.lookupColumn(startX | relativeX, startZ | relativeZ), BigGlobeMath.floorI(center))) * entry.thickness().maxValue();
							columnMinYs.setI(index, BigGlobeMath.floorI(center - thickness));
							columnMaxYs.setI(index, BigGlobeMath.floorI(center + thickness));
							layerMinY = Math.min(layerMinY, columnMinYs.getI(index));
							layerMaxY = Math.max(layerMaxY, columnMaxYs.getI(index));
						}
					}

					if (layerMaxY >= layerMinY) {
						int layerSectionMinY = Math.max(layerMinY >> 4, startSection);
						int layerSectionMaxY = Math.min(layerMaxY >> 4, endSection - 1);
						for (int layerSectionY = layerSectionMinY; layerSectionY <= layerSectionMaxY; layerSectionY++) {
							ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(layerSectionY));
							if (isEmpty(section)) continue;
							SectionGenerationContext context = SectionGenerationContext.forSectionCoord(chunk, section, layerSectionY, generator.worldSeed);
							PaletteIdReplacer replacer = entry.getReplacer(context);
							PaletteStorage storage = context.storage();
							int sectionMinY = context.startY();
							int sectionMaxY = sectionMinY | 15;

							for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
								int columnMinY = Math.max(columnMinYs.getI(horizontalIndex), sectionMinY);
								int columnMaxY = Math.min(columnMaxYs.getI(horizontalIndex), sectionMaxY);
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
			}
		});
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