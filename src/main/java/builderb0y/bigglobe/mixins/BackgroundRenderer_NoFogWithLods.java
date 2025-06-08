package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
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
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRenderer_NoFogWithLods {

	#if MC_VERSION >= MC_1_21_2

		@ModifyExpressionValue(method = "applyFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/BackgroundRenderer;fogEnabled:Z"))
		private static boolean bigglobe_disableFogWhenRenderingLods(boolean fogEnabled, @Local(argsOnly = true) FogType type, @Local(argsOnly = true) Camera camera, @Local(argsOnly = true) Vector4f fogColor) {
			LodSystem system = ((LodSystemHolder)(MinecraftClient.getInstance().worldRenderer)).bigglobe_getLodSystem();
			if (type == FogType.FOG_TERRAIN && system != null && !MinecraftClient.getInstance().worldRenderer.hasBlindnessOrDarkness(camera)) {
				system.fog.red   = fogColor.x;
				system.fog.green = fogColor.y;
				system.fog.blue  = fogColor.z;
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
			LodSystem system = ((LodSystemHolder)(MinecraftClient.getInstance().worldRenderer)).bigglobe_getLodSystem();
			if (fogType == FogType.FOG_TERRAIN && system != null) {
				system.fog.red   = red;
				system.fog.green = green;
				system.fog.blue  = blue;
				//clearFog() works by setting the fog start to Float.MAX_VALUE,
				//while leaving the fog end unchanged.
				//this basically inverts the range where fog exists,
				//so that it exists LESS THAN Float.MAX_VALUE blocks away from the player.
				//which is to say, it exists everywhere.
				//and this problem is visible with create, but for some reason, it's not visible in vanilla.
				//nevertheless, setting the fog start and end to large numbers without inverting them fixes create.
				RenderSystem.setShaderFogStart(30_000_000.0F * 4.0F);
				RenderSystem.setShaderFogEnd(30_000_000.0F * 8.0F);
				callback.cancel();
			}
		}

	#endif
}