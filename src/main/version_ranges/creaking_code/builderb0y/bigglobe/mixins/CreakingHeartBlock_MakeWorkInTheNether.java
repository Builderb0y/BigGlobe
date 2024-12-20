package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.CreakingHeartBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(CreakingHeartBlock.class)
public class CreakingHeartBlock_MakeWorkInTheNether {

	@Inject(method = "isNightAndNatural", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_makeWorkInTheNether(World world, CallbackInfoReturnable<Boolean> callback) {
		ServerWorld replacement;
		if (
			world instanceof ServerWorld serverWorld &&
			serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.creaking_overrides != null &&
			(replacement = serverWorld.getServer().getWorld(generator.creaking_overrides.time_reference())) != null
		) {
			callback.setReturnValue(replacement.isNight());
		}
	}
}