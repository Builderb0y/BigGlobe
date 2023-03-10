package builderb0y.bigglobe.fluids;

import java.util.Optional;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldView;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.items.BigGlobeItems;

public abstract class SoulLavaFluid extends FlowableFluid {

	@Override
	public Fluid getFlowing() {
		return BigGlobeFluids.FLOWING_SOUL_LAVA;
	}

	@Override
	public Fluid getStill() {
		return BigGlobeFluids.SOUL_LAVA;
	}

	@Override
	public boolean isInfinite() {
		return false;
	}

	@Override
	public void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
		world.syncWorldEvent(WorldEvents.LAVA_EXTINGUISHED, pos, 0);
	}

	@Override
	public int getFlowSpeed(WorldView world) {
		return world.getDimension().ultrawarm() ? 4 : 2;
	}

	@Override
	public int getLevelDecreasePerBlock(WorldView world) {
		return world.getDimension().ultrawarm() ? 1 : 2;
	}

	@Override
	public Item getBucketItem() {
		return BigGlobeItems.SOUL_LAVA_BUCKET;
	}

	@Override
	public boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
		return state.getHeight(world, pos) >= 0.44444445F && fluid.isIn(FluidTags.WATER);
	}

	@Override
	public int getTickRate(WorldView world) {
		return world.getDimension().ultrawarm() ? 10 : 30;
	}

	@Override
	public float getBlastResistance() {
		return 100.0F;
	}

	@Override
	public BlockState toBlockState(FluidState state) {
		return BigGlobeBlocks.SOUL_LAVA.getDefaultState().with(FluidBlock.LEVEL, getBlockStateLevel(state));
	}

	@Override
	public boolean matchesType(Fluid fluid) {
		return fluid == BigGlobeFluids.SOUL_LAVA || fluid == BigGlobeFluids.FLOWING_SOUL_LAVA;
	}

	@Override
	public Optional<SoundEvent> getBucketFillSound() {
		return Optional.of(SoundEvents.ITEM_BUCKET_FILL_LAVA);
	}

	public static class Flowing extends SoulLavaFluid {

		@Override
		protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
			super.appendProperties(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getLevel(FluidState state) {
			return state.get(LEVEL);
		}

		@Override
		public boolean isStill(FluidState state) {
			return false;
		}
	}

	public static class Still extends SoulLavaFluid {

		@Override
		public int getLevel(FluidState state) {
			return 8;
		}

		@Override
		public boolean isStill(FluidState state) {
			return true;
		}
	}
}