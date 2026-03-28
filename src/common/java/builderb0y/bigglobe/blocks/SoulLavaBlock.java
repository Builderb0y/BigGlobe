package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

@AddPseudoField("fluid")
public class SoulLavaBlock extends LiquidBlock {

	public static final MapCodec<SoulLavaBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(SoulLavaBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public SoulLavaBlock(Holder<Fluid> fluid, Properties settings) {
		super((FlowingFluid)(fluid.value()), settings);
	}

	@SuppressWarnings("deprecation")
	public Holder<Fluid> fluid() {
		return this.fluid.builtInRegistryHolder();
	}

	@Override
	public void neighborChanged(
		BlockState state,
		Level world,
		BlockPos pos,
		Block sourceBlock,

		@Nullable Orientation wireOrientation,

		boolean moved
	) {
		if (this.checkNeighborFluids(world, pos, state)) {
			world.scheduleTick(pos, state.getFluidState().getType(), this.fluid.getTickDelay(world));
		}
	}

	/**
	copy-pasted from {@link LiquidBlock#shouldSpreadLiquid(Level, BlockPos, BlockState)}.
	the only difference is that we produce crying obsidian instead of regular obsidian.
	*/
	public boolean checkNeighborFluids(Level world, BlockPos pos, BlockState state) {
		//if (this.fluid.isIn(BigGlobeFluidTags.SOUL_LAVA)) {
		boolean bl = world.getBlockState(pos.below()).is(Blocks.SOUL_SOIL);
		for (Direction direction : POSSIBLE_FLOW_DIRECTIONS) {
			BlockPos blockPos = pos.relative(direction.getOpposite());
			if (world.getFluidState(blockPos).is(FluidTags.WATER)) {
				Block block = world.getFluidState(pos).isSource() ? Blocks.CRYING_OBSIDIAN : Blocks.COBBLESTONE;
				world.setBlockAndUpdate(pos, block.defaultBlockState());
				world.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
				return false;
			}
			if (!bl || !world.getBlockState(blockPos).is(Blocks.BLUE_ICE)) continue;
			world.setBlockAndUpdate(pos, Blocks.BASALT.defaultBlockState());
			world.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
			return false;
		}
		//}
		return true;
	}

	@Override
	public boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}