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

#if MC_VERSION >= MC_1_21_6
	import com.mojang.blaze3d.buffers.GpuBufferSlice;
#endif

@Mixin(WorldRenderer.class)
public abstract class WorldRenderer_RenderHyperspaceSky {

	@Shadow private @Nullable ClientWorld world;

	#if MC_VERSION >= MC_1_21_3

		@Shadow @Final private DefaultFramebufferSet framebufferSet;

		@Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getDimensionEffects()Lnet/minecraft/client/render/DimensionEffects;"), cancellable = true)
		private void bigglobe_renderHyperspaceSky(
			FrameGraphBuilder frameGraphBuilder,
			Camera camera,
			float tickProgress,
			#if MC_VERSION >= MC_1_21_6
				GpuBufferSlice fog,
			#else
				Fog fog,
			#endif
			CallbackInfo callback
		) {
			if (HyperspaceRenderer.INSTANCE != null && this.world.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
				#if MC_VERSION >= MC_1_21_5
					FramePass
				#else
					RenderPass
				#endif
					framePass = frameGraphBuilder.createPass("bigglobe_hyperspace_sky");
				this.framebufferSet.mainFramebuffer = framePass.transfer(this.framebufferSet.mainFramebuffer); //I have no idea what this does.
				framePass.setRenderer(HyperspaceRenderer.INSTANCE::draw);
				callback.cancel();
			}
		}

	#else

		@Inject(
			#if MC_VERSION >= MC_1_20_5
				method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
			#else
				method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
			#endif
			at = @At("HEAD"),
			cancellable = true
		)
		private void bigglobe_renderHyperspaceSky(
			#if MC_VERSION >= MC_1_20_5
				Matrix4f matrix4f,
				Matrix4f projectionMatrix,
				float tickDelta,
				Camera camera,
				boolean thickFog,
				Runnable fogCallback,
				CallbackInfo callback
			#else
				MatrixStack matrices,
				Matrix4f projectionMatrix,
				float tickDelta,
				Camera camera,
				boolean thickFog,
				Runnable fogCallback,
				CallbackInfo callback
			#endif
		) {
			if (HyperspaceRenderer.INSTANCE != null && this.world.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
				HyperspaceRenderer.INSTANCE.draw();
				callback.cancel();
			}
		}

	#endif
}