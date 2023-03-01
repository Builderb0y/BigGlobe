package builderb0y.bigglobe.noise;

public class ChangeRangeGrid3D extends ChangeRangeGrid implements UnaryGrid3D {

	public final Grid3D grid;

	public ChangeRangeGrid3D(Grid3D grid, double min, double max) {
		super(grid, min, max);
		this.grid = grid;
	}

	@Override
	public Grid3D getGrid() {
		return this.grid;
	}
}