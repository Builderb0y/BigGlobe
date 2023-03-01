package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import builderb0y.bigglobe.trees.SaplingGrowHandler;

@Mixin(SaplingBlock.class)
public class SaplingBlock_GrowIntoBigGlobeTree {

	@Inject(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/sapling/SaplingGenerator;generate(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/random/Random;)Z"), cancellable = true)
	private void bigglobe_generateBigTree(ServerWorld world, BlockPos pos, BlockState state, Random random, CallbackInfo callback) {
		if (SaplingGrowHandler.replaceSaplingGrowth(world, pos, state, random)) {
			callback.cancel();
		}
	}
}