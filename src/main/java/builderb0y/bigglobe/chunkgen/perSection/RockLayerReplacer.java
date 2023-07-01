package builderb0y.bigglobe.chunkgen.perSection;

import java.util.stream.IntStream;

import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.rockLayers.LinkedRockLayerConfig;
import builderb0y.bigglobe.features.rockLayers.RockLayerEntryFeature;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.DelegatingContainedRandomList.RandomAccessDelegatingContainedRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;

public class RockLayerReplacer {

	public static void generateNew(long worldSeed, Chunk chunk, ChunkOfColumns<? extends WorldColumn> columns, int minY, int maxY, LinkedRockLayerConfig<?> config) {
		int minSection = minY >> 4;
		int maxSection = (maxY + 15) >> 4;
		int totalSections = maxSection - minSection;
		int threads = Math.min(Runtime.getRuntime().availableProcessors(), totalSections);
		//floorDivide(a + b - 1, b) == ceilDivide(a, b)
		int sectionsPerThread = (totalSections + threads - 1) / threads;
		IRandomList<? extends RockLayerEntryFeature.Entry> entries = new RandomAccessDelegatingContainedRandomList<>(config.entries);
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
						SectionGenerationContext context = SectionGenerationContext.forSectionCoord(chunk, section, layerSectionY, worldSeed, columns);
						PaletteIdReplacer replacer = entry.getReplacer(context);
						PaletteStorage storage = context.storage();
						int sectionMinY = context.startY();
						int sectionMaxY = sectionMinY | 15;

						for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
							int columnMinY = Math.max(columnMinYs[horizontalIndex], sectionMinY);
							int columnMaxY = Math.min(columnMaxYs[horizontalIndex], sectionMaxY);
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
		});
	}
}