package builderb0y.bigglobe.noise;

public class NegateGrid3D extends NegateGrid implements UnaryGrid3D {

	public final Grid3D grid;

	public NegateGrid3D(Grid3D grid) {
		this.grid = grid;
	}

	@Override
	public Grid3D getGrid() {
		return this.grid;
	}
}