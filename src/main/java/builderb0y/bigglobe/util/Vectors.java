package builderb0y.bigglobe.util;

import java.util.random.RandomGenerator;

import org.joml.Vector3d;
import org.joml.Vector3f;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

public class Vectors {

	public static Vector3d setInSphere(Vector3d vector, RandomGenerator random, double radius) {
		double x, y, z, r;
		do {
			x = Permuter.nextUniformDouble(random);
			y = Permuter.nextUniformDouble(random);
			z = Permuter.nextUniformDouble(random);
			r = BigGlobeMath.squareD(x, y, z);
		}
		while (r > 1.0D);
		return vector.set(x * radius, y * radius, z * radius);
	}

	public static Vector3d setOnSphere(Vector3d vector, RandomGenerator random, double radius) {
		double x = Permuter.nextUniformDouble(random);
		double y = random.nextDouble() * BigGlobeMath.TAU;
		double r = Math.sqrt(1.0D - x * x) * radius;
		return vector.set(Math.cos(y) * r, Math.sin(y) * r, x * radius);
	}

	public static Vector3f setInSphere(Vector3f vector, RandomGenerator random, float radius) {
		float x, y, z, r;
		do {
			x = Permuter.nextUniformFloat(random);
			y = Permuter.nextUniformFloat(random);
			z = Permuter.nextUniformFloat(random);
			r = BigGlobeMath.squareF(x, y, z);
		}
		while (r > 1.0D);
		return vector.set(x * radius, y * radius, z * radius);
	}

	public static Vector3f setOnSphere(Vector3f vector, RandomGenerator random, float radius) {
		double x = Permuter.nextUniformDouble(random);
		double y = random.nextDouble() * BigGlobeMath.TAU;
		double r = Math.sqrt(1.0D - x * x) * radius;
		return vector.set(Math.cos(y) * r, Math.sin(y) * r, x * radius);
	}
}