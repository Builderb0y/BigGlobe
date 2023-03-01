package builderb0y.bigglobe.trees.branches;

@FunctionalInterface
public interface BranchHeight {

	public static final BranchHeight
		UP_UP     = fracLength -> fracLength,
		UP_FLAT   = fracLength -> fracLength * (2.0D - fracLength),
		UP_DOWN   = fracLength -> fracLength * (fracLength * -4.0D + 4.0D),

		FLAT_UP   = fracLength -> fracLength * fracLength,
		FLAT_FLAT = fracLength -> 0.0D,
		FLAT_DOWN = fracLength -> -fracLength * fracLength,

		DOWN_UP   = fracLength -> fracLength * (fracLength * 4.0D - 4.0D),
		DOWN_FLAT = fracLength -> fracLength * (fracLength - 2.0D),
		DOWN_DOWN = fracLength -> -fracLength;

	public abstract double getHeight(double fracLength);
}