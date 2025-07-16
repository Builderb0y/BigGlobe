package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;

import builderb0y.bigglobe.rendering.SoulLavaFogHandler;

@Environment(EnvType.CLIENT)
#if MC_VERSION >= MC_1_21_6
	@Mixin(net.minecraft.client.render.fog.LavaFogModifier.class)
#else
	@Mixin(net.minecraft.client.render.BackgroundRenderer.class)
#endif
public class BackgroundRenderer_SoulLavaFogColor {

	#if MC_VERSION >= MC_1_21_6

		@ModifyReturnValue(method = "getFogColor", at = @At("RETURN"))
		private int bigglobe_useBlueFogColorInSoulLava(int original) {
			if (SoulLavaFogHandler.inSoulLava) {
				return (original & 0xFF00FF00) | (Integer.rotateLeft(original, 16) & 0x00FF00FF);
			}
			return original;
		}

	#elif MC_VERSION >= MC_1_21_2

		@ModifyReturnValue(method = "getFogColor", at = @At("TAIL"))
		private static Vector4f bigglobe_useBlueFogColorInSoulLava(Vector4f color) {
			if (SoulLavaFogHandler.inSoulLava) {
				float tmp = color.x;
				color.x = color.z;
				color.z = tmp;
			}
			return color;
		}

	#else

		@Shadow private static float red;
		@Shadow private static float blue;

		@Inject(method = "render", at = @At(value = "INVOKE", target = "net/minecraft/client/render/BackgroundRenderer.getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;"))
		private static void bigglobe_useBlueFogColorInSoulLava(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo callback) {
			if (SoulLavaFogHandler.inSoulLava) {
				float tmp = red;
				red = blue;
				blue = tmp;
			}
		}

	#endif
}