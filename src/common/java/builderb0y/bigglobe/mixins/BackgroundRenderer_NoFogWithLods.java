package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering.lods.LodSystem;

@Mixin(FogRenderer.class)

public abstract class BackgroundRenderer_NoFogWithLods {

	@Inject(method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getDevice()Lcom/mojang/blaze3d/systems/GpuDevice;", remap = false))

	private void bigglobe_disableFogWhenRenderingLods(
		Camera camera,
		int viewDistance,

		DeltaTracker tickCounter,
		float skyDarkness,
		ClientLevel world,
		CallbackInfoReturnable<Vector4f> callback,
		@Local FogData fogData
	) {
		if (Minecraft.getInstance().levelRenderer instanceof LodSystemHolder holder && holder.bigglobe_getLodSystem() != null) {
			fogData.renderDistanceStart = fogData.environmentalStart = 30_000_000.0F * 4.0F;
			fogData.renderDistanceEnd = fogData.environmentalEnd = 30_000_000.0F * 8.0F;
		}
	}

	@ModifyReturnValue(method = "computeFogColor", at = @At(value = "RETURN"))
	private Vector4f bigglobe_captureFogColor(Vector4f original) {
		if (Minecraft.getInstance() != null && Minecraft.getInstance().levelRenderer != null) {
			LodSystem system = LodSystemHolder.of(Minecraft.getInstance().levelRenderer).bigglobe_getLodSystem();
			if (system != null) {
				system.renderState.fogR = original.x;
				system.renderState.fogG = original.y;
				system.renderState.fogB = original.z;
			}
		}
		return original;
	}
}