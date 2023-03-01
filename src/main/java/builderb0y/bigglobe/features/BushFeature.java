package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.trees.TreeRegistry;
import builderb0y.bigglobe.util.WorldUtil;

public class BushFeature extends Feature<BushFeature.Config> {

	public BushFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public BushFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos.Mutable mutable = WorldUtil.findNonReplaceableGround(world, context.getOrigin());
		if (mutable == null) return false;
		int originX = mutable.getX();
		int originY = mutable.getY() + 1;
		int originZ = mutable.getZ();
		if (!world.getBlockState(mutable).isIn(BlockTags.DIRT)) return false;
		if (!WorldUtil.isReplaceableNonFluid(world, mutable.setY(originY))) return false;
		WorldColumn column = WorldColumn.forWorld(world, originX, originZ);
		TreeRegistry.Entry palette = context.getConfig().palette();
		world.setBlockState(mutable, palette.getWood(Axis.Y), Block.NOTIFY_ALL);
		Permuter permuter = Permuter.from(context.getRandom());
		double horizontalRadius = permuter.nextDouble(1.0D, 3.0D);
		double   verticalRadius = permuter.nextDouble(1.0D, 2.0D);
		double reciprocalHorizontalRadius = 1.0D / horizontalRadius;
		double   reciprocalVerticalRadius = 1.0D / verticalRadius;
		double centerX = permuter.nextDouble() - 0.5D;
		double centerZ = permuter.nextDouble() - 0.5D;
		double snowChance = ColumnValue.OVERWORLD_SNOW_CHANCE.getValue(column, originY);
		int minX = BigGlobeMath. ceilI(centerX - horizontalRadius);
		int minZ = BigGlobeMath. ceilI(centerZ - horizontalRadius);
		int maxX = BigGlobeMath.floorI(centerX + horizontalRadius);
		int maxZ = BigGlobeMath.floorI(centerZ + horizontalRadius);
		for (int x = minX; x <= maxX; x++) {
			double x2 = BigGlobeMath.squareD((x - centerX) * reciprocalHorizontalRadius);
			mutable.setX(originX + x);
			for (int z = minZ; z <= maxZ; z++) {
				double z2 = x2 + BigGlobeMath.squareD((z - centerZ) * reciprocalHorizontalRadius);
				if (z2 > 1.0D) continue;
				mutable.setZ(originZ + z);

				mutable.setY(originY);
				if (WorldUtil.isReplaceableNonFluid(world, mutable)) {
					world.setBlockState(mutable, palette.getLeaves(Math.abs(x) + Math.abs(z), false), Block.NOTIFY_ALL);
				}

				BlockState existingState = world.getBlockState(mutable.setY(originY + 1));
				if (WorldUtil.isReplaceableNonFluid(existingState)) {
					if (z2 + BigGlobeMath.squareD(reciprocalVerticalRadius) <= 1.0D) {
						world.setBlockState(mutable, palette.getLeaves(Math.abs(x) + Math.abs(z) + 1, false), Block.NOTIFY_ALL);
						existingState = world.getBlockState(mutable.setY(originY + 2));
						if (
							WorldUtil.isReplaceableNonFluid(existingState) &&
							existingState.getBlock() != Blocks.SNOW &&
							Permuter.nextChancedBoolean(permuter, snowChance)
						) {
							world.setBlockState(mutable, BlockStates.SNOW, Block.NOTIFY_ALL);
						}
					}
					else if (
						existingState.getBlock() != Blocks.SNOW &&
						Permuter.nextChancedBoolean(permuter, snowChance)
					) {
						world.setBlockState(mutable, BlockStates.SNOW, Block.NOTIFY_ALL);
					}
				}
			}
		}
		return true;
	}

	public static record Config(TreeRegistry.Entry palette) implements FeatureConfig {}
}