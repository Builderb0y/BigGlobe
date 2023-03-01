package builderb0y.bigglobe.chunkgen.perSection;

import java.util.stream.IntStream;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.features.rockLayers.LinkedRockLayerConfig;
import builderb0y.bigglobe.features.rockLayers.RockLayerEntryFeature;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.DelegatingContainedRandomList.RandomAccessDelegatingContainedRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;

public class RockLayerResplacer {

	/*
	public static void compute(
		BitSet[] bitSets,
		RockLayerFeature.Config config,
		ChunkOfColumns<OverworldColumn> columns,
		int minY,
		int maxY
	) {
		double minWindow = config.center.minValue() - config.thickness.maxValue();
		double maxWindow = config.center.maxValue() + config.thickness.maxValue();
		IntStream.range(0, 16).parallel().forEach(relativeZ -> {
			for (int relativeX = 0; relativeX < 16; relativeX++) {
				bitSets[(relativeZ << 4) | relativeX].clear();
			}
			int startX = columns.getColumn(0).x;
			int startZ = columns.getColumn(0).z;
			int minLayer = BigGlobeMath.ceilI((minY - maxWindow) / config.repeat);
			int maxLayer = BigGlobeMath.floorI((maxY - minWindow) / config.repeat);
			double[] centerSamples = new double[16];
			double[] thicknessSamples = new double[16];
			for (int layer = minLayer; layer <= maxLayer; layer++) {
				long seed = Permuter.permute(columns.getColumn(0).seed ^ config.nameHash, layer);
				config.center.getBulkX(seed, startX, startZ | relativeZ, centerSamples, 16);
				config.thickness.getBulkX(seed, startX, startZ | relativeZ, thicknessSamples, 16);
				for (int relativeX = 0; relativeX < 16; relativeX++) {
					int index = (relativeZ << 4) | relativeX;
					BitSet bits = bitSets[index];
					double center = centerSamples[relativeX] + layer * config.repeat;
					if (!Double.isNaN(center)) {
						OverworldColumn column = columns.getColumn(index);
						double thickness = thicknessSamples[relativeX] - (
							(1.0D - config.restrictions.getRestriction(column, center))
							* config.thickness.maxValue()
						);
						if (thickness > 0.0D) {
							double fromD = center - thickness;
							double toD = center + thickness;
							int from = BigGlobeMath.floorI(fromD);
							int to = BigGlobeMath.floorI(toD);
							to = Math.min(to + 1 / * convert to exclusive * /, column.getFinalTopHeightI());
							from = Math.max(from - minY, 0);
							to -= minY;
							if (to > from) bits.set(from, to);
						}
					}
				}
			}
		});
	}

	public static void place(int minY, SectionGenerationContext context, RockLayerFeature.Config config, BitSet[] bitSets) {
		PaletteStorage storage = context.storage();
		int fromStoneID  = context.id(BlockStates.STONE);
		int fromCobbleID = context.id(BlockStates.COBBLESTONE);
		int toStoneID    = context.id(config.smooth_state);
		int toCobbleID   = context.id(config.cobble_state);
		if (storage != (storage = context.storage())) { //resize.
			fromStoneID  = context.id(BlockStates.STONE);
			fromCobbleID = context.id(BlockStates.COBBLESTONE);
			toStoneID    = context.id(config.smooth_state);
			toCobbleID   = context.id(config.cobble_state);
			assert storage == context.storage();
		}
		int startY = context.startY();
		for (int index = 0; index < 4096; index++) {
			int x = index & 15;
			int y = index >>> 8;
			int z = (index >>> 4) & 15;
			BitSet bits = bitSets[(z << 4) | x];
			if (bits.get((startY | y) - minY)) {
				int oldID = storage.get(index);
				if (oldID == fromStoneID) {
					storage.set(index, toStoneID);
				}
				else if (oldID == fromCobbleID) {
					storage.set(index, toCobbleID);
				}
			}
		}
	}

	public static void generate(SectionGenerationContext context, RockLayerFeature.Config config) {
		PaletteStorage storage = context.storage();
		int fromStoneID  = context.id(BlockStates.STONE);
		int fromCobbleID = context.id(BlockStates.COBBLESTONE);
		int toStoneID    = context.id(config.smooth_state);
		int toCobbleID   = context.id(config.cobble_state);
		if (storage != (storage = context.storage())) { //resize.
			fromStoneID  = context.id(BlockStates.STONE);
			fromCobbleID = context.id(BlockStates.COBBLESTONE);
			toStoneID    = context.id(config.smooth_state);
			toCobbleID   = context.id(config.cobble_state);
			assert storage == context.storage();
		}
		double[] centerSamples = new double[16];
		double[] thicknessSamples = new double[16];
		for (int z = 0; z < 16; z++) {
			double minWindow =  Double.MAX_VALUE;
			double maxWindow = -Double.MAX_VALUE;
			for (int x = 0; x < 16; x++) {
				double y = ((OverworldColumn)(context.columns.getColumn((z << 4) | x))).getPreCliffHeight();
				if (y < minWindow) minWindow = y;
				if (y > maxWindow) maxWindow = y;
			}
			minWindow += config.center.minValue() - config.thickness.maxValue();
			maxWindow += config.center.maxValue() + config.thickness.maxValue();
			int minLayer = BigGlobeMath.ceilI((context.startY() - maxWindow) / config.repeat);
			int maxLayer = BigGlobeMath.floorI((context.endY() - minWindow) / config.repeat);

			for (int layer = minLayer; layer <= maxLayer; layer++) {
				long seed = Permuter.permute(context.worldSeed() ^ config.nameHash, layer);
				config.center.getBulkX(seed, context.startX(), context.startZ() | z, centerSamples, 16);
				config.thickness.getBulkX(seed, context.startX(), context.startZ() | z, thicknessSamples, 16);
				for (int x = 0; x < 16; x++) {
					OverworldColumn column = (OverworldColumn)(context.columns.getColumn((z << 4) | x));
					double center = (
						+ centerSamples[x]
						+ column.getPreCliffHeight()
						+ layer * config.repeat
					);
					double thickness = thicknessSamples[x] - (
						(1.0D - config.restrictions.getRestriction(column, center))
						* config.thickness.maxValue()
					);
					if (!Double.isNaN(center) && thickness > 0.0D) {
						int minY = Math.max(BigGlobeMath. ceilI(center - thickness) - context.startY(),  0);
						int maxY = Math.min(BigGlobeMath.floorI(center + thickness) - context.startY(), 15);
						for (int y = minY; y <= maxY; y++) {
							int index = x | (z << 4) | (y << 8);
							int oldID = storage.get(index);
							if (oldID == fromStoneID) {
								storage.set(index, toStoneID);
							}
							else if (oldID == fromCobbleID) {
								storage.set(index, toCobbleID);
							}
						}
					}
				}
			}
		}
	}
	*/

	public static void generateNew(long worldSeed, Chunk chunk, ChunkOfColumns<OverworldColumn> columns, int minY, int maxY, LinkedRockLayerConfig config) {
		int minSection = minY >> 4;
		int maxSection = (maxY + 15) >> 4;
		int totalSections = maxSection - minSection;
		int threads = Math.min(Runtime.getRuntime().availableProcessors(), totalSections);
		//floorDivide(a + b - 1, b) == ceilDivide(a, b)
		int sectionsPerThread = (totalSections + threads - 1) / threads;
		IRandomList<RockLayerEntryFeature.Entry> entries = new RandomAccessDelegatingContainedRandomList<>(config.entries);
		IntStream.range(0, threads).parallel().forEach((int thread) -> {
			long     configSeed       = worldSeed ^ config.nameHash;
			int      startX           = chunk.getPos().getStartX();
			int      startZ           = chunk.getPos().getStartZ();
			double[] centerSamples    = new double[16];
			double[] thicknessSamples = new double[16];
			int[]    columnMinYs      = new int[256];
			int[]    columnMaxYs      = new int[256];
			int      startSection     =          sectionsPerThread *  thread      + minSection             ;
			int      endSection       = Math.min(sectionsPerThread * (thread + 1) + minSection, maxSection);
			int      minThreadY       = startSection << 4;
			int      maxThreadY       = endSection   << 4;
			int      minLayer         = BigGlobeMath. ceilI((minThreadY - config.maxWindow) / config.group.repeat);
			int      maxLayer         = BigGlobeMath.floorI((maxThreadY - config.minWindow) / config.group.repeat);
			for (int layer = minLayer; layer <= maxLayer; layer++) {
				long layerSeed = Permuter.permute(configSeed, layer);
				RockLayerEntryFeature.Entry entry = entries.getRandomElement(layerSeed);
				double averageCenter = layer * config.group.repeat;
				int layerMinY = Integer.MAX_VALUE;
				int layerMaxY = Integer.MIN_VALUE;
				for (int relativeZ = 0; relativeZ < 16; relativeZ++) {
					entry.center.getBulkX(layerSeed, startX, startZ | relativeZ, centerSamples, 16);
					entry.thickness.getBulkX(layerSeed, startX, startZ | relativeZ, thicknessSamples, 16);
					for (int relativeX = 0; relativeX < 16; relativeX++) {
						int index = (relativeZ << 4) | relativeX;
						double center = centerSamples[relativeX] + averageCenter;
						double thickness = thicknessSamples[relativeX] - (1.0D - entry.restrictions.getRestriction(columns.getColumn(index), center)) * entry.thickness.maxValue();
						columnMinYs[index] = BigGlobeMath.floorI(center - thickness);
						columnMaxYs[index] = BigGlobeMath.floorI(center + thickness);
						layerMinY = Math.min(layerMinY, columnMinYs[index]);
						layerMaxY = Math.max(layerMaxY, columnMaxYs[index]);
					}
				}

				if (layerMaxY >= layerMinY) {
					int layerSectionMinY = Math.max(layerMinY >> 4, startSection);
					int layerSectionMaxY = Math.min(layerMaxY >> 4, endSection - 1);
					for (int layerSectionY = layerSectionMinY; layerSectionY <= layerSectionMaxY; layerSectionY++) {
						ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(layerSectionY));
						PalettedContainer<BlockState> container = section.getBlockStateContainer();
						PaletteStorage storage = SectionUtil.storage(container);
						int fromStoneID = SectionUtil.id(container, BlockStates.STONE);
						int fromCobbleID = SectionUtil.id(container, BlockStates.COBBLESTONE);
						int toStoneID = SectionUtil.id(container, entry.smooth_state);
						int toCobbleID = SectionUtil.id(container, entry.cobble_state);
						if (storage != (storage = SectionUtil.storage(container))) { //resize.
							fromStoneID = SectionUtil.id(container, BlockStates.STONE);
							fromCobbleID = SectionUtil.id(container, BlockStates.COBBLESTONE);
							toStoneID = SectionUtil.id(container, entry.smooth_state);
							toCobbleID = SectionUtil.id(container, entry.cobble_state);
							assert storage == SectionUtil.storage(container);
						}
						int sectionMinY = section.getYOffset();
						int sectionMaxY = sectionMinY | 15;

						for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
							int columnMinY = Math.max(columnMinYs[horizontalIndex], sectionMinY);
							int columnMaxY = Math.min(columnMaxYs[horizontalIndex], sectionMaxY);
							for (int columnY = columnMinY; columnY <= columnMaxY; columnY++) {
								int relativeY = columnY & 15;
								int index = (relativeY << 8) | horizontalIndex;
								int oldID = storage.get(index);
								if (oldID == fromStoneID) {
									storage.set(index, toStoneID);
								}
								else if (oldID == fromCobbleID) {
									storage.set(index, toCobbleID);
								}
							}
						}
					}
				}
			}
		});
	}
}