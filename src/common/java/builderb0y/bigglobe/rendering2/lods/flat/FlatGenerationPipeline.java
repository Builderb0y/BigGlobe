package builderb0y.bigglobe.rendering2.lods.flat;

import java.util.Arrays;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.chunkgen.QuadHolder.QuadColumn;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.rendering2.lods.ColumnBlockGetter;
import builderb0y.bigglobe.rendering2.lods.GenerationPipeline;
import builderb0y.bigglobe.rendering2.lods.LodGenerator;
import builderb0y.bigglobe.rendering2.lods.LodGenerator.LoadMode;

public class FlatGenerationPipeline extends GenerationPipeline {

	public FlatGenerationPipeline(FlatLodSystem system, FlatLoadingLodGenerator generator) {
		super(system, generator);
	}

	@Override
	public ColumnBlockGetter generateWithPadding(BoundingBox area, byte lod, LoadMode mode) {
		final int rawPadding = 2;
		int padding = rawPadding << lod;
		byte prevLod = (byte)(Math.max(lod - 1, 0));
		LodGenerator<?> generator = this.generator;
		ColumnBlockGetter center = generator.generateRegion(area, lod, mode);
		if (center == null) return null;
		mode = mode.allowGeneration();
		//         posZ
		//------+--------+------
		// negX | center | posX
		//------+--------+------
		//         negZ
		ColumnBlockGetter
			posX = generator.generateRegion(new BoundingBox(area.maxX() + 1,       area.minY(), area.minZ(),           area.maxX() + padding, area.maxY(), area.maxZ()          ), prevLod, mode),
			negX = generator.generateRegion(new BoundingBox(area.minX() - padding, area.minY(), area.minZ(),           area.minX() - 1,       area.maxY(), area.maxZ()          ), prevLod, mode),
			posZ = generator.generateRegion(new BoundingBox(area.minX() - padding, area.minY(), area.maxZ() + 1,       area.maxX() + padding, area.maxY(), area.maxZ() + padding), prevLod, mode),
			negZ = generator.generateRegion(new BoundingBox(area.minX() - padding, area.minY(), area.minZ() - padding, area.maxX() + padding, area.maxY(), area.minZ() - 1      ), prevLod, mode);
		int combinedArea = ((area.getXSpan() >> lod) + (rawPadding << 1)) * ((area.getZSpan() >> lod) + (rawPadding << 1));
		ColumnBlockGetter combined = new ColumnBlockGetter(
			new BlockSegmentList[combinedArea],
			new ScriptedColumn[combinedArea],
			center.unpaddedVolume,
			center.unpaddedVolume.inflatedBy(rawPadding, 0, rawPadding),
			lod,
			center.ambientLight,
			center.colorOverrides,
			center.biomeSource,
			center.lighting,
			(ScriptedColumn[] toRecycle) -> generator.columnRecycler.addAll(Arrays.asList(toRecycle))
		);
		for (int z = center.unpaddedVolume.minZ(); z <= center.unpaddedVolume.maxZ(); z++) {
			for (int x = center.unpaddedVolume.minX(); x <= center.unpaddedVolume.maxX(); x++) {
				int fromIndex = center.columnIndex(x, z);
				int toIndex = combined.columnIndex(x, z);
				combined.columns[toIndex] = center.columns[fromIndex];
				combined.storage[toIndex] = center.storage[fromIndex];
			}
		}
		int
			sizeX = area.getXSpan() >> prevLod,
			sizeZ = area.getZSpan() >> prevLod;
		if (lod == 0) {
			for (int z = 0; z < sizeZ; z++) {
				for (int x = 0; x < padding; x++) {
					int fromIndex = posX.relativeColumnIndex(x, z);
					int toIndex = combined.relativeColumnIndex(combined.unpaddedVolume.maxX() + x, z);
					combined.columns[toIndex] = center.columns[fromIndex];
					combined.storage[toIndex] = center.storage[fromIndex];

					fromIndex = negX.relativeColumnIndex(x, z);
					toIndex = combined.relativeColumnIndex(x, z);
					combined.columns[toIndex] = center.columns[fromIndex];
					combined.storage[toIndex] = center.storage[fromIndex];
				}
			}
			for (int z = 0; z < padding; z++) {
				for (int x = 0; x < sizeX + (padding << 1); x++) {
					int fromIndex = posZ.relativeColumnIndex(x, z);
					int toIndex = combined.relativeColumnIndex(x, combined.unpaddedVolume.maxZ() + z);
					combined.columns[toIndex] = center.columns[fromIndex];
					combined.storage[toIndex] = center.storage[fromIndex];

					fromIndex = negZ.relativeColumnIndex(x, z);
					toIndex = combined.relativeColumnIndex(x, z);
					combined.columns[toIndex] = center.columns[fromIndex];
					combined.storage[toIndex] = center.storage[fromIndex];
				}
			}
		}
		else {
			QuadList quadList = new QuadList();
			for (int z = 0; z < sizeZ; z++) {
				for (int x = 0; x < padding; x++) {

				}
			}
		}
	}
}