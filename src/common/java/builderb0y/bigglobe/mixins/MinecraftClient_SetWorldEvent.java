package builderb0y.bigglobe.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;

import builderb0y.bigglobe.util.ClientWorldEvents;

@Mixin(Minecraft.class)
public class MinecraftClient_SetWorldEvent {

	@Shadow
	@Nullable
	public ClientLevel level;

	@Inject(method = "setLevel", at = @At("HEAD"))
	private void bigglobe_unloadOnJoinWorld(ClientLevel world, CallbackInfo callback) {
		ClientWorldEvents.WORLD_CHANGED.invoker().worldChanged(this.level, world);
	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
	private void bigglobe_unloadOnDisconnect(Screen screen, boolean transferring, CallbackInfo callback) {
		if (this.level != null) {
			ClientWorldEvents.WORLD_CHANGED.invoker().worldChanged(this.level, null);
		}
	}

	@Inject(method = "clearClientLevel", at = @At("HEAD"))
	private void bigglobe_unloadOnReconfiguration(Screen screen, CallbackInfo callback) {
		if (this.level != null) {
			ClientWorldEvents.WORLD_CHANGED.invoker().worldChanged(this.level, null);
		}
	}
}