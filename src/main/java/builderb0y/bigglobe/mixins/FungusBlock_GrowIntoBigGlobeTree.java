package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.block.FungusBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import builderb0y.bigglobe.trees.SaplingGrowHandler;

@Mixin(FungusBlock.class)
public class FungusBlock_GrowIntoBigGlobeTree {

	@Inject(method = "grow", at = @At("HEAD"), cancellable = true)
	private void bigglobe_generateBigTree(ServerWorld world, Random random, BlockPos pos, BlockState state, CallbackInfo callback) {
		if (SaplingGrowHandler.replaceSaplingGrowth(world, pos, state, random)) {
			callback.cancel();
		}
	}
}