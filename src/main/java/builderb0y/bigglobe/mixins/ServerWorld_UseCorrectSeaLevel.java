package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(value = ServerWorld.class, priority = 2000)
public abstract class ServerWorld_UseCorrectSeaLevel {

	@Shadow public abstract ServerChunkManager getChunkManager();

	@Dynamic("method added by a different mixin.")
	@Inject(method = "getSeaLevel", at = @At("HEAD"), cancellable = true)
	private void bigglobe_useCorrectSeaLevel(CallbackInfoReturnable<Integer> callback) {
		if (this.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			callback.setReturnValue(generator.getSeaLevel() - 1);
		}
	}
}