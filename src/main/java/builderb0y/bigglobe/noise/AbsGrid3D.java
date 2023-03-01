package builderb0y.bigglobe.noise;

public class AbsGrid3D extends AbsGrid implements UnaryGrid3D {

	public final Grid3D grid;

	public AbsGrid3D(Grid3D grid) {
		super(grid);
		this.grid = grid;
	}

	@Override
	public Grid3D getGrid() {
		return this.grid;
	}
}