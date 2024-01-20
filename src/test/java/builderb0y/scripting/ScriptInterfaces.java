package builderb0y.scripting;

public class ScriptInterfaces {

	//////////////// int ////////////////

	@FunctionalInterface
	public static interface IntSupplier {

		public int getAsInt();
	}

	@FunctionalInterface
	public static interface IntUnaryOperator {

		public int applyAsInt(int x);
	}

	@FunctionalInterface
	public static interface IntBinaryOperator {

		public int applyAsInt(int x, int y);
	}

	//////////////// long ////////////////

	@FunctionalInterface
	public static interface LongSupplier {

		public long getAsLong();
	}

	@FunctionalInterface
	public static interface LongUnaryOperator {

		public long applyAsLong(long x);
	}

	@FunctionalInterface
	public static interface LongBinaryOperator {

		public long applyAsLong(long x, long y);
	}

	//////////////// float ////////////////

	@FunctionalInterface
	public static interface FloatSupplier {

		public abstract float getAsFloat();
	}

	@FunctionalInterface
	public static interface FloatUnaryOperator {

		public abstract float applyAsFloat(float x);
	}

	@FunctionalInterface
	public static interface FloatBinaryOperator {

		public abstract float applyAsFloat(float x, float y);
	}

	//////////////// double ////////////////

	@FunctionalInterface
	public static interface DoubleSupplier {

		public abstract double getAsDouble();
	}

	@FunctionalInterface
	public static interface DoubleUnaryOperator {

		public abstract double applyAsDouble(double x);
	}

	@FunctionalInterface
	public static interface DoubleBinaryOperator {

		public abstract double applyAsDouble(double x, double y);
	}

	//////////////// boolean ////////////////

	@FunctionalInterface
	public static interface BooleanSupplier {

		public abstract boolean getAsBoolean();
	}

	@FunctionalInterface
	public static interface BooleanUnaryOperator {

		public abstract boolean applyAsBoolean(boolean x);
	}

	@FunctionalInterface
	public static interface BooleanBinaryOperator {

		public abstract boolean applyAsBoolean(boolean x, boolean y);
	}

	//////////////// Object ////////////////

	@FunctionalInterface
	public static interface ObjectSupplier {

		public abstract Object getAsObject();
	}

	@FunctionalInterface
	public static interface ObjectUnaryOperator {

		public abstract Object applyAsObject(Object x);
	}

	@FunctionalInterface
	public static interface ObjectBinaryOperator {

		public abstract Object applyAsObject(Object x, Object y);
	}

	//////////////// other ////////////////

	@FunctionalInterface
	public static interface LongIntToLongOperator {

		public abstract long applyAsLong(long x, int y);
	}

	@FunctionalInterface
	public static interface IntToLongOperator {

		public abstract long applyAsLong(int x);
	}

	@FunctionalInterface
	public static interface IntToFloatOperator {

		public abstract float applyAsFloat(int x);
	}

	@FunctionalInterface
	public static interface IntToDoubleOperator {

		public abstract double applyAsDouble(int x);
	}

	@FunctionalInterface
	public static interface FloatIntToFloatOperator {

		public abstract float applyAsFloat(float x, int y);
	}

	@FunctionalInterface
	public static interface DoubleIntToDoubleOperator {

		public abstract double applyAsDouble(double x, int y);
	}

	@FunctionalInterface
	public static interface LongToIntOperator {

		public abstract int applyAsInt(long x);
	}
}