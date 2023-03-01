package builderb0y.bigglobe.noise;

public interface LayeredGrid extends Grid {

	public abstract Grid[] getLayers();

	public abstract double accumulate(double a, double b);

	public default void accumulate(double[] mainSamples, double[] scratch, int sampleCount) {
		for (int index = 0; index < sampleCount; index++) {
			mainSamples[index] = this.accumulate(mainSamples[index], scratch[index]);
		}
	}
}