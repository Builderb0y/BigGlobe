package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.trees.SaplingGrowHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SaplingBlock.class)
public class SaplingBlock_GrowIntoBigGlobeTree {

	@Inject(
		method = "advanceTree",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/grower/TreeGrower;growTree(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Z"
		),
		cancellable = true
	)
	private void bigglobe_generateBigTree(ServerLevel world, BlockPos pos, BlockState state, RandomSource random, CallbackInfo callback) {
		if (SaplingGrowHandler.replaceSaplingGrowth(world, pos, state, random)) {
			callback.cancel();
		}
	}
}