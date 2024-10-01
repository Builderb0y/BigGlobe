package builderb0y.bigglobe.math;

@SuppressWarnings({ "unused", "RedundantCast", "OverloadedMethodsWithSameNumberOfParameters" })
public class BigGlobeMath {

	public static final double
		TAU    = Math.PI * 2.0D,
		LN_2   = Math.log(2.0D),
		LOG2_E = 1.0D / LN_2;

	//////////////////////////////// square1 ////////////////////////////////

	public static int squareI(int i) {
		return i * i;
	}

	public static long squareL(long l) {
		return l * l;
	}

	public static long squareL(int i) {
		return squareL((long)(i));
	}

	public static float squareF(float f) {
		return f * f;
	}

	public static float squareF(int i) {
		return squareF((float)(i));
	}

	public static double squareD(double d) {
		return d * d;
	}

	public static double squareD(int i) {
		return squareD((double)(i));
	}

	public static double squareD(long l) {
		return squareD((double)(l));
	}

	public static double squareD(float f) {
		return squareD((double)(f));
	}

	//////////////////////////////// square2 ////////////////////////////////

	public static int squareI(int i1, int i2) {
		return i1 * i1 + i2 * i2;
	}

	public static long squareL(long l1, long l2) {
		return l1 * l1 + l2 * l2;
	}

	public static long squareL(int i1, int i2) {
		return squareL((long)(i1), (long)(i2));
	}

	public static float squareF(float f1, float f2) {
		return f1 * f1 + f2 * f2;
	}

	public static float squareF(int i1, int i2) {
		return squareF((float)(i1), (float)(i2));
	}

	public static double squareD(double d1, double d2) {
		return d1 * d1 + d2 * d2;
	}

	public static double squareD(int i1, int i2) {
		return squareD((double)(i1), (double)(i2));
	}

	public static double squareD(long l1, long l2) {
		return squareD((double)(l1), (double)(l2));
	}

	public static double squareD(float f1, float f2) {
		return squareD((double)(f1), (double)(f2));
	}

	//////////////////////////////// square3 ////////////////////////////////

	public static int squareI(int i1, int i2, int i3) {
		return i1 * i1 + i2 * i2 + i3 * i3;
	}

	public static long squareL(long l1, long l2, long l3) {
		return l1 * l1 + l2 * l2 + l3 * l3;
	}

	public static long squareL(int i1, int i2, int i3) {
		return squareL((long)(i1), (long)(i2), (long)(i3));
	}

	public static float squareF(float f1, float f2, float f3) {
		return f1 * f1 + f2 * f2 + f3 * f3;
	}

	public static float squareF(int i1, int i2, int i3) {
		return squareF((float)(i1), (float)(i2), (float)(i3));
	}

	public static double squareD(double d1, double d2, double d3) {
		return d1 * d1 + d2 * d2 + d3 * d3;
	}

	public static double squareD(int i1, int i2, int i3) {
		return squareD((double)(i1), (double)(i2), (double)(i3));
	}

	public static double squareD(long l1, long l2, long l3) {
		return squareD((double)(l1), (double)(l2), (double)(l3));
	}

	public static double squareD(float f1, float f2, float f3) {
		return squareD((double)(f1), (double)(f2), (double)(f3));
	}

	//////////////////////////////// floor, ceil, round ////////////////////////////////

	public static int floorI(float number) {
		int floor = (int)(number);
		if (floor > number) floor--;
		return floor;
	}

	public static int floorI(double number) {
		int floor = (int)(number);
		if (floor > number) floor--;
		return floor;
	}

	public static long floorL(float number) {
		long floor = (long)(number);
		if (floor > number) floor--;
		return floor;
	}

	public static long floorL(double number) {
		long floor = (long)(number);
		if (floor > number) floor--;
		return floor;
	}

	public static int ceilI(float number) {
		int ceil = (int)(number);
		if (ceil < number) ceil++;
		return ceil;
	}

	public static int ceilI(double number) {
		int ceil = (int)(number);
		if (ceil < number) ceil++;
		return ceil;
	}

	public static long ceilL(float number) {
		long ceil = (long)(number);
		if (ceil < number) ceil++;
		return ceil;
	}

	public static long ceilL(double number) {
		long ceil = (long)(number);
		if (ceil < number) ceil++;
		return ceil;
	}

	public static int roundI(float number) {
		return floorI(number + 0.5F);
	}

	public static int roundI(double number) {
		return floorI(number + 0.5D);
	}

	public static long roundL(float number) {
		return floorL(number + 0.5F);
	}

	public static long roundL(double number) {
		return floorL(number + 0.5D);
	}

	//////////////////////////////// modulus ////////////////////////////////

	//in java, the default % operator is fucking useless when a < 0 || b <= 0.
	//these methods are how you do modulus operations PROPERLY.
	//these implementations make the following guarantees:
	//	if (b > 0), then (a mod b) >= 0.
	//	if (b < 0), then (a mod b) <= 0.
	//	if (b == 0), then (a mod b) == 0.
	//		for floats and doubles, if (b == 0), then (a mod b) = 0 with the same sign as b.
	//	for floats and doubles, if (b is NaN), then (a mod b) = b.
	//	for floats and doubles, the sign bit of (a mod b) equals the sign bit of b.

	/** returns (a mod b) when nothing is known in advance about the signs of a or b. */
	public static int modulus(int a, int b) {
		if      (b > 0) return modulus_BP(a, b);
		else if (b < 0) return modulus_BN(a, b);
		else            return 0; //lim[b -> 0] (a mod b) = 0 for all values of a.
	}

	/**
	returns (a mod b) when b is known in advance to be positive (> 0).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static int modulus_BP(int a, int b) {
		return (a %= b) < 0 ? a + b : a;
	}

	/**
	returns (a mod b) when b is known in advance to be negative (< 0).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static int modulus_BN(int a, int b) {
		return (a %= -b) > 0 ? a + b : a;
	}

	/** returns (a mod b) when nothing is known in advance about the signs of a or b. */
	public static long modulus(long a, long b) {
		if      (b > 0L) return modulus_BP(a, b);
		else if (b < 0L) return modulus_BN(a, b);
		else             return 0L; //lim[b -> 0] (a mod b) = 0 for all values of a.
	}

	/**
	returns (a mod b) when b is known in advance to be positive (> 0L).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static long modulus_BP(long a, long b) {
		return (a %= b) < 0L ? a + b : a;
	}

	/**
	returns (a mod b) when b is known in advance to be negative (< 0L).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static long modulus_BN(long a, long b) {
		return (a %= -b) > 0L ? a + b : a;
	}

	/** returns (a mod b) when nothing is known in advance about the signs of a or b. */
	public static float modulus(float a, float b) {
		if      (b > 0.0F) return modulus_BP(a, b);
		else if (b < 0.0F) return modulus_BN(a, b);
		else               return b; //+0.0, -0.0, and NaN.
	}

	/**
	returns (a mod b) when b is known in advance to be positive (> 0.0F).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static float modulus_BP(float a, float b) {
		return (a %= b) + (a < 0.0F ? b : 0.0F); //adding 0.0 will convert -0.0 to +0.0.
	}

	/**
	returns (a mod b) when b is known in advance to be negative (< 0.0F).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static float modulus_BN(float a, float b) {
		float mod = a % -b;
		//b is negative, so this acts as subtraction, not addition.
		if (mod > 0.0F) mod += b;
		//convert +0.0F to -0.0F.
		mod = Float.intBitsToFloat(Float.floatToRawIntBits(mod) | 0x8000_0000);
		return mod;
	}

	/** returns (a mod b) when nothing is known in advance about the signs of a or b. */
	public static double modulus(double a, double b) {
		if      (b > 0.0D) return modulus_BP(a, b);
		else if (b < 0.0D) return modulus_BN(a, b);
		else               return b; //+0.0, -0.0, and NaN.
	}

	/**
	returns (a mod b) when b is known in advance to be positive (> 0.0D).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static double modulus_BP(double a, double b) {
		return (a %= b) + (a < 0.0D ? b : 0.0D); //adding 0.0 will convert -0.0 to +0.0.
	}

	/**
	returns (a mod b) when b is known in advance to be negative (< 0.0D).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static double modulus_BN(double a, double b) {
		double mod = a % -b;
		//b is negative, so this acts as subtraction, not addition.
		if (mod > 0.0D) mod += b;
		//convert +0.0D to -0.0.
		mod = Double.longBitsToDouble(Double.doubleToRawLongBits(mod) | 0x8000_0000_0000_0000L);
		return mod;
	}

	//////////////////////////////// exponentials ////////////////////////////////

	/** returns 2 raised to the power of value. */
	public static float exp2(float value) {
		//return FastExp.compute2(value);
		return (float)(Math.exp(value * LN_2));
	}

	/** returns 2 raised to the power of value. */
	public static double exp2(double value) {
		//return FastExp.compute2(value);
		return Math.exp(value * LN_2);
	}

	/**
	returns {@link Math#E} raised to the power of value.
	@see Math#exp(double)
	*/
	public static float exp(float value) {
		//return FastExp.computeE(value);
		return (float)(Math.exp(value));
	}

	/**
	returns {@link Math#E} raised to the power of value.
	@see Math#exp(double)
	*/
	public static double exp(double value) {
		//return FastExp.computeE(value);
		return Math.exp(value);
	}

	/** returns the logarithm of value in base 2. */
	public static float log2(float value) {
		//return FastLog.compute2(value);
		return (float)(Math.log(value) / LN_2);
	}

	/** returns the logarithm of value in base 2. */
	public static double log2(double value) {
		//return FastLog.compute2(value);
		return Math.log(value) / LN_2;
	}

	/**
	returns the natural logarithm (base {@link Math#E}) of value.
	@see Math#log(double)
	*/
	public static float ln(float value) {
		//return FastLog.computeE(value);
		return (float)(Math.log(value));
	}

	/**
	returns the natural logarithm (base {@link Math#E}) of value.
	@see Math#log(double)
	*/
	public static double ln(double value) {
		//return FastLog.computeE(value);
		return Math.log(value);
	}

	//////////////////////////////// miscellaneous ////////////////////////////////

	public static float sigmoid01(float value) {
		return 1.0F / (exp(-4.0F * value) + 1.0F);
	}

	public static double sigmoid01(double value) {
		return 1.0D / (exp(-4.0D * value) + 1.0D);
	}

	public static float sigmoidM11(float value) {
		return (float)(Math.tanh(value));
		//return 2.0F / (exp(-2.0F * value) + 1.0F) - 1.0F;
	}

	public static double sigmoidM11(double value) {
		return Math.tanh(value);
		//return 2.0D / (exp(-2.0D * value) + 1.0D) - 1.0D;
	}

	/** local maximum at value = 0 */
	public static float fogify(float width, float value) {
		return width / (squareF(value) + width);
	}

	/** local maximum at x = y = 0 */
	public static float fogify(float width, float x, float y) {
		return width / (squareF(x, y) + width);
	}

	/** local maximum at x = y = z = 0 */
	public static float fogify(float width, float x, float y, float z) {
		return width / (squareF(x, y, z) + width);
	}

	/** local minimum at value = 0 */
	public static float upsideDownFogify(float width, float value) {
		value *= value;
		return value / (value + width);
	}

	/** local minimum at x = y = 0 */
	public static float upsideDownFogify(float width, float x, float y) {
		float value = squareF(x, y);
		return value / (value + width);
	}

	/** local minimum at x = y = z = 0 */
	public static float upsideDownFogify(float width, float x, float y, float z) {
		float value = squareF(x, y, z);
		return value / (value + width);
	}

	public static float bellCurve(float width, float value) {
		return exp(-squareF(value) / width);
	}

	public static float bellCurve(float width, float x, float y) {
		return exp(-squareF(x, y) / width);
	}

	public static float bellCurve(float width, float x, float y, float z) {
		return exp(-squareF(x, y, z) / width);
	}

	public static byte toByteExact(int value) {
		byte b = (byte)(value);
		if (value == b) return b;
		else throw new ArithmeticException("Value too big for a byte: " + value);
	}

	public static short toShortExact(int value) {
		short s = (short)(value);
		if (value == s) return s;
		else throw new ArithmeticException("Value too big for a short: " + value);
	}

	public static byte toUnsignedByteExact(int value) {
		if (value == (value & 0xFF)) return (byte)(value);
		else throw new ArithmeticException("Value too big for an unsigned byte: " + value);
	}

	public static short toUnsignedShortExact(int value) {
		if (value == (value & 0xFFFF)) return (short)(value);
		else throw new ArithmeticException("Value too big for an unsigned short: " + value);
	}

	public static int toUnsignedIntExact(long value) {
		if (value == (value & 0xFFFFFFFFL)) return (int)(value);
		else throw new ArithmeticException("Value too big for an unsigned int: " + value);
	}

	public static char toCharExact(int value) {
		if (value == (char)(value)) return (char)(value);
		else throw new ArithmeticException("Value too big for a char: " + value);
	}

	public static int positiveProduct(int a, int b) {
		return a > 0 && b > 0 ? a * b : 0;
	}

	public static long positiveProduct(long a, long b) {
		return a > 0L && b > 0L ? a * b : 0L;
	}

	public static float positiveProduct(float a, float b) {
		return a > 0.0F && b > 0.0F ? a * b : 0.0F;
	}

	public static double positiveProduct(double a, double b) {
		return a > 0.0D && b > 0.0D ? a * b : 0.0D;
	}
}