package builderb0y.bigglobe.noise;

public interface UnaryGrid extends Grid {

	public abstract Grid getGrid();

	public abstract double operate(double value);

	public default void operate(double[] samples, int sampleCount) {
		for (int index = 0; index < sampleCount; index++) {
			samples[index] = this.operate(samples[index]);
		}
	}
}