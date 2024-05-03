package builderb0y.bigglobe.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;

import builderb0y.bigglobe.util.ClientWorldEvents;

@Mixin(MinecraftClient.class)
public class MinecraftClient_SetWorldEvent {

	@Shadow @Nullable public ClientWorld world;

	@Inject(method = "joinWorld", at = @At("HEAD"))
	private void bigglobe_unloadOnJoinWorld(ClientWorld world, #if MC_VERSION >= MC_1_20_5 net.minecraft.client.gui.screen.DownloadingTerrainScreen.WorldEntryReason reason, #endif CallbackInfo callback) {
		ClientWorldEvents.WORLD_CHANGED.invoker().worldChanged(this.world, world);
	}

	@Inject(method = { "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V" #if MC_VERSION >= MC_1_20_2 , "enterReconfiguration" #endif }, at = @At("HEAD"))
	private void bigglobe_unloadOnDisconnect(Screen screen, CallbackInfo callback) {
		if (this.world != null) {
			ClientWorldEvents.WORLD_CHANGED.invoker().worldChanged(this.world, null);
		}
	}
}