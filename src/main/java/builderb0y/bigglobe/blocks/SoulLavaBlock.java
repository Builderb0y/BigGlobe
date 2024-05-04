package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.UseSuperClass;

@AddPseudoField("fluid")
public class SoulLavaBlock extends FluidBlock {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<SoulLavaBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(SoulLavaBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public SoulLavaBlock(FlowableFluid fluid, Settings settings) {
		super(fluid, settings);
	}

	@UseSuperClass(Fluid.class)
	public FlowableFluid fluid() {
		return this.fluid;
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
		if (this.checkNeighborFluids(world, pos, state)) {
			world.scheduleFluidTick(pos, state.getFluidState().getFluid(), this.fluid.getTickRate(world));
		}
	}

	/**
	copy-pasted from {@link FluidBlock#receiveNeighborFluids(World, BlockPos, BlockState)}.
	the only difference is that we produce crying obsidian instead of regular obsidian.
	*/
	public boolean checkNeighborFluids(World world, BlockPos pos, BlockState state) {
		//if (this.fluid.isIn(BigGlobeFluidTags.SOUL_LAVA)) {
		boolean bl = world.getBlockState(pos.down()).isOf(Blocks.SOUL_SOIL);
		for (Direction direction : FLOW_DIRECTIONS) {
			BlockPos blockPos = pos.offset(direction.getOpposite());
			if (world.getFluidState(blockPos).isIn(FluidTags.WATER)) {
				Block block = world.getFluidState(pos).isStill() ? Blocks.CRYING_OBSIDIAN : Blocks.COBBLESTONE;
				world.setBlockState(pos, block.getDefaultState());
				world.syncWorldEvent(WorldEvents.LAVA_EXTINGUISHED, pos, 0);
				return false;
			}
			if (!bl || !world.getBlockState(blockPos).isOf(Blocks.BLUE_ICE)) continue;
			world.setBlockState(pos, Blocks.BASALT.getDefaultState());
			world.syncWorldEvent(WorldEvents.LAVA_EXTINGUISHED, pos, 0);
			return false;
		}
		//}
		return true;
	}

	@Override
	public boolean canPathfindThrough(BlockState state, #if MC_VERSION < MC_1_20_5 BlockView world, BlockPos pos, #endif NavigationType type) {
		return false;
	}
}