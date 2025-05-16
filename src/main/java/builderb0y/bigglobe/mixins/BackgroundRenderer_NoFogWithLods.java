package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.render.Camera;

import builderb0y.bigglobe.lods.LodSystem;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRenderer_NoFogWithLods {

	#if MC_VERSION >= MC_1_21_2

		@ModifyExpressionValue(method = "applyFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/BackgroundRenderer;fogEnabled:Z"))
		private static boolean bigglobe_disableFogWhenRenderingLods(boolean fogEnabled, @Local(argsOnly = true) FogType type, @Local(argsOnly = true) Camera camera, @Local(argsOnly = true) Vector4f fogColor) {
			if (type == FogType.FOG_TERRAIN && LodSystem.INSTANCE != null && !MinecraftClient.getInstance().worldRenderer.hasBlindnessOrDarkness(camera)) {
				LodSystem.INSTANCE.fog.set(fogColor);
				return false;
			}
			return fogEnabled;
		}

	#else

		@Shadow private static float red;
		@Shadow private static float green;
		@Shadow private static float blue;

		@Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
		private static void bigglobe_disableFogWhenRenderingLODs(
			Camera camera,
			FogType fogType,
			float viewDistance,
			boolean thickFog,
			float tickDelta,
			CallbackInfo callback
		) {
			if (fogType == FogType.FOG_TERRAIN && LodSystem.INSTANCE != null) {
				LodSystem.INSTANCE.fog.set(red, green, blue, 1.0F);
				BackgroundRenderer.clearFog();
				callback.cancel();
			}
		}

	#endif
}