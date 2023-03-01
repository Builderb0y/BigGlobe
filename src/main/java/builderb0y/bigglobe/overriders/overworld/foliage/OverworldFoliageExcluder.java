package builderb0y.bigglobe.overriders.overworld.foliage;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.MathHelper;

import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.overriders.CachedStructures;
import builderb0y.bigglobe.overriders.overworld.OverworldOverrideContext;

public interface OverworldFoliageExcluder {

	public abstract void exclude(Context context);

	public static class Context extends OverworldOverrideContext {

		public Context(
			BigGlobeOverworldChunkGenerator generator,
			OverworldColumn column,
			CachedStructures structures,
			OverridePhase phase
		) {
			super(generator, column, structures, phase);
		}

		public void exclude(double amount) {
			this.column.foliage *= 1.0D - amount;
		}

		public void excludeBox(BlockBox box, int maxDistance) {
			int clampX = MathHelper.clamp(this.column.x, box.getMinX(), box.getMaxX());
			int clampZ = MathHelper.clamp(this.column.z, box.getMinZ(), box.getMaxZ());
			double offsetX = 1.0D - ((double)(Math.abs(this.column.x - clampX))) / ((double)(maxDistance));
			double offsetZ = 1.0D - ((double)(Math.abs(this.column.z - clampZ))) / ((double)(maxDistance));
			if (offsetX > 0.0D && offsetZ > 0.0D) {
				this.exclude(Interpolator.smooth(offsetX) * Interpolator.smooth(offsetZ));
			}
		}
	}
}