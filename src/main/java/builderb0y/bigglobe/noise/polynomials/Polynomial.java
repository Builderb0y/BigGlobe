package builderb0y.bigglobe.noise.polynomials;

public interface Polynomial {

	public abstract void push(double next, double rcp);

	public abstract double interpolate(double fraction);

	public abstract PolyForm form();

	public static interface PolyForm {

		public abstract double calcMinValue(double min, double max, double rcp);

		public abstract double calcMaxValue(double min, double max, double rcp);
	}
}