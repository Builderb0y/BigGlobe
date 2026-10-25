package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

@Mixin(BubbleColumnBlock.class)
public class BubbleColumnBlock_WorkWithSoulMagma {

	@Shadow
	@Final
	public static BooleanProperty DRAG_DOWN;

	@Inject(method = "getColumnState", at = @At("TAIL"), cancellable = true)
	private static void bigglobe_checkForSoulMagma(BlockState state, CallbackInfoReturnable<BlockState> callback) {
		if (state.is(BigGlobeBlocks.SOUl_MAGMA)) {
			callback.setReturnValue(Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(DRAG_DOWN, true));
		}
	}

	@Inject(method = "canSurvive", at = @At("TAIL"), cancellable = true)
	private void bigglobe_checkForSoulMagma(BlockState state, LevelReader world, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
		//for some reason, local capture doesn't seem to want to work correctly.
		//so, we will query world.getBlockState() again instead of re-using the local variable.
		if (world.getBlockState(pos.below()).is(BigGlobeBlocks.SOUl_MAGMA)) {
			callback.setReturnValue(true);
		}
	}
}