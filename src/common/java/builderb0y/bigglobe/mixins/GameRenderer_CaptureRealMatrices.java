package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import builderb0y.bigglobe.rendering.CommonState;

@Mixin(GameRenderer.class)
public class GameRenderer_CaptureRealMatrices {

	#if MC_VERSION >= MC_26_3_0

		@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V"))
		private void bigglobe_captureMatrices(
			CallbackInfo callback,
			@Local(name = "cameraState") CameraRenderState cameraState,
			@Local(name = "projectionMatrix") Matrix4f projectionMatrix
		) {
			CommonState.setMatrices(cameraState.viewRotationMatrix, projectionMatrix);
		}

	#else

		@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V", ordinal = 0))
		private void bigglobe_captureRealProjectionMatrices(DeltaTracker deltaTracker, CallbackInfo callback, @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix, @Local(name = "projectionMatrix") Matrix4f projectionMatrix) {
			CommonState.setMatrices(modelViewMatrix, projectionMatrix);
		}

	#endif
}