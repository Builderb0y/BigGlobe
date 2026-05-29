package builderb0y.bigglobe.rendering.lods.flat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.rendering.lods.ColumnBlockGetter;
import builderb0y.bigglobe.rendering.lods.GenerationPipeline;
import builderb0y.bigglobe.rendering.lods.LodGenerator;
import builderb0y.bigglobe.rendering.lods.LodGenerator.DownscaleSettings;
import builderb0y.bigglobe.rendering.lods.LodGenerator.LoadMode;
import builderb0y.bigglobe.rendering.lods.LodMesher;

@Environment(EnvType.CLIENT)
public class FlatGenerationPipeline extends GenerationPipeline {

	public FlatGenerationPipeline(FlatLodSystem system, LodGenerator<?> generator, LodMesher mesher) {
		super(system, generator, mesher);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public @Nullable ColumnBlockGetter generateWithPadding(BoundingBox area, byte lod, LoadMode mode) {
		final int rawPadding = 2;
		int padding = rawPadding << lod;
		byte prevLod = (byte)(Math.max(lod - 1, 0));
		LodGenerator generator = this.generator;
		Object loadCache = (
			mode.canLoad() && lod < generator.maxLoadLevel
			? generator.preload(new BoundingBox(area.minX() - padding, area.minY(), area.minZ() - padding, area.maxX() + padding, area.maxY(), area.maxZ() + padding))
			: null
		);
		//"center translate". name kept short for readability below.
		BoundingBox ct = new BoundingBox(0, area.minY(), 0, (area.getXSpan() >> lod) - 1, area.maxY(), (area.getZSpan() >> lod) - 1);
		ColumnBlockGetter center = generator.generateRegion(
			area,
			ct,
			lod,
			mode,
			DownscaleSettings.NONE,
			loadCache
		);
		if (center == null) return null;
		mode = mode.allowGeneration();
		//         posZ
		//------+--------+------
		// negX | center | posX
		//------+--------+------
		//         negZ
		DownscaleSettings downscaleSettings = DownscaleSettings.NONE.deltaLod(lod > 0 ? 1 : 0).mergeHorizontally(lod > 0).keepAir(true);
		ColumnBlockGetter
			posX = generator.generateRegion(
				new BoundingBox(area.maxX() + 1, area.minY(), area.minZ(), area.maxX() + padding, area.maxY(), area.maxZ()),
				new BoundingBox(ct.maxX() + 1, ct.minY(), ct.minZ(), ct.maxX() + rawPadding, ct.maxY(), ct.maxZ()),
				prevLod,
				mode,
				downscaleSettings,
				loadCache
			),
			negX = generator.generateRegion(
				new BoundingBox(area.minX() - padding, area.minY(), area.minZ(), area.minX() - 1, area.maxY(), area.maxZ()),
				new BoundingBox(ct.minX() - rawPadding, ct.minY(), ct.minZ(), ct.minX() - 1, ct.maxY(), ct.maxZ()),
				prevLod,
				mode,
				downscaleSettings,
				loadCache
			),
			posZ = generator.generateRegion(
				new BoundingBox(area.minX() - padding, area.minY(), area.maxZ() + 1, area.maxX() + padding, area.maxY(), area.maxZ() + padding),
				new BoundingBox(ct.minX() - rawPadding, ct.minY(), ct.maxZ() + 1, ct.maxX() + rawPadding, ct.maxY(), ct.maxZ() + rawPadding),
				prevLod,
				mode,
				downscaleSettings,
				loadCache
			),
			negZ = generator.generateRegion(
				new BoundingBox(area.minX() - padding, area.minY(), area.minZ() - padding, area.maxX() + padding, area.maxY(), area.minZ() - 1),
				new BoundingBox(ct.minX() - rawPadding, ct.minY(), ct.minZ() - rawPadding, ct.maxX() + rawPadding, ct.maxY(), ct.minZ() - 1),
				prevLod,
				mode,
				downscaleSettings,
				loadCache
			);
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
			center.cleanup
		);
		for (int z = combined.paddedVolume.minZ(); z <= combined.paddedVolume.maxZ(); z++) {
			for (int x = combined.paddedVolume.minX(); x <= combined.paddedVolume.maxX(); x++) {
				ColumnBlockGetter region;
				if (z < combined.unpaddedVolume.minZ()) {
					region = negZ;
				}
				else if (z > combined.unpaddedVolume.maxZ()) {
					region = posZ;
				}
				else if (x < combined.unpaddedVolume.minX()) {
					region = negX;
				}
				else if (x > combined.unpaddedVolume.maxX()) {
					region = posX;
				}
				else {
					region = center;
				}
				int fromIndex = region.columnIndex(x, z);
				int toIndex = combined.columnIndex(x, z);
				combined.columns[toIndex] = region.columns[fromIndex];
				combined.storage[toIndex] = region.storage[fromIndex];
			}
		}
		return combined;
	}
}