package builderb0y.bigglobe.mixins;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;

import builderb0y.bigglobe.rendering.lods.LodSystem;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;

#if MC_VERSION >= MC_1_21_6
	@Mixin(net.minecraft.client.render.fog.FogRenderer.class)
#else
	@Mixin(net.minecraft.client.render.BackgroundRenderer.class)
#endif
public abstract class BackgroundRenderer_NoFogWithLods {

	#if MC_VERSION >= MC_1_21_6

		#if MC_VERSION >= MC_1_21_11
			@Inject(method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getDevice()Lcom/mojang/blaze3d/systems/GpuDevice;", remap = false))
		#else
			@Inject(method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getDevice()Lcom/mojang/blaze3d/systems/GpuDevice;", remap = false))
		#endif
		private void bigglobe_disableFogWhenRenderingLods(
			Camera camera,
			int viewDistance,
			#if MC_VERSION < MC_1_21_11
				boolean thick,
			#endif
			RenderTickCounter tickCounter,
			float skyDarkness,
			ClientWorld world,
			CallbackInfoReturnable<Vector4f> callback,
			@Local net.minecraft.client.render.fog.FogData fogData
		) {
			if (MinecraftClient.getInstance().worldRenderer instanceof LodSystemHolder holder && holder.bigglobe_getLodSystem() != null) {
				fogData.renderDistanceStart = fogData.environmentalStart = 30_000_000.0F * 4.0F;
				fogData.renderDistanceEnd = fogData.environmentalEnd = 30_000_000.0F * 8.0F;
			}
		}

		@ModifyReturnValue(method = "getFogColor", at = @At(value = "RETURN"))
		private Vector4f bigglobe_captureFogColor(Vector4f original) {
			if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().worldRenderer != null) {
				LodSystem system = LodSystemHolder.of(MinecraftClient.getInstance().worldRenderer).bigglobe_getLodSystem();
				if (system != null) {
					system.renderState.fogR = original.x;
					system.renderState.fogG = original.y;
					system.renderState.fogB = original.z;
				}
			}
			return original;
		}

	#elif MC_VERSION >= MC_1_21_2

		@ModifyExpressionValue(method = "applyFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/BackgroundRenderer;fogEnabled:Z"))
		private static boolean bigglobe_disableFogWhenRenderingLods(
			boolean fogEnabled,
			@Local(argsOnly = true) net.minecraft.client.render.BackgroundRenderer.FogType type,
			@Local(argsOnly = true) Camera camera,
			@Local(argsOnly = true) Vector4f fogColor
		) {
			LodSystem system = LodSystemHolder.of(MinecraftClient.getInstance().worldRenderer).bigglobe_getLodSystem();
			if (type == net.minecraft.client.render.BackgroundRenderer.FogType.FOG_TERRAIN && system != null && !MinecraftClient.getInstance().worldRenderer.hasBlindnessOrDarkness(camera)) {
				system.renderState.fogR = fogColor.x;
				system.renderState.fogG = fogColor.y;
				system.renderState.fogB = fogColor.z;
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
			net.minecraft.client.render.BackgroundRenderer.FogType fogType,
			float viewDistance,
			boolean thickFog,
			float tickDelta,
			CallbackInfo callback
		) {
			LodSystem system = LodSystemHolder.of(MinecraftClient.getInstance().worldRenderer).bigglobe_getLodSystem();
			if (fogType == net.minecraft.client.render.BackgroundRenderer.FogType.FOG_TERRAIN && system != null) {
				system.renderState.fogR = red;
				system.renderState.fogG = green;
				system.renderState.fogB = blue;
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