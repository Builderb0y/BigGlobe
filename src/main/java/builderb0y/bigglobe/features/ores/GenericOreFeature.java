package builderb0y.bigglobe.features.ores;

import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomSources.RandomRangeVerifier.VerifyRandomRange;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class GenericOreFeature extends Feature<GenericOreFeature.Config> {

	public GenericOreFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public GenericOreFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		Permuter permuter = Permuter.from(context.getRandom());

		double centerX = context.getOrigin().getX() + permuter.nextDouble() - 0.5D;
		double centerY = context.getOrigin().getY() + permuter.nextDouble() - 0.5D;
		double centerZ = context.getOrigin().getZ() + permuter.nextDouble() - 0.5D;

		double radius  = context.getConfig().radius.get(permuter);
		double radius2 = radius * radius;
		double reciprocalRadius2 = 1.0D / radius2;

		int minX = BigGlobeMath. ceilI(centerX - radius);
		int minY = BigGlobeMath. ceilI(centerY - radius);
		int minZ = BigGlobeMath. ceilI(centerZ - radius);
		int maxX = BigGlobeMath.floorI(centerX + radius);
		int maxY = BigGlobeMath.floorI(centerY + radius);
		int maxZ = BigGlobeMath.floorI(centerZ + radius);

		StructureWorldAccess world = context.getWorld();
		Map<BlockState, BlockState> states = context.getConfig().states.runtimeStates;
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (pos.setZ(minZ); pos.getZ() <= maxZ; pos.setZ(pos.getZ() + 1)) {
			double offsetZ2 = BigGlobeMath.squareD(pos.getZ() - centerZ);
			for (pos.setX(minX); pos.getX() <= maxX; pos.setX(pos.getX() + 1)) {
				double offsetXZ2 = offsetZ2 + BigGlobeMath.squareD(pos.getX() - centerX);
				if (!(offsetXZ2 < radius2)) continue;
				for (pos.setY(minY); pos.getY() <= maxY; pos.setY(pos.getY() + 1)) {
					double offsetXYZ2 = offsetXZ2 + BigGlobeMath.squareD(pos.getY() - centerY);
					if (!(offsetXYZ2 < radius2)) continue;
					double chance = BigGlobeMath.squareD(1.0D - offsetXYZ2 * reciprocalRadius2);
					if (Permuter.nextChancedBoolean(permuter, chance)) {
						BlockState replacement = states.get(world.getBlockState(pos));
						if (replacement != null) world.setBlockState(pos, replacement, Block.NOTIFY_ALL);
					}
				}
			}
		}
		return true;
	}

	public static record Config(
		BlockState2ObjectMap<BlockState> states,
		@VerifyRandomRange(min = 0.0D, minInclusive = false, max = 16.0D) RandomSource radius
	)
	implements FeatureConfig {}
}