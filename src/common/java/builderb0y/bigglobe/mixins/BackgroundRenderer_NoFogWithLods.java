package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;

import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;

@Mixin(value = FogRenderer.class, priority = 500) //before sodium.
public abstract class BackgroundRenderer_NoFogWithLods {

	@Inject(method = "setupFog", at = @At(value = "TAIL"))
	private void bigglobe_disableFogWhenRenderingLods(
		Camera camera,
		int viewDistance,
		DeltaTracker tickCounter,
		float skyDarkness,
		ClientLevel world,
		CallbackInfoReturnable<FogData> callback
	) {
		if (Minecraft.getInstance().levelRenderer instanceof LodSystemHolder holder && holder.bigglobe_getLodSystem() != null) {
			FogData fogData = callback.getReturnValue();
			fogData.renderDistanceStart = fogData.environmentalStart = 30_000_000.0F * 4.0F;
			fogData.renderDistanceEnd = fogData.environmentalEnd = 30_000_000.0F * 8.0F;
		}
	}
}