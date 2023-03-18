package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;

@Mixin(BubbleColumnBlock.class)
public class BubbleColumnBlock_WorkWithSoulMagma {

	@Shadow @Final public static BooleanProperty DRAG;

	@Inject(method = "getBubbleState", at = @At("TAIL"), cancellable = true)
	private static void bigglobe_checkForSoulMagma(BlockState state, CallbackInfoReturnable<BlockState> callback) {
		if (state.isOf(BigGlobeBlocks.SOUl_MAGMA)) {
			callback.setReturnValue(Blocks.BUBBLE_COLUMN.getDefaultState().with(DRAG, true));
		}
	}

	@Inject(method = "canPlaceAt", at = @At("TAIL"), cancellable = true)
	private void bigglobe_checkForSoulMagma(BlockState state, WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
		//for some reason, local capture doesn't seem to want to work correctly.
		//so, we will query world.getBlockState() again instead of re-using the local variable.
		if (world.getBlockState(pos.down()).isOf(BigGlobeBlocks.SOUl_MAGMA)) {
			callback.setReturnValue(true);
		}
	}
}