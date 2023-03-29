package builderb0y.bigglobe.chunkgen.perSection;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.util.collection.PaletteStorage;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.OverworldColumn.CaveCell;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.OverworldCaveSettings.CaveSurfaceBlocks;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;

public class CaveSurfaceReplacer {

	public static void generate(SectionGenerationContext context) {
		long chunkSeed = context.chunkSeed(0x5075F6D2679E5234L);

		PaletteStorage storage = context.storage();
		int sectionStartY = context.startY();
		int sectionEndY = sectionStartY + 15;
		for (int columnIndex = 0; columnIndex < 256; columnIndex++) {
			OverworldColumn column = (OverworldColumn)(context.columns.getColumn(columnIndex));
			double rawDepth = column.getCaveSurfaceDepth();
			if (!(rawDepth > 0.0D)) continue;
			int depth = BigGlobeMath.floorI(Permuter.nextPositiveDouble(Permuter.permute(chunkSeed, columnIndex)) * rawDepth);

			CaveCell caveCell = column.getCaveCell();
			if (caveCell == null) return;
			LocalOverworldCaveSettings caveSettings = caveCell.settings;
			CaveSurfaceBlocks floorBlocks = caveSettings.floor_blocks();
			CaveSurfaceBlocks ceilingBlocks = caveSettings.ceiling_blocks();
			if (floorBlocks == null && ceilingBlocks == null) continue;

			int      floorSurfaceID =   floorBlocks != null ? context.id(  floorBlocks.surface   ()) : Integer.MIN_VALUE;
			int   floorSubsurfaceID =   floorBlocks != null ? context.id(  floorBlocks.subsurface()) : Integer.MIN_VALUE;
			int    ceilingSurfaceID = ceilingBlocks != null ? context.id(ceilingBlocks.surface   ()) : Integer.MIN_VALUE;
			int ceilingSubsurfaceID = ceilingBlocks != null ? context.id(ceilingBlocks.subsurface()) : Integer.MIN_VALUE;
			if (storage != (storage = context.storage())) { //resize
				floorSurfaceID      =   floorBlocks != null ? context.id(  floorBlocks.surface   ()) : Integer.MIN_VALUE;
				floorSubsurfaceID   =   floorBlocks != null ? context.id(  floorBlocks.subsurface()) : Integer.MIN_VALUE;
				ceilingSurfaceID    = ceilingBlocks != null ? context.id(ceilingBlocks.surface   ()) : Integer.MIN_VALUE;
				ceilingSubsurfaceID = ceilingBlocks != null ? context.id(ceilingBlocks.subsurface()) : Integer.MIN_VALUE;			}

			IntArrayList ceilings = column.caveCeilings;
			if (ceilings != null && ceilingBlocks != null) {
				int[] yLevels = ceilings.elements();
				int length = ceilings.size();
				int yIndex = Arrays.binarySearch(yLevels, 0, length, sectionEndY - 1);
				if (yIndex < 0) yIndex = ~yIndex;
				if (yIndex >= length) yIndex = length - 1;
				for (; yIndex >= 0; yIndex--) {
					int surfaceBottomY = yLevels[yIndex] + 1;
					int surfaceTopY = surfaceBottomY + depth;
					if (surfaceTopY >= sectionStartY) {
						surfaceTopY = Math.min(surfaceTopY, sectionEndY);
						surfaceBottomY = Math.max(surfaceBottomY, sectionStartY);
						for (int surfaceY = surfaceBottomY; surfaceY <= surfaceTopY; surfaceY++) {
							if (column.isCaveAt(surfaceY, true) || surfaceY >= column.getFinalTopHeightI()) break;
							storage.set(columnIndex | ((surfaceY & 15) << 8), surfaceY == surfaceBottomY ? ceilingSurfaceID : ceilingSubsurfaceID);
						}
					}
					else {
						break;
					}
				}
			}

			IntArrayList floors = column.caveFloors;
			if (floors != null && floorBlocks != null) {
				int[] yLevels = floors.elements();
				int length = floors.size();
				int yIndex = Arrays.binarySearch(yLevels, 0, length, sectionStartY + 1);
				if (yIndex < 0) yIndex = ~yIndex - 1;
				if (yIndex < 0) yIndex = 0;
				for (; yIndex < length; yIndex++) {
					int surfaceTopY = yLevels[yIndex] - 1;
					int surfaceBottomY = surfaceTopY - depth;
					if (surfaceBottomY <= sectionEndY) {
						surfaceTopY = Math.min(surfaceTopY, sectionEndY);
						surfaceBottomY = Math.max(surfaceBottomY, sectionStartY);
						for (int surfaceY = surfaceBottomY; surfaceY <= surfaceTopY; surfaceY++) {
							if (column.isCaveAt(surfaceY, true)) break;
							storage.set(columnIndex | ((surfaceY & 15) << 8), surfaceY == surfaceTopY ? floorSurfaceID : floorSubsurfaceID);
						}
					}
					else {
						break;
					}
				}
			}
		}
	}
}