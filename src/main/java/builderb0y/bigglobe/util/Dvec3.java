package builderb0y.bigglobe.util;

import java.util.random.RandomGenerator;

import net.minecraft.util.math.Position;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

public class Dvec3 implements Position, Cloneable {

	public double x, y, z;

	public Dvec3() {}

	public Dvec3(double value) {
		this.x = value;
		this.y = value;
		this.z = value;
	}

	public Dvec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Dvec3(Dvec3 that) {
		this.x = that.x;
		this.y = that.y;
		this.z = that.z;
	}

	//////////////// get ////////////////

	@Override public double getX() { return this.x; }
	@Override public double getY() { return this.y; }
	@Override public double getZ() { return this.z; }

	//////////////// set ////////////////

	public Dvec3 x(double x) { this.x = x; return this; }
	public Dvec3 y(double y) { this.y = y; return this; }
	public Dvec3 z(double z) { this.z = z; return this; }

	public Dvec3 set(double value) {
		return this.set(value, value, value);
	}

	public Dvec3 set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	public Dvec3 set(Dvec3 that) {
		return this.set(that.x, that.y, that.z);
	}

	//////////////// sphere generation ////////////////

	public Dvec3 setInSphere(RandomGenerator random, double radius) {
		double x, y, z, r;
		do {
			x = Permuter.nextUniformDouble(random);
			y = Permuter.nextUniformDouble(random);
			z = Permuter.nextUniformDouble(random);
			r = BigGlobeMath.squareD(x, y, z);
		}
		while (r > 1.0D);
		return this.set(x * radius, y * radius, z * radius);
	}

	public Dvec3 setOnSphere(RandomGenerator random, double radius) {
		double x = Permuter.nextUniformDouble(random);
		double y = random.nextDouble() * BigGlobeMath.TAU;
		double r = Math.sqrt(1.0D - x * x) * radius;
		return this.set(Math.cos(y) * r, Math.sin(y) * r, x * radius);
	}

	//////////////// add ////////////////

	public Dvec3 add(double value) {
		return this.add(value, value, value);
	}

	public Dvec3 add(double x, double y, double z) {
		return this.set(this.x + x, this.y + y, this.z + z);
	}

	public Dvec3 add(Dvec3 that) {
		return this.add(this, that);
	}

	public Dvec3 add(Dvec3 a, Dvec3 b) {
		return this.set(a.x + b.x, a.y + b.y, a.z + b.z);
	}

	//////////////// subtract ////////////////

	public Dvec3 subtract(double value) {
		return this.subtract(value, value, value);
	}

	public Dvec3 subtract(double x, double y, double z) {
		return this.set(this.x - x, this.y - y, this.z - z);
	}

	public Dvec3 subtract(Dvec3 that) {
		return this.subtract(this, that);
	}

	public Dvec3 subtract(Dvec3 a, Dvec3 b) {
		return this.set(a.x - b.x, a.y - b.y, a.z - b.z);
	}

	//////////////// multiply ////////////////

	public Dvec3 multiply(double value) {
		return this.multiply(value, value, value);
	}

	public Dvec3 multiply(double x, double y, double z) {
		return this.set(this.x * x, this.y * y, this.z * z);
	}

	public Dvec3 multiply(Dvec3 that) {
		return this.multiply(this, that);
	}

	public Dvec3 multiply(Dvec3 a, Dvec3 b) {
		return this.set(a.x * b.x, a.y * b.y, a.z * b.z);
	}

	//////////////// divide ////////////////

	public Dvec3 divide(double value) {
		return this.divide(value, value, value);
	}

	public Dvec3 divide(double x, double y, double z) {
		return this.set(this.x / x, this.y / y, this.z / z);
	}

	public Dvec3 divide(Dvec3 that) {
		return this.divide(this, that);
	}

	public Dvec3 divide(Dvec3 a, Dvec3 b) {
		return this.set(a.x / b.x, a.y / b.y, a.z / b.z);
	}

	//////////////// dot ////////////////

	public double dot(Dvec3 that) {
		return this.x * that.x + this.y * that.y + this.z * that.z;
	}

	public double dotNorm(Dvec3 that) {
		double div = Math.sqrt(this.lengthSquared() * that.lengthSquared());
		return div == 0.0D ? 0.0D : this.dot(that) / div;
	}

	//////////////// length ////////////////

	public double lengthSquared() {
		return BigGlobeMath.squareD(this.x, this.y, this.z);
	}

	public double length() {
		return Math.sqrt(this.lengthSquared());
	}

	//////////////// distance ////////////////

	public double distanceSquared(Dvec3 that) {
		return BigGlobeMath.squareD(this.x - that.x, this.y - that.y, this.z - that.z);
	}

	public double distance(Dvec3 that) {
		return Math.sqrt(this.distanceSquared(that));
	}

	//////////////// cross ////////////////

	public Dvec3 cross(Dvec3 that) {
		return this.cross(this, that);
	}

	public Dvec3 cross(Dvec3 a, Dvec3 b) {
		return this.set(
			a.y * b.z - a.z * b.y,
			a.x * b.z - a.z * b.x,
			a.x * b.y - a.y * b.x
		);
	}

	//////////////// misc ////////////////

	public Dvec3 negate() {
		return this.set(-this.x, -this.y, -this.z);
	}

	public Dvec3 normalize() {
		double length = this.length();
		return length == 0.0D ? this : this.divide(length);
	}

	public Dvec3 setLength(double newLength) {
		double oldLength = this.length();
		return oldLength == 0.0D ? this : this.multiply(newLength / oldLength);
	}

	@Override
	public int hashCode() {
		int hash = Double.hashCode(this.x);
		hash = hash * 31 + Double.hashCode(this.y);
		hash = hash * 31 + Double.hashCode(this.z);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof Dvec3 that &&
			Double.doubleToLongBits(this.x) == Double.doubleToLongBits(that.x) &&
			Double.doubleToLongBits(this.y) == Double.doubleToLongBits(that.y) &&
			Double.doubleToLongBits(this.z) == Double.doubleToLongBits(that.z)
		);
	}

	@Override
	public String toString() {
		return "Dvec3(" + this.x + ", " + this.y + ", " + this.z + ')';
	}

	@Override
	public Dvec3 clone() {
		try {
			return (Dvec3)(super.clone());
		}
		catch (CloneNotSupportedException exception) {
			throw new Error(exception);
		}
	}
}