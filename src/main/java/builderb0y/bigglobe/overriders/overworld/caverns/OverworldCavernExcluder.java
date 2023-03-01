package builderb0y.bigglobe.overriders.overworld.caverns;

import net.minecraft.util.math.MathHelper;

import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.OverworldColumn.CavernCell;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.overriders.CachedStructures;
import builderb0y.bigglobe.overriders.overworld.OverworldOverrideContext;
import builderb0y.bigglobe.settings.OverworldCavernSettings.LocalCavernSettings;

public interface OverworldCavernExcluder {

	public abstract void exclude(Context context);

	public static class Context extends OverworldOverrideContext {

		public CavernCell cavernCell;
		public LocalCavernSettings cavernSettings;

		public Context(
			BigGlobeOverworldChunkGenerator generator,
			OverworldColumn column,
			CachedStructures structures,
			OverridePhase phase
		) {
			super(generator, column, structures, phase);
			this.cavernCell = column.getCavernCell();
			this.cavernSettings = this.cavernCell.settings;
		}

		public double getExclusionFactor(double spacing, double minY, double maxY) {
			double averageCenter = this.cavernCell.averageCenter;
			double sqrtMaxThickness = this.cavernSettings.sqrtMaxThickness();
			double lowerOverlap = Interpolator.unmixLinear(maxY + spacing, maxY, averageCenter - sqrtMaxThickness);
			double upperOverlap = Interpolator.unmixLinear(minY - spacing, minY, averageCenter + sqrtMaxThickness);
			return MathHelper.clamp(Math.min(lowerOverlap, upperOverlap), 0.0D, 1.0D);
		}

		public void exclude(double fraction) {
			this.column.cavernThicknessSquared -= fraction * this.cavernSettings.thickness().maxValue();
		}
	}
}