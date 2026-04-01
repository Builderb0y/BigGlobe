package builderb0y.bigglobe.util;

import java.util.function.DoubleUnaryOperator;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.math.BigGlobeMath;

public class Derivative1D implements Cloneable {

	public double value, dx;

	public Derivative1D() {}

	public Derivative1D(double value) {
		this.value = value;
	}

	public Derivative1D(double value, double dx) {
		this.value = value;
		this.dx = dx;
	}

	public Derivative1D(Derivative1D that) {
		this.value = that.value;
		this.dx = that.dx;
	}

	//////////////////////////////// set ////////////////////////////////

	public Derivative1D set(double value) {
		return this.set(value, 0.0D);
	}

	public Derivative1D set(double value, double dx) {
		this.value = value;
		this.dx = dx;
		return this;
	}

	public Derivative1D set(Derivative1D that) {
		return this.set(that.value, that.dx);
	}

	//////////////////////////////// add ////////////////////////////////

	public Derivative1D add(double value) {
		this.value += value;
		return this;
	}

	public Derivative1D add(double value, double dx) {
		return this.set(this.value + value, this.dx + dx);
	}

	public Derivative1D add(Derivative1D that) {
		return this.add(that.value, that.dx);
	}

	//////////////////////////////// sub ////////////////////////////////

	public Derivative1D sub(double value) {
		this.value -= value;
		return this;
	}

	public Derivative1D sub(double value, double dx) {
		return this.set(this.value - value, this.dx - dx);
	}

	public Derivative1D sub(Derivative1D that) {
		return this.sub(that.value, that.dx);
	}

	//////////////////////////////// subRev ////////////////////////////////

	public Derivative1D subRev(double value) {
		return this.set(value - this.value, -this.dx);
	}

	public Derivative1D subRev(double value, double dx) {
		return this.set(value - this.value, dx - this.dx);
	}

	public Derivative1D subRev(Derivative1D that) {
		return this.subRev(that.value, that.dx);
	}

	//////////////////////////////// mul ////////////////////////////////

	public Derivative1D mul(double value) {
		return this.set(this.value * value, this.dx * value);
	}

	public Derivative1D mul(double value, double dx) {
		return this.set(
			this.value * value,
			this.value * dx + this.dx * value
		);
	}

	public Derivative1D mul(Derivative1D that) {
		return this.mul(that.value, that.dx);
	}

	//////////////////////////////// div ////////////////////////////////

	public Derivative1D div(double value) {
		return this.set(this.value / value, this.dx / value);
	}

	public Derivative1D div(double value, double dx) {
		double valueSquared = value * value;
		return this.set(
			this.value / value,
			(value * this.dx - dx * this.value) / valueSquared
		);
	}

	public Derivative1D div(Derivative1D that) {
		return this.div(that.value, that.dx);
	}

	//////////////////////////////// divRev ////////////////////////////////

	public Derivative1D divRev(double value) {
		double minusValueSquared = -BigGlobeMath.squareD(this.value);
		return this.set(
			value / this.value,
			(this.dx * value) / minusValueSquared
		);
	}

	public Derivative1D divRev(double value, double dx) {
		double valueSquared = this.value * this.value;
		return this.set(
			value / this.value,
			(this.value * dx - this.dx * value) / valueSquared
		);
	}

	public Derivative1D divRev(Derivative1D that) {
		return this.divRev(that.value, that.dx);
	}

	//////////////////////////////// pos ////////////////////////////////

	public Derivative1D pow(double exponent) {
		double chain = exponent * Math.pow(this.value, exponent - 1.0D);
		return this.set(
			Math.pow(this.value, exponent),
			chain * this.dx
		);
	}

	public Derivative1D pow(double value, double dx) {
		double pow = Math.pow(this.value, value);
		double common = value / this.value;
		double logValue = Math.log(this.value);
		return this.set(
			pow,
			pow * (common * this.dx + logValue * dx)
		);
	}

	public Derivative1D pow(Derivative1D that) {
		return this.pow(that.value, that.dx);
	}

	//////////////////////////////// unary ////////////////////////////////

	public Derivative1D negate() {
		return this.set(-this.value, -this.dx);
	}

	//////////////////////////////// simple powers ////////////////////////////////

	public Derivative1D rcp() {
		double minusValueSquared = -BigGlobeMath.squareD(this.value);
		return this.set(
			1.0D / this.value,
			this.dx / minusValueSquared
		);
	}

	public Derivative1D square() {
		double twoValue = this.value * 2.0D;
		return this.set(
			BigGlobeMath.squareD(this.value),
			twoValue * this.dx
		);
	}

	public Derivative1D sqrt() {
		double sqrtValue = Math.sqrt(this.value);
		double halfOverSqrtValue = 0.5D / sqrtValue;
		return this.set(
			sqrtValue,
			halfOverSqrtValue * this.dx
		);
	}

	//////////////////////////////// exponentials ////////////////////////////////

	public Derivative1D exp() {
		double expValue = Math.exp(this.value);
		return this.set(
			expValue,
			expValue * this.dx
		);
	}

	public Derivative1D log() {
		return this.set(
			Math.log(this.value),
			this.dx / this.value
		);
	}

	//////////////////////////////// trig ////////////////////////////////

	public Derivative1D sin() {
		double chain = Math.cos(this.value);
		return this.set(
			Math.sin(this.value),
			chain * this.dx
		);
	}

	public Derivative1D cos() {
		double chain = -Math.sin(this.value);
		return this.set(
			Math.cos(this.value),
			chain * this.dx
		);
	}

	public Derivative1D tan() {
		double chain = BigGlobeMath.squareD(1.0D / Math.cos(this.value));
		return this.set(
			Math.tan(this.value),
			chain * this.dx
		);
	}

	//////////////////////////////// hyperbolic trig ////////////////////////////////

	public Derivative1D sinh() {
		double chain = Math.cosh(this.value);
		return this.set(
			Math.sinh(this.value),
			chain * this.dx
		);
	}

	public Derivative1D cosh() {
		double chain = -Math.sinh(this.value);
		return this.set(
			Math.cosh(this.value),
			chain * this.dx
		);
	}

	public Derivative1D tanh() {
		double chain = BigGlobeMath.squareD(1.0D / Math.cosh(this.value));
		return this.set(
			Math.tanh(this.value),
			chain * this.dx
		);
	}

	//////////////////////////////// inverse trig ////////////////////////////////

	public Derivative1D asin() {
		double chain = 1.0D / Math.sqrt(1.0D - BigGlobeMath.squareD(this.value));
		return this.set(
			Math.asin(this.value),
			chain * this.dx
		);
	}

	public Derivative1D acos() {
		double chain = -1.0D / Math.sqrt(1.0D - BigGlobeMath.squareD(this.value));
		return this.set(
			Math.acos(this.value),
			chain * this.dx
		);
	}

	public Derivative1D atan() {
		double chain = 1.0D / (BigGlobeMath.squareD(this.value) + 1.0D);
		return this.set(
			Math.atan(this.value),
			chain * this.dx
		);
	}

	//////////////////////////////// inverse hyperbolic trig ////////////////////////////////

	public Derivative1D asinh() {
		double chain = 1.0D / Math.sqrt(BigGlobeMath.squareD(this.value) + 1.0D);
		return this.set(
			BigGlobeMath.atanh(this.value),
			chain * this.dx
		);
	}

	public Derivative1D acosh() {
		double chain = Math.sqrt(BigGlobeMath.squareD(this.value) - 1.0D);
		return this.set(
			BigGlobeMath.acosh(this.value),
			chain * this.dx
		);
	}

	public Derivative1D atanh() {
		double chain = 1.0D / (1.0D - BigGlobeMath.squareD(this.value));
		return this.set(
			BigGlobeMath.atanh(this.value),
			chain * this.dx
		);
	}

	//////////////////////////////// misc ////////////////////////////////

	public Derivative1D chain(DoubleUnaryOperator function, DoubleUnaryOperator derivative) {
		double chain = derivative.applyAsDouble(this.value);
		return this.set(
			function.applyAsDouble(this.value),
			chain * this.dx
		);
	}

	@Override
	public String toString() {
		return this.value + " changing by " + this.dx;
	}

	@Override
	public Derivative1D clone() {
		try {
			return (Derivative1D)(super.clone());
		}
		catch (CloneNotSupportedException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}
}