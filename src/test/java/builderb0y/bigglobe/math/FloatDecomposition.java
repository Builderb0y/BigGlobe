package builderb0y.bigglobe.math;

public class FloatDecomposition {

	public final float value;
	public final int bits;
	public final int sign, exponent, mantissa;
	public final int signMask, exponentMask;
	public final float mantissaValue;
	public final int exponentValue;

	public FloatDecomposition(float value) {
		this.value = value;
		this.bits = Float.floatToRawIntBits(value);
		this.signMask = this.bits & 0x80000000;
		this.sign = this.signMask >>> 31;
		this.exponentMask = this.bits & 0x7F800000;
		this.exponent = this.exponentMask >>> 23;
		this.exponentValue = this.exponent - 127;
		this.mantissa = this.bits & 0x007FFFFF;
		this.mantissaValue = ((float)(this.mantissa | 0x00800000)) / ((float)(0x00800000));
	}
}