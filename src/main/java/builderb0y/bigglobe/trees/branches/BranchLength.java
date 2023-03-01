package builderb0y.bigglobe.trees.branches;

@FunctionalInterface
public interface BranchLength {

	public static final BranchLength CONE         = fracY -> 1.0D - fracY;
	public static final BranchLength ROUND        = fracY -> fracY * (fracY * -4.0D + 4.0D);
	public static final BranchLength ROUND_TOP    = fracY -> {
		double arch = fracY * (fracY * -4.0D + 4.0D);
		return fracY > 0.5D ? Math.sqrt(arch) : arch;
	};
	public static final BranchLength CANOPY       = fracY -> fracY * fracY;
	public static final BranchLength SEMI_ROUND   = fracY -> Math.sqrt(1.0D - fracY * fracY);

	public abstract double getLength(double fracY);
}