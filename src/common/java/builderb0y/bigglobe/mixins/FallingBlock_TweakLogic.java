package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.mechanics.FallingBlockLogicTweaks;

@Mixin(FallingBlock.class)
public class FallingBlock_TweakLogic {

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FallingBlock;isFree(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean bigglobe_tweakIsFree(boolean original, @Local(argsOnly = true) ServerLevel world, @Local(argsOnly = true) BlockPos pos) {
		if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.alt_block_falling_logic()) {
			return original && FallingBlockLogicTweaks.canFall(world, pos);
		}
		return original;
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;fall(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/entity/item/FallingBlockEntity;"), cancellable = true)
	private void bigglobe_tweakFall(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo callback) {
		if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.alt_block_falling_logic()) {
			FallingBlockLogicTweaks.fall(world, pos);
			callback.cancel();
		}
	}
}