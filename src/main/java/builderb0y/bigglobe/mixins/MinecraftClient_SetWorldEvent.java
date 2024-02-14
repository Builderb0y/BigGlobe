package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

import builderb0y.bigglobe.util.ClientWorldEvents;

@Mixin(MinecraftClient.class)
public class MinecraftClient_SetWorldEvent {

	@Inject(method = "setWorld", at = @At("HEAD"))
	private void bigglobe_setWorldEvent(ClientWorld world, CallbackInfo callback) {
		if (world != null) {
			ClientWorldEvents.LOAD.invoker().load(world);
		}
		else {
			ClientWorldEvents.UNLOAD.invoker().unload();
		}
	}
}