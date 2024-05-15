package builderb0y.bigglobe.noise.processing;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.bigglobe.settings.Seed;

public abstract class BinaryGrid extends AbstractGrid {

	public final @VerifyFloatRange(min = 0.0D, minInclusive = false, max = 1.0D, maxInclusive = false) @DefaultDouble(0.5D) double chance;

	public BinaryGrid(Seed salt, double amplitude, double chance) {
		super(salt, amplitude);
		this.chance = chance;
	}
}