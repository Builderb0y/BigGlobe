package builderb0y.bigglobe.noise;

public class SquareGrid1D extends SquareGrid implements UnaryGrid1D {

	public final Grid1D grid;

	public SquareGrid1D(Grid1D grid) {
		super(grid);
		this.grid = grid;
	}

	@Override
	public Grid1D getGrid() {
		return this.grid;
	}
}