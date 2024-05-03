package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(value = World.class, priority = 2000)
public abstract class World_UseCorrectSeaLevel implements WorldAccess {

	@Inject(method = "getSeaLevel", at = @At("HEAD"), cancellable = true)
	private void bigglobe_useCorrectSeaLevel(CallbackInfoReturnable<Integer> callback) {
		if (this.getChunkManager() instanceof ServerChunkManager serverChunkManager && serverChunkManager.getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			callback.setReturnValue(generator.getSeaLevel() - 1);
		}
	}
}