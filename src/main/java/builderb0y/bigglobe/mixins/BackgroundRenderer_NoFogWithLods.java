package builderb0y.bigglobe.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering.lods.LodSystem;

@Mixin(FogRenderer.class)
public abstract class BackgroundRenderer_NoFogWithLods {

	@Shadow private static float fogRed;
	@Shadow private static float fogGreen;
	@Shadow private static float fogBlue;

	@Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_disableFogWhenRenderingLODs(
		Camera camera,
		FogRenderer.FogMode fogType,
		float viewDistance,
		boolean thickFog,
		float tickDelta,
		CallbackInfo callback
	) {
		LodSystem system = LodSystemHolder.of(Minecraft.getInstance().levelRenderer).bigglobe_getLodSystem();
		if (fogType == FogRenderer.FogMode.FOG_TERRAIN && system != null && system.renderingThisFrame) {
			system.renderState.fogR = fogRed;
			system.renderState.fogG = fogGreen;
			system.renderState.fogB = fogBlue;
			RenderSystem.setShaderFogStart(30_000_000.0F * 4.0F);
			RenderSystem.setShaderFogEnd(30_000_000.0F * 8.0F);
			callback.cancel();
		}
	}
}
