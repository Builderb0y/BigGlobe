package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

import builderb0y.bigglobe.ClientState;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class MinecraftClient_ResetOverworldSettingsHook {

	@Inject(method = "setWorld", at = @At("RETURN"))
	private void bigglobe_resetOverworldSettings(ClientWorld world, CallbackInfo callback) {
		if (world == null) {
			ClientState.reset();
		}
	}
}