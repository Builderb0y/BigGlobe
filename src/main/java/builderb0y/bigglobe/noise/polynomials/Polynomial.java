package builderb0y.bigglobe.noise.polynomials;

public interface Polynomial {

	public abstract void push(double next);

	public abstract double interpolate(double fraction);

	public abstract PolyForm form();

	public static interface PolyForm {

		public abstract double getMaxOvershoot();
	}
}