package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.trees.SaplingGrowHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(FungusBlock.class)
public class FungusBlock_GrowIntoBigGlobeTree {

	@Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
	private void bigglobe_generateBigTree(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, CallbackInfo callback) {
		if (SaplingGrowHandler.replaceSaplingGrowth(world, pos, state, random)) {
			callback.cancel();
		}
	}
}