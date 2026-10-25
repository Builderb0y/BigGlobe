package builderb0y.bigglobe.mixins;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.rendering.SoulLavaFogHandler;

@Environment(EnvType.CLIENT)
@Mixin(FogRenderer.class)
public class BackgroundRenderer_SoulLavaFogColor {

	@Shadow private static float fogRed;
	@Shadow private static float fogBlue;

	@Inject(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;getPriorityFogFunction(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/FogRenderer$MobEffectFogFunction;"))
	private static void bigglobe_useBlueFogColorInSoulLava(Camera camera, float tickDelta, ClientLevel world, int viewDistance, float skyDarkness, CallbackInfo callback) {
		if (SoulLavaFogHandler.inSoulLava) {
			float tmp = fogRed;
			fogRed = fogBlue;
			fogBlue = tmp;
		}
	}
}
