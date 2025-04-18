package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;

import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering;
import builderb0y.bigglobe.hyperspace.VanillaHyperspaceRendering;

@Mixin(WorldRenderer.class)
public abstract class WorldRenderer_RenderHyperspaceSky {

	@Shadow @Final private MinecraftClient client;

	@Shadow @Final private DefaultFramebufferSet framebufferSet;

	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void bigglobe_renderHyperspaceSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo callback) {
		if (this.client.world != null && this.client.world.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
			VanillaHyperspaceRendering.renderHyperspaceSkybox(frameGraphBuilder, tickDelta, this.framebufferSet);
			callback.cancel();
		}
	}
}