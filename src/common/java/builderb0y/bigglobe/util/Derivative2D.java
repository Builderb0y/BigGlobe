package builderb0y.bigglobe.util;

import java.util.function.DoubleUnaryOperator;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.math.BigGlobeMath;

public class Derivative2D implements Cloneable {

	public double value, dx, dy;

	public Derivative2D() {}

	public Derivative2D(double value) {
		this.value = value;
	}

	public Derivative2D(double value, double dx, double dy) {
		this.value = value;
		this.dx = dx;
		this.dy = dy;
	}

	public Derivative2D(Derivative2D that) {
		this.value = that.value;
		this.dx = that.dx;
		this.dy = that.dy;
	}

	//////////////////////////////// set ////////////////////////////////

	public Derivative2D set(double value) {
		return this.set(value, 0.0D, 0.0D);
	}

	public Derivative2D set(double value, double dx, double dy) {
		this.value = value;
		this.dx = dx;
		this.dy = dy;
		return this;
	}

	public Derivative2D set(Derivative2D that) {
		return this.set(that.value, that.dx, that.dy);
	}

	//////////////////////////////// add ////////////////////////////////

	public Derivative2D add(double value) {
		this.value += value;
		return this;
	}

	public Derivative2D add(double value, double dx, double dy) {
		return this.set(this.value + value, this.dx + dx, this.dy + dy);
	}

	public Derivative2D add(Derivative2D that) {
		return this.add(that.value, that.dx, that.dy);
	}

	//////////////////////////////// sub ////////////////////////////////

	public Derivative2D sub(double value) {
		this.value -= value;
		return this;
	}

	public Derivative2D sub(double value, double dx, double dy) {
		return this.set(this.value - value, this.dx - dx, this.dy - dy);
	}

	public Derivative2D sub(Derivative2D that) {
		return this.sub(that.value, that.dx, that.dy);
	}

	//////////////////////////////// subRev ////////////////////////////////

	public Derivative2D subRev(double value) {
		return this.set(value - this.value, -this.dx, -this.dy);
	}

	public Derivative2D subRev(double value, double dx, double dy) {
		return this.set(value - this.value, dx - this.dx, dy - this.dy);
	}

	public Derivative2D subRev(Derivative2D that) {
		return this.subRev(that.value, that.dx, that.dy);
	}

	//////////////////////////////// mul ////////////////////////////////

	public Derivative2D mul(double value) {
		return this.set(this.value * value, this.dx * value, this.dy * value);
	}

	public Derivative2D mul(double value, double dx, double dy) {
		return this.set(
			this.value * value,
			this.value * dx + this.dx * value,
			this.value * dy + this.dy * value
		);
	}

	public Derivative2D mul(Derivative2D that) {
		return this.mul(that.value, that.dx, that.dy);
	}

	//////////////////////////////// div ////////////////////////////////

	public Derivative2D div(double value) {
		return this.set(this.value / value, this.dx / value, this.dy / value);
	}

	public Derivative2D div(double value, double dx, double dy) {
		double valueSquared = value * value;
		return this.set(
			this.value / value,
			(value * this.dx - dx * this.value) / valueSquared,
			(value * this.dy - dy * this.value) / valueSquared
		);
	}

	public Derivative2D div(Derivative2D that) {
		return this.div(that.value, that.dx, that.dy);
	}

	//////////////////////////////// divRev ////////////////////////////////

	public Derivative2D divRev(double value) {
		double minusValueSquared = -BigGlobeMath.squareD(this.value);
		return this.set(
			value / this.value,
			(this.dx * value) / minusValueSquared,
			(this.dy * value) / minusValueSquared
		);
	}

	public Derivative2D divRev(double value, double dx, double dy) {
		double valueSquared = this.value * this.value;
		return this.set(
			value / this.value,
			(this.value * dx - this.dx * value) / valueSquared,
			(this.value * dy - this.dy * value) / valueSquared
		);
	}

	public Derivative2D divRev(Derivative2D that) {
		return this.divRev(that.value, that.dx, that.dy);
	}

	//////////////////////////////// pos ////////////////////////////////

	public Derivative2D pow(double exponent) {
		double common = exponent * Math.pow(this.value, exponent - 1.0D);
		return this.set(
			Math.pow(this.value, exponent),
			common * this.dx,
			common * this.dy
		);
	}

	public Derivative2D pow(double value, double dx, double dy) {
		double pow = Math.pow(this.value, value);
		double common = value / this.value;
		double logValue = Math.log(this.value);
		return this.set(
			pow,
			pow * (common * this.dx + logValue * dx),
			pow * (common * this.dy + logValue * dy)
		);
	}

	public Derivative2D pow(Derivative2D that) {
		return this.pow(that.value, that.dx, that.dy);
	}

	//////////////////////////////// unary ////////////////////////////////

	public Derivative2D negate() {
		return this.set(-this.value, -this.dx, -this.dy);
	}

	//////////////////////////////// simple powers ////////////////////////////////

	public Derivative2D rcp() {
		double minusValueSquared = -BigGlobeMath.squareD(this.value);
		return this.set(
			1.0D / this.value,
			this.dx / minusValueSquared,
			this.dy / minusValueSquared
		);
	}

	public Derivative2D square() {
		double twoValue = this.value * 2.0D;
		return this.set(
			BigGlobeMath.squareD(this.value),
			twoValue * this.dx,
			twoValue * this.dy
		);
	}

	public Derivative2D sqrt() {
		double sqrtValue = Math.sqrt(this.value);
		double halfOverSqrtValue = 0.5D / sqrtValue;
		return this.set(
			sqrtValue,
			halfOverSqrtValue * this.dx,
			halfOverSqrtValue * this.dy
		);
	}

	//////////////////////////////// exponentials ////////////////////////////////

	public Derivative2D exp() {
		double expValue = Math.exp(this.value);
		return this.set(
			expValue,
			expValue * this.dx,
			expValue * this.dy
		);
	}

	public Derivative2D log() {
		return this.set(
			Math.log(this.value),
			this.dx / this.value,
			this.dy / this.value
		);
	}

	//////////////////////////////// trig ////////////////////////////////

	public Derivative2D sin() {
		double chain = Math.cos(this.value);
		return this.set(
			Math.sin(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	public Derivative2D cos() {
		double chain = -Math.sin(this.value);
		return this.set(
			Math.cos(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	public Derivative2D tan() {
		double chain = BigGlobeMath.squareD(1.0D / Math.cos(this.value));
		return this.set(
			Math.tan(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	//////////////////////////////// hyperbolic trig ////////////////////////////////

	public Derivative2D sinh() {
		double chain = Math.cosh(this.value);
		return this.set(
			Math.sinh(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	public Derivative2D cosh() {
		double chain = -Math.sinh(this.value);
		return this.set(
			Math.cosh(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	public Derivative2D tanh() {
		double chain = BigGlobeMath.squareD(1.0D / Math.cosh(this.value));
		return this.set(
			Math.tanh(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	//////////////////////////////// inverse trig ////////////////////////////////

	public Derivative2D asin() {
		double chain = 1.0D / Math.sqrt(1.0D - BigGlobeMath.squareD(this.value));
		return this.set(
			Math.asin(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	public Derivative2D acos() {
		double chain = -1.0D / Math.sqrt(1.0D - BigGlobeMath.squareD(this.value));
		return this.set(
			Math.acos(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	public Derivative2D atan() {
		double chain = 1.0D / (BigGlobeMath.squareD(this.value) + 1.0D);
		return this.set(
			Math.atan(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	//////////////////////////////// inverse hyperbolic trig ////////////////////////////////

	public Derivative2D asinh() {
		double chain = 1.0D / Math.sqrt(BigGlobeMath.squareD(this.value) + 1.0D);
		return this.set(
			BigGlobeMath.atanh(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	public Derivative2D acosh() {
		double chain = Math.sqrt(BigGlobeMath.squareD(this.value) - 1.0D);
		return this.set(
			BigGlobeMath.acosh(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	public Derivative2D atanh() {
		double chain = 1.0D / (1.0D - BigGlobeMath.squareD(this.value));
		return this.set(
			BigGlobeMath.atanh(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	//////////////////////////////// misc ////////////////////////////////

	public Derivative2D chain(DoubleUnaryOperator function, DoubleUnaryOperator derivative) {
		double chain = derivative.applyAsDouble(this.value);
		return this.set(
			function.applyAsDouble(this.value),
			chain * this.dx,
			chain * this.dy
		);
	}

	@Override
	public String toString() {
		return this.value + " changing by (" + this.dx + ", " + this.dy + ')';
	}

	@Override
	public Derivative2D clone() {
		try {
			return (Derivative2D)(super.clone());
		}
		catch (CloneNotSupportedException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}
}