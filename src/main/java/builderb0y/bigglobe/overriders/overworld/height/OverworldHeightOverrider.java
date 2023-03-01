package builderb0y.bigglobe.overriders.overworld.height;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.MathHelper;

import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.overriders.CachedStructures;
import builderb0y.bigglobe.overriders.overworld.OverworldOverrideContext;

public interface OverworldHeightOverrider {

	public abstract void override(Context context);

	public static class Context extends OverworldOverrideContext {

		public Context(
			BigGlobeOverworldChunkGenerator generator,
			OverworldColumn column,
			CachedStructures structures,
			OverridePhase phase
		) {
			super(generator, column, structures, phase);
		}

		public void sanityCheck() {
			assert this.column.finalHeight >= this.column.settings.height().min_y() && this.column.finalHeight <= this.column.settings.height().max_y();
			assert this.column. snowHeight >= this.column.settings.height().min_y() && this.column.finalHeight <= this.column.settings.height().max_y();
		}

		public void add(double y) {
			this.column.finalHeight += y;
			this.column.snowHeight += y;
			this.sanityCheck();
		}

		public void mixHeight(double y, double amount, SnowMixPolicy snow) {
			OverworldColumn column = this.column;
			double newHeight = Interpolator.mixLinear(column.finalHeight, y, amount);
			switch (snow) {
				case ADD -> {
					column.snowHeight += newHeight - column.finalHeight;
				}
				case MIX, SNOW_ONLY -> {
					column.snowHeight = Interpolator.mixLinear(column.snowHeight, y, amount);
				}
				case MIX_DOWN_ONLY -> {
					if (column.snowHeight > y) {
						column.snowHeight = Interpolator.mixLinear(column.snowHeight, y, amount);
					}
				}
				case SKIP_SNOW -> {}
			}
			if (snow != SnowMixPolicy.SNOW_ONLY) {
				column.finalHeight = newHeight;
			}
			this.sanityCheck();
		}

		public void mixHeightIncreaseOnly(double y, double amount, SnowMixPolicy snow) {
			if (this.column.finalHeight < y) {
				this.mixHeight(y, amount, snow);
			}
		}

		public void fitToBottomOfBox(BlockBox box, double offset, double maxDistance, SnowMixPolicy snow) {
			int clampedRelativeX = MathHelper.clamp(this.column.x, box.getMinX(), box.getMaxX()) - this.column.x;
			int clampedRelativeZ = MathHelper.clamp(this.column.z, box.getMinZ(), box.getMaxZ()) - this.column.z;
			int distanceSquared = BigGlobeMath.squareI(clampedRelativeX, clampedRelativeZ);
			if (distanceSquared < maxDistance * maxDistance) {
				this.mixHeight(box.getMinY() + offset, Interpolator.smooth(1.0D - Math.sqrt(distanceSquared) / maxDistance), snow);
			}
		}

		public static enum SnowMixPolicy {
			ADD,
			MIX,
			MIX_DOWN_ONLY,
			SNOW_ONLY,
			SKIP_SNOW;
		}
	}
}