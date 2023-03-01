package builderb0y.bigglobe.noise;

public abstract class CubicGrid implements Grid {

	public final long salt;
	public final double amplitude;

	public CubicGrid(long salt, double amplitude) {
		this.salt = salt;
		this.amplitude = amplitude;
	}
}