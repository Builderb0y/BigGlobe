package builderb0y.bigglobe.util;

import java.util.function.DoubleUnaryOperator;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.math.BigGlobeMath;

public class Derivative3D implements Cloneable {

	public double value, dx, dy, dz;

	public Derivative3D() {}

	public Derivative3D(double value) {
		this.value = value;
	}

	public Derivative3D(double value, double dx, double dy, double dz) {
		this.value = value;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
	}

	public Derivative3D(Derivative3D that) {
		this.value = that.value;
		this.dx = that.dx;
		this.dy = that.dy;
		this.dz = that.dz;
	}

	//////////////////////////////// set ////////////////////////////////

	public Derivative3D set(double value) {
		return this.set(value, 0.0D, 0.0D, 0.0D);
	}

	public Derivative3D set(double value, double dx, double dy, double dz) {
		this.value = value;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		return this;
	}

	public Derivative3D set(Derivative3D that) {
		return this.set(that.value, that.dx, that.dy, that.dz);
	}

	//////////////////////////////// add ////////////////////////////////

	public Derivative3D add(double value) {
		this.value += value;
		return this;
	}

	public Derivative3D add(double value, double dx, double dy, double dz) {
		return this.set(this.value + value, this.dx + dx, this.dy + dy, this.dz + dz);
	}

	public Derivative3D add(Derivative3D that) {
		return this.add(that.value, that.dx, that.dy, that.dz);
	}

	//////////////////////////////// sub ////////////////////////////////

	public Derivative3D sub(double value) {
		this.value -= value;
		return this;
	}

	public Derivative3D sub(double value, double dx, double dy, double dz) {
		return this.set(this.value - value, this.dx - dx, this.dy - dy, this.dz - dz);
	}

	public Derivative3D sub(Derivative3D that) {
		return this.sub(that.value, that.dx, that.dy, that.dz);
	}

	//////////////////////////////// subRev ////////////////////////////////

	public Derivative3D subRev(double value) {
		return this.set(value - this.value, -this.dx, -this.dy, -this.dz);
	}

	public Derivative3D subRev(double value, double dx, double dy, double dz) {
		return this.set(value - this.value, dx - this.dx, dy - this.dy, dz - this.dz);
	}

	public Derivative3D subRev(Derivative3D that) {
		return this.subRev(that.value, that.dx, that.dy, that.dz);
	}

	//////////////////////////////// mul ////////////////////////////////

	public Derivative3D mul(double value) {
		return this.set(this.value * value, this.dx * value, this.dy * value, this.dz * value);
	}

	public Derivative3D mul(double value, double dx, double dy, double dz) {
		return this.set(
			this.value * value,
			this.value * dx + this.dx * value,
			this.value * dy + this.dy * value,
			this.value * dz + this.dz * value
		);
	}

	public Derivative3D mul(Derivative3D that) {
		return this.mul(that.value, that.dx, that.dy, that.dz);
	}

	//////////////////////////////// div ////////////////////////////////

	public Derivative3D div(double value) {
		return this.set(this.value / value, this.dx / value, this.dy / value, this.dz / value);
	}

	public Derivative3D div(double value, double dx, double dy, double dz) {
		double valueSquared = value * value;
		return this.set(
			this.value / value,
			(value * this.dx - dx * this.value) / valueSquared,
			(value * this.dy - dy * this.value) / valueSquared,
			(value * this.dz - dz * this.value) / valueSquared
		);
	}

	public Derivative3D div(Derivative3D that) {
		return this.div(that.value, that.dx, that.dy, that.dz);
	}

	//////////////////////////////// divRev ////////////////////////////////

	public Derivative3D divRev(double value) {
		double minusValueSquared = -BigGlobeMath.squareD(this.value);
		return this.set(
			value / this.value,
			(this.dx * value) / minusValueSquared,
			(this.dy * value) / minusValueSquared,
			(this.dz * value) / minusValueSquared
		);
	}

	public Derivative3D divRev(double value, double dx, double dy, double dz) {
		double valueSquared = this.value * this.value;
		return this.set(
			value / this.value,
			(this.value * dx - this.dx * value) / valueSquared,
			(this.value * dy - this.dy * value) / valueSquared,
			(this.value * dz - this.dz * value) / valueSquared
		);
	}

	public Derivative3D divRev(Derivative3D that) {
		return this.divRev(that.value, that.dx, that.dy, that.dz);
	}

	//////////////////////////////// pos ////////////////////////////////

	public Derivative3D pow(double exponent) {
		double chain = exponent * Math.pow(this.value, exponent - 1.0D);
		return this.set(
			Math.pow(this.value, exponent),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	public Derivative3D pow(double value, double dx, double dy, double dz) {
		double pow = Math.pow(this.value, value);
		double common = value / this.value;
		double logValue = Math.log(this.value);
		return this.set(
			pow,
			pow * (common * this.dx + logValue * dx),
			pow * (common * this.dy + logValue * dy),
			pow * (common * this.dz + logValue * dz)
		);
	}

	public Derivative3D pow(Derivative3D that) {
		return this.pow(that.value, that.dx, that.dy, that.dz);
	}

	//////////////////////////////// unary ////////////////////////////////

	public Derivative3D negate() {
		return this.set(-this.value, -this.dx, -this.dy, -this.dz);
	}

	//////////////////////////////// simple powers ////////////////////////////////

	public Derivative3D rcp() {
		double minusValueSquared = -BigGlobeMath.squareD(this.value);
		return this.set(
			1.0D / this.value,
			this.dx / minusValueSquared,
			this.dy / minusValueSquared,
			this.dz / minusValueSquared
		);
	}

	public Derivative3D square() {
		double twoValue = this.value * 2.0D;
		return this.set(
			BigGlobeMath.squareD(this.value),
			twoValue * this.dx,
			twoValue * this.dy,
			twoValue * this.dz
		);
	}

	public Derivative3D sqrt() {
		double sqrtValue = Math.sqrt(this.value);
		double halfOverSqrtValue = 0.5D / sqrtValue;
		return this.set(
			sqrtValue,
			halfOverSqrtValue * this.dx,
			halfOverSqrtValue * this.dy,
			halfOverSqrtValue * this.dz
		);
	}

	//////////////////////////////// exponentials ////////////////////////////////

	public Derivative3D exp() {
		double expValue = Math.exp(this.value);
		return this.set(
			expValue,
			expValue * this.dx,
			expValue * this.dy,
			expValue * this.dz
		);
	}

	public Derivative3D log() {
		return this.set(
			Math.log(this.value),
			this.dx / this.value,
			this.dy / this.value,
			this.dz / this.value
		);
	}

	//////////////////////////////// trig ////////////////////////////////

	public Derivative3D sin() {
		double chain = Math.cos(this.value);
		return this.set(
			Math.sin(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	public Derivative3D cos() {
		double chain = -Math.sin(this.value);
		return this.set(
			Math.cos(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	public Derivative3D tan() {
		double chain = BigGlobeMath.squareD(1.0D / Math.cos(this.value));
		return this.set(
			Math.tan(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	//////////////////////////////// hyperbolic trig ////////////////////////////////

	public Derivative3D sinh() {
		double chain = Math.cosh(this.value);
		return this.set(
			Math.sinh(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	public Derivative3D cosh() {
		double chain = -Math.sinh(this.value);
		return this.set(
			Math.cosh(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	public Derivative3D tanh() {
		double chain = BigGlobeMath.squareD(1.0D / Math.cosh(this.value));
		return this.set(
			Math.tanh(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	//////////////////////////////// inverse trig ////////////////////////////////

	public Derivative3D asin() {
		double chain = 1.0D / Math.sqrt(1.0D - BigGlobeMath.squareD(this.value));
		return this.set(
			Math.asin(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	public Derivative3D acos() {
		double chain = -1.0D / Math.sqrt(1.0D - BigGlobeMath.squareD(this.value));
		return this.set(
			Math.acos(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	public Derivative3D atan() {
		double chain = 1.0D / (BigGlobeMath.squareD(this.value) + 1.0D);
		return this.set(
			Math.atan(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	//////////////////////////////// inverse hyperbolic trig ////////////////////////////////

	public Derivative3D asinh() {
		double chain = 1.0D / Math.sqrt(BigGlobeMath.squareD(this.value) + 1.0D);
		return this.set(
			BigGlobeMath.atanh(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	public Derivative3D acosh() {
		double chain = Math.sqrt(BigGlobeMath.squareD(this.value) - 1.0D);
		return this.set(
			BigGlobeMath.acosh(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	public Derivative3D atanh() {
		double chain = 1.0D / (1.0D - BigGlobeMath.squareD(this.value));
		return this.set(
			BigGlobeMath.atanh(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	//////////////////////////////// misc ////////////////////////////////

	public Derivative3D chain(DoubleUnaryOperator function, DoubleUnaryOperator derivative) {
		double chain = derivative.applyAsDouble(this.value);
		return this.set(
			function.applyAsDouble(this.value),
			chain * this.dx,
			chain * this.dy,
			chain * this.dz
		);
	}

	@Override
	public String toString() {
		return this.value + " changing by (" + this.dx + ", " + this.dy + ", " + this.dz + ')';
	}

	@Override
	public Derivative3D clone() {
		try {
			return (Derivative3D)(super.clone());
		}
		catch (CloneNotSupportedException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}
}