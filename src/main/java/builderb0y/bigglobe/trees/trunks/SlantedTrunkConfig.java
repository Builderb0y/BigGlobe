package builderb0y.bigglobe.trees.trunks;

public class SlantedTrunkConfig extends TrunkConfig {

	public final double dx, dz;

	public SlantedTrunkConfig(
		double startX,
		int startY,
		double startZ,
		int height,
		double startRadius,
		double dx,
		double dz,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		super(startX, startY, startZ, height, startRadius, requireValidGround, canGenerateInLiquid);
		this.dx = dx;
		this.dz = dz;
	}

	public static SlantedTrunkConfig create(
		double originX,
		int originY,
		double originZ,
		int height,
		double startRadius,
		double slantAngle,
		double slantAmount,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		return new SlantedTrunkConfig(
			originX,
			originY,
			originZ,
			height,
			startRadius,
			Math.cos(slantAngle) * slantAmount,
			Math.sin(slantAngle) * slantAmount,
			requireValidGround,
			canGenerateInLiquid
		);
	}

	@Override
	public void setFrac(double fracY) {
		super.setFrac(fracY);
		this.currentX = this.startX + this.dx * fracY * this.height;
		this.currentZ = this.startZ + this.dz * fracY * this.height;
	}
}