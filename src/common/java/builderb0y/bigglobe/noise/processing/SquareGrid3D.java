package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid3D;

public class SquareGrid3D extends SquareGrid implements UnaryGrid3D {

	public final Grid3D grid;

	public SquareGrid3D(Grid3D grid) {
		super(grid);
		this.grid = grid;
	}

	@Override
	public Grid3D getGrid() {
		return this.grid;
	}
}