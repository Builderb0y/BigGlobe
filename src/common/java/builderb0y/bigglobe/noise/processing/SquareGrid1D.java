package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid1D;

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