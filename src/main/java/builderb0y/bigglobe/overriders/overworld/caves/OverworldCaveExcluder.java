package builderb0y.bigglobe.overriders.overworld.caves;

import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.OverworldColumn.CaveCell;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.overriders.CachedStructures;
import builderb0y.bigglobe.overriders.overworld.OverworldOverrideContext;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;

public interface OverworldCaveExcluder {

	public abstract void exclude(Context context);

	public static class Context extends OverworldOverrideContext {

		public CaveCell caveCell;
		public LocalOverworldCaveSettings caveSettings;
		public int topI, bottomI;
		public double topD, bottomD;
		public double ledgeMin;

		public Context(
			BigGlobeOverworldChunkGenerator generator,
			OverworldColumn column,
			CachedStructures structures,
			OverridePhase phase
		) {
			super(generator, column, structures, phase);
			this.caveCell     = column.getCaveCell();
			this.caveSettings = this.caveCell.settings;
			this.topD         = column.getFinalTopHeightD();
			this.topI         = column.getFinalTopHeightI();
			this.bottomD      = this.topD - this.caveSettings.depth();
			this.bottomI      = this.topI - this.caveSettings.depth();
			this.ledgeMin     = this.caveSettings.ledge_noise() != null ? this.caveSettings.ledge_noise().minValue() : 0.0D;
		}

		public double getExcludionMultiplier(int y) {
			double width = this.caveSettings.getWidthSquared(this.topD, y);
			width -= this.ledgeMin * width;
			return width;
		}

		public int yToIndex(int y) {
			return y - this.bottomI;
		}

		public void excludeUnchecked(int y, double exclusion) {
			this.column.caveNoise[this.yToIndex(y)] += exclusion * this.getExcludionMultiplier(y);
		}

		public void exclude(int y, double exclusion) {
			if (y >= this.bottomI && y < this.topI) {
				this.excludeUnchecked(y, exclusion);
			}
		}

		public void excludeSurface(double multiplier) {
			if (multiplier <= 0.0D) return;
			double baseY = this.topD;
			double width = this.caveSettings.upper_width();
			double intersection = baseY - width * 2.0D;
			multiplier /= width;
			int minY = Math.max(BigGlobeMath.ceilI(intersection), this.bottomI);
			int maxY = this.topI;
			for (int y = minY; y < maxY; y++) {
				this.excludeUnchecked(y, BigGlobeMath.squareD((y - intersection) * multiplier));
			}
		}
	}
}