package builderb0y.bigglobe.mixins;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.rendering.hyperspace.HyperspaceRenderer;

@Mixin(WorldRenderer.class)
public abstract class WorldRenderer_RenderHyperspaceSky {

	@Shadow private @Nullable ClientWorld world;
	@Shadow @Final private DefaultFramebufferSet framebufferSet;

	@Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getDimensionEffects()Lnet/minecraft/client/render/DimensionEffects;"), cancellable = true)
	private void bigglobe_renderHyperspaceSky(
		FrameGraphBuilder frameGraphBuilder,
		Camera camera,
		float tickProgress,
		Fog fog,
		CallbackInfo callback
	) {
		if (HyperspaceRenderer.INSTANCE != null && this.world.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
			RenderPass framePass = frameGraphBuilder.createPass("bigglobe_hyperspace_sky");
			this.framebufferSet.mainFramebuffer = framePass.transfer(this.framebufferSet.mainFramebuffer); //I have no idea what this does.
			framePass.setRenderer(HyperspaceRenderer.INSTANCE::draw);
			callback.cancel();
		}
	}
}