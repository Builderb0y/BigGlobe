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

	@Inject(
		method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void bigglobe_renderHyperspaceSky(
		Matrix4f matrix4f,
		Matrix4f projectionMatrix,
		float tickDelta,
		Camera camera,
		boolean thickFog,
		Runnable fogCallback,
		CallbackInfo callback
	) {
		if (HyperspaceRenderer.INSTANCE != null && this.world.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
			HyperspaceRenderer.INSTANCE.draw();
			callback.cancel();
		}
	}
}