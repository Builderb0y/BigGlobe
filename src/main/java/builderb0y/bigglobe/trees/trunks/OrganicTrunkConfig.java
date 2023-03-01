package builderb0y.bigglobe.trees.trunks;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.noise.CubicGrid1D;
import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.noise.SummingGrid1D;
import builderb0y.bigglobe.settings.Seed.NumberSeed;

public class OrganicTrunkConfig extends TrunkConfig {

	public final SummingGrid1D noiseX, noiseZ;

	public OrganicTrunkConfig(
		double startX,
		int startY,
		double startZ,
		int height,
		double startRadius,
		SummingGrid1D noiseX,
		SummingGrid1D noiseZ,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		super(startX, startY, startZ, height, startRadius, requireValidGround, canGenerateInLiquid);
		this.noiseX = noiseX;
		this.noiseZ = noiseZ;
	}

	public static OrganicTrunkConfig create(
		BlockPos origin,
		int height,
		double startRadius,
		RandomGenerator random,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		return new OrganicTrunkConfig(
			origin.getX() + random.nextDouble() - 0.5D,
			origin.getY(),
			origin.getZ() + random.nextDouble() - 0.5D,
			height,
			startRadius,
			newGrid(height, startRadius, random),
			newGrid(height, startRadius, random),
			requireValidGround,
			canGenerateInLiquid
		);
	}

	public static SummingGrid1D newGrid(int height, double radius, RandomGenerator random) {
		List<Grid1D> layers = new ArrayList<>(4);
		long seed = random.nextLong();
		while ((height >>= 1) >= 4) {
			layers.add(new CubicGrid1D(new NumberSeed(Permuter.permute(seed, layers.size())), radius, height));
			radius *= 0.5D;
		}
		return new SummingGrid1D(layers.toArray(new Grid1D[layers.size()]));
	}

	@Override
	public void setFrac(double fracY) {
		super.setFrac(fracY);
		int offset = (int)(fracY * this.height);
		double bias = fracY * (1.0D - fracY) * 4.0D;
		this.currentX = this.startX + this.noiseX.getValue(0L, offset) * bias;
		this.currentZ = this.startZ + this.noiseZ.getValue(0L, offset) * bias;
	}
}