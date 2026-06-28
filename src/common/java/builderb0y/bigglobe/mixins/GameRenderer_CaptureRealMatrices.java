package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

import builderb0y.bigglobe.rendering.CommonState;

@Mixin(GameRenderer.class)
public class GameRenderer_CaptureRealMatrices {

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V", ordinal = 0))
	private void bigglobe_captureRealProjectionMatrices(DeltaTracker deltaTracker, CallbackInfo callback, @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix, @Local(name = "projectionMatrix") Matrix4f projectionMatrix) {
		CommonState.setMatrices(modelViewMatrix, projectionMatrix);
	}
}