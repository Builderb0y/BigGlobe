package builderb0y.bigglobe.noise;

public class SquareGrid2D extends SquareGrid implements UnaryGrid2D {

	public final Grid2D grid;

	public SquareGrid2D(Grid2D grid) {
		super(grid);
		this.grid = grid;
	}

	@Override
	public Grid2D getGrid() {
		return this.grid;
	}
}