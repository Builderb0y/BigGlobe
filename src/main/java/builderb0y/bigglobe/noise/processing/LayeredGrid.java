package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid;
import builderb0y.bigglobe.noise.NumberArray;

public interface LayeredGrid extends Grid {

	public abstract Grid[] getLayers();

	/**
	I wanted to have an accumulate(double, double) method here which
	would return a sum, product, or any other associative/commutative
	operation, but the polymorphic dispatch to said method was enough
	to show up in my profiling data. so I'm replacing it with a simple
	boolean. where possible, this boolean is checked outside of any loops.
	*/
	public abstract boolean isProduct();

	public default void accumulate(NumberArray mainSamples, NumberArray scratch) {
		if (this.isProduct()) {
			for (int index = 0, length = mainSamples.length(); index < length; index++) {
				mainSamples.mul(index, scratch.getD(index));
			}
		}
		else {
			for (int index = 0, length = mainSamples.length(); index < length; index++) {
				mainSamples.add(index, scratch.getD(index));
			}
		}
	}
}