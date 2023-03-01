package builderb0y.bigglobe.noise;

public class ChangeRangeGrid2D extends ChangeRangeGrid implements UnaryGrid2D {

	public final Grid2D grid;

	public ChangeRangeGrid2D(Grid2D grid, double min, double max) {
		super(grid, min, max);
		this.grid = grid;
	}

	@Override
	public Grid2D getGrid() {
		return this.grid;
	}
}