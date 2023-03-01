package builderb0y.bigglobe.math.pointSequences;

import java.util.random.RandomGenerator;

import builderb0y.bigglobe.noise.Permuter;

public class GoldenSpiralIterator implements PointIterator2D {

	//https://en.wikipedia.org/wiki/Golden_angle
	public static final double
		GOLDEN_ANGLE     = 2.39996322972865332D,
		SIN_GOLDEN_ANGLE = Math.sin(GOLDEN_ANGLE),
		COS_GOLDEN_ANGLE = Math.cos(GOLDEN_ANGLE);

	public double originX, originY;
	public double radiusStepSize;

	public double normX, normY;
	public double x, y;
	public double radius;
	public int index;

	public GoldenSpiralIterator(double originX, double originY, double radiusStepSize, double startAngle) {
		this.originX = originX;
		this.originY = originY;
		this.radiusStepSize = radiusStepSize;
		this.normX = Math.cos(startAngle);
		this.normY = Math.sin(startAngle);
		this.x = originX;
		this.y = originY;
	}

	public GoldenSpiralIterator(double originX, double originY, double radiusStepSize, RandomGenerator randomStartAngle) {
		this(originX, originY, radiusStepSize, randomStartAngle.nextDouble() * (Math.PI * 2.0D));
	}

	public GoldenSpiralIterator(double originX, double originY, double radiusStepSize, long randomStartAngleSeed) {
		this(originX, originY, radiusStepSize, Permuter.nextPositiveDouble(randomStartAngleSeed) * (Math.PI * 2.0D));
	}

	@Override
	public void next() {
		double normX = this.normX;
		double normZ = this.normY;
		this.normX = normX * COS_GOLDEN_ANGLE + normZ * -SIN_GOLDEN_ANGLE;
		this.normY = normX * SIN_GOLDEN_ANGLE + normZ *  COS_GOLDEN_ANGLE;
		this.index++;
		this.radius = this.index * this.radiusStepSize;
		this.x = this.originX + this.normX * this.radius;
		this.y = this.originY + this.normY * this.radius;
	}

	@Override public int index() { return this.index; }
	@Override public double x() { return this.x; }
	@Override public double y() { return this.y; }
}