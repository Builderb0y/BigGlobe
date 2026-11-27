package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.versions.BlockStateVersions;

#if MC_VERSION >= MC_26_3_0
	import net.minecraft.world.level.block.BonemealSource;
#endif

public abstract class ChorusSporeBlock extends VegetationBlock implements BonemealableBlock {

	#if MC_VERSION < MC_26_3_0

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public abstract MapCodec codec();

	#endif

	public final Holder<Block> grow_into;

	public ChorusSporeBlock(Properties settings, Holder<Block> grow_into) {
		super(settings);
		this.grow_into = grow_into;
	}

	@Override
	@Deprecated
	public abstract VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context);

	@Override
	public boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
		return BlockStateVersions.isOpaqueFullCube(floor, world, pos);
	}

	@Override
	public boolean isValidBonemealTarget(
		LevelReader world,
		BlockPos pos,
		BlockState state
		#if MC_VERSION >= MC_26_3_0 , BonemealSource source #endif
	) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(
		Level world,
		RandomSource random,
		BlockPos pos,
		BlockState state
		#if MC_VERSION >= MC_26_3_0 , BonemealSource source #endif
	) {
		return true;
	}

	@Override
	public void performBonemeal(
		ServerLevel world,
		RandomSource random,
		BlockPos pos,
		BlockState state
		#if MC_VERSION >= MC_26_3_0 , BonemealSource source #endif
	) {
		SingleBlockFeature.place(world, pos, this.grow_into.value().defaultBlockState(), SingleBlockFeature.IS_REPLACEABLE);
	}
}