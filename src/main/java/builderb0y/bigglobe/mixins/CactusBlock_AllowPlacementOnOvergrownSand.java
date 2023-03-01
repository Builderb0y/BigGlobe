package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;

@Mixin(CactusBlock.class)
public class CactusBlock_AllowPlacementOnOvergrownSand {

	@Inject(
		method = "canPlaceAt",
		at = @At(
			value = "INVOKE_ASSIGN",
			target = "net/minecraft/world/WorldView.getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
			ordinal = 1
		),
		cancellable = true,
		locals = LocalCapture.CAPTURE_FAILSOFT
	)
	private void placeOnOvergrownSand(BlockState state, WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> callback, BlockState downState) {
		//I hope mojang some day replaces this logic with BlockTags.SAND
		//instead of explicitly checking for Blocks.SAND and Blocks.RED_SAND.
		if (downState.getBlock() == BigGlobeBlocks.OVERGROWN_SAND) {
			callback.setReturnValue(!world.getBlockState(pos.up()).getMaterial().isLiquid()); //match vanilla logic.
		}
	}
}