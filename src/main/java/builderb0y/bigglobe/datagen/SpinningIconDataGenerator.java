package builderb0y.bigglobe.datagen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Math;

import javax.imageio.ImageIO;

import org.joml.*;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;

public class SpinningIconDataGenerator {

	public static final long
		heightSeed = 0xFA9FFE76D640E288L,
		temperatureSeed = 0x13A5921433CB11EAL;
	public static final Vector2d
		uv = new Vector2d(),
		rotated2D = new Vector2d();
	public static final Vector3d
		ray = new Vector3d(),
		intersection = new Vector3d(),
		color = new Vector3d(),
		unit = new Vector3d(),
		closestToCenterPoint = new Vector3d(),
		noisePos = new Vector3d(),
		mountainNoisePos = new Vector3d(),
		sandColor = new Vector3d(0.863D, 0.831D, 0.631D),
		waterColor = new Vector3d(0.125D, 0.125D, 1.0D),
		grassColor = new Vector3d(),
		stoneColor = new Vector3d(0.5D),
		snowColor = new Vector3d(1.0D);
	public static final Vector4d
		sphere = new Vector4d(0.0D, 0.0D, 2.0D, 1.0D),
		projected4D = new Vector4d();
	public static final Matrix2d
		globeRotation = new Matrix2d();
	public static final Matrix4d
		inverseProjection = new Matrix4d().setPerspective(Math.toRadians(80.0D), 1.0D, 0.05D, 5.0D).invert();
	public static final int[]
		pixel = new int[1];
	public static final BufferedImage
		image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
	public static final Vector3d[]
		gayColors = {
			new Vector3d(1.0D, 0.0D, 0.0D),
			new Vector3d(1.0D, 0.375D, 0.0D),
			new Vector3d(1.0D, 1.0D, 0.0D),
			new Vector3d(0.0D, 0.75D, 0.1875D),
			new Vector3d(0.0D, 0.375D, 1.0D),
			new Vector3d(0.5D, 0.0D, 0.5D)
		},
		transColors = {
			new Vector3d(0.375D, 0.75D, 1.0D),
			new Vector3d(0.9375D, 0.9375D, 0.9375D),
			new Vector3d(1.0D, 0.6666666666666666D, 0.7D)
		};
	public static final int frames = 200;
	public static double time;

	public static void main(String[] args) throws IOException {
		new File("src/main/resources/wip/icon").mkdirs();
		for (int frame = 0; frame < frames; frame++) {
			System.out.println("Frame " + frame);
			time = ((double)(frame)) / ((double)(frames));
			globeRotation.rotation(time * -BigGlobeMath.TAU);
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					uv.set(x + 0.5D, y + 0.5D).div(image.getWidth(), image.getHeight()).mul(2.0D).sub(1.0D, 1.0D);
					computeColor();
					pixel[0] = (
						0xFF000000 |
						(Interpolator.clamp(0, 255, (int)(color.x * 256.0D)) << 16) |
						(Interpolator.clamp(0, 255, (int)(color.y * 256.0D)) <<  8) |
						(Interpolator.clamp(0, 255, (int)(color.z * 256.0D))      )
					);
					image.getRaster().setDataElements(x, y, pixel);
				}
			}
			ImageIO.write(image, "png", new File("src/main/resources/wip/icon/frame" + (char)((frame / 100) + '0') + (char)(((frame / 10) % 10) + '0') + (char)((frame % 10) + '0') + ".png"));
		}
	}

	public static void computeColor() {
		projected4D.set(uv.x, uv.y, 1.0D, 1.0D);
		inverseProjection.transform(projected4D);
		ray.set(projected4D.x, projected4D.y, projected4D.z).normalize();
		ray.z = -ray.z;
		double uvLengthSquared;
		if (intersect()) {
			intersection.sub(sphere.x, sphere.y, sphere.z, unit);
			globeRotation.transform(rotated2D.set(unit.x, unit.z));
			unit.x = rotated2D.x;
			unit.z = rotated2D.y;

			double noise = 0.0D;
			unit.mul( 2.0D, mountainNoisePos); noise += mountain(Permuter.permute(heightSeed, 0)) * 0.5D;
			unit.mul( 4.0D, mountainNoisePos); noise -= mountain(Permuter.permute(heightSeed, 1)) * 0.25D;
			unit.mul( 8.0D, mountainNoisePos); noise += mountain(Permuter.permute(heightSeed, 2)) * 0.125D;
			unit.mul(16.0D, mountainNoisePos); noise -= mountain(Permuter.permute(heightSeed, 3)) * 0.0625D;
			unit.mul(32.0D, mountainNoisePos); noise += mountain(Permuter.permute(heightSeed, 4)) * 0.03125D;
			noise /= 1.0D - 0.03125D;
			noise *= 1024.0D;

			double temperature = 0.0D;
			unit.mul( 4.0D, noisePos); temperature += noise(Permuter.permute(temperatureSeed, 0)) * 0.5D;
			unit.mul( 8.0D, noisePos); temperature += noise(Permuter.permute(temperatureSeed, 1)) * 0.25D;
			unit.mul(16.0D, noisePos); temperature += noise(Permuter.permute(temperatureSeed, 2)) * 0.125D;
			unit.mul(32.0D, noisePos); temperature += noise(Permuter.permute(temperatureSeed, 3)) * 0.0625D;
			unit.mul(64.0D, noisePos); temperature += noise(Permuter.permute(temperatureSeed, 4)) * 0.03125D;
			temperature /= 1.0D - 0.03125D;
			temperature -= 0.5D + noise / 2048.0D;

			grassColor.set(0.5D + Math.max(temperature, 0.0D), 0.75D, 0.375D + Math.max(-temperature, 0.0D)).mul(0.8125D);

			if (noise < 0.0D) {
				color.set(waterColor).mul(Interpolator.unmixLinear(-1024.0D, 0.0D, noise));
			}
			else if (noise < 32) {
				mix(waterColor, sandColor, Interpolator.unmixSmooth(0.0D, 32.0D, noise), color);
			}
			else if (noise < 64.0D) {
				mix(sandColor, grassColor, Interpolator.unmixSmooth(32.0D, 64.0D, noise), color);
			}
			else if (noise < 256.0D) {
				mix(grassColor, stoneColor, Interpolator.unmixSmooth(64.0D, 256.0D, noise), color);
			}
			else if (noise < 512.0D) {
				mix(stoneColor, snowColor, Interpolator.unmixSmooth(256.0D, 512.0D, noise), color);
			}
			else {
				color.set(snowColor);
			}
		}
		else if ((uvLengthSquared = uv.lengthSquared()) < 1.0D && uvLengthSquared > 0.8125D * 0.8125D) {
			double angle = Math.atan2(-uv.y, uv.x) * (6.0D / BigGlobeMath.TAU) - (time * 6.0D);
			double fractAngle = BigGlobeMath.modulus_BP(angle, 1.0D);
			if (fractAngle > 0.25D && fractAngle < 0.75D) {
				color.set(gayColors[BigGlobeMath.modulus_BP(BigGlobeMath.floorI(angle), 6)]);
			}
			else if (fractAngle < 0.125D || fractAngle > 0.875D) {
				color.set(transColors[BigGlobeMath.modulus_BP(BigGlobeMath.roundI(angle), 3)]);
			}
			else {
				color.set(0.0D);
			}
		}
		else {
			color.set(0.0D);
		}
	}

	public static void mix(Vector3d a, Vector3d b, double amount, Vector3d destination) {
		destination.x = Interpolator.mixLinear(a.x, b.x, amount);
		destination.y = Interpolator.mixLinear(a.y, b.y, amount);
		destination.z = Interpolator.mixLinear(a.z, b.z, amount);
	}

	public static boolean intersect() {
		double distanceFromOriginToClosestToCenter = ray.dot(sphere.x, sphere.y, sphere.z);
		ray.mul(distanceFromOriginToClosestToCenter, closestToCenterPoint);
		double distanceSquaredFromCenterToClosestToCenter = closestToCenterPoint.distanceSquared(sphere.x, sphere.y, sphere.z);
		if (distanceSquaredFromCenterToClosestToCenter > sphere.w * sphere.w) return false;
		double backtrack = Math.sqrt(sphere.w * sphere.w - distanceSquaredFromCenterToClosestToCenter);
		double distanceFromOriginToIntersection = distanceFromOriginToClosestToCenter - backtrack;
		ray.mul(distanceFromOriginToIntersection, intersection);
		return true;
	}

	public static double mountain(long seed) {
		double noise = 0.0D;
		noisePos.set(mountainNoisePos);        noise += noise(Permuter.permute(seed, 0)) * 0.5D;
		mountainNoisePos.mul( 2.0D, noisePos); noise += noise(Permuter.permute(seed, 1)) * 0.25D;
		mountainNoisePos.mul( 4.0D, noisePos); noise += noise(Permuter.permute(seed, 2)) * 0.125D;
		mountainNoisePos.mul( 8.0D, noisePos); noise += noise(Permuter.permute(seed, 3)) * 0.0625D;
		mountainNoisePos.mul(16.0D, noisePos); noise += noise(Permuter.permute(seed, 4)) * 0.03125D;
		noise /= 1.0D - 0.03125D;
		noise = Math.abs(noise * 2.0D - 1.0D);
		noise *= 2.0D - noise;
		return noise * 2.0D - 1.0D;
	}

	public static double noise(long seed) {
		int floorX = BigGlobeMath.floorI(noisePos.x);
		int floorY = BigGlobeMath.floorI(noisePos.y);
		int floorZ = BigGlobeMath.floorI(noisePos.z);
		int ceilX = floorX + 1;
		int ceilY = floorY + 1;
		int ceilZ = floorZ + 1;
		double smoothX = Interpolator.smooth(noisePos.x - floorX);
		double smoothY = Interpolator.smooth(noisePos.y - floorY);
		double smoothZ = Interpolator.smooth(noisePos.z - floorZ);

		return Interpolator.mixLinear(
			Interpolator.mixLinear(
				Interpolator.mixLinear(
					hash(seed, floorX, floorY, floorZ),
					hash(seed, floorX, floorY,  ceilZ),
					smoothZ
				),
				Interpolator.mixLinear(
					hash(seed, floorX, ceilY, floorZ),
					hash(seed, floorX, ceilY,  ceilZ),
					smoothZ
				),
				smoothY
			),
			Interpolator.mixLinear(
				Interpolator.mixLinear(
					hash(seed,  ceilX, floorY, floorZ),
					hash(seed,  ceilX, floorY,  ceilZ),
					smoothZ
				),
				Interpolator.mixLinear(
					hash(seed,  ceilX, ceilY, floorZ),
					hash(seed,  ceilX, ceilY,  ceilZ),
					smoothZ
				),
				smoothY
			),
			smoothX
		);
	}

	public static double hash(long seed, int x, int y, int z) {
		return Permuter.nextPositiveDouble(Permuter.permute(seed, x, y, z));
	}
}