package builderb0y.bigglobe.noise;

public class NegateGrid2D extends NegateGrid implements UnaryGrid2D {

	public final Grid2D grid;

	public NegateGrid2D(Grid2D grid) {
		this.grid = grid;
	}

	@Override
	public Grid2D getGrid() {
		return this.grid;
	}
}