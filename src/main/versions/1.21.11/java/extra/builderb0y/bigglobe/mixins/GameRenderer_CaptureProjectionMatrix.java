package builderb0y.bigglobe.mixins;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.render.GameRenderer;

import builderb0y.bigglobe.versions.RenderVersions;

@Mixin(GameRenderer.class)
public class GameRenderer_CaptureProjectionMatrix {

	@ModifyArg(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RawProjectionMatrix;set(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
	private Matrix4f bigglobe_captureProjectionMatrix(Matrix4f projectionMatrix) {
		RenderVersions.minecraftProjectionMatrix.set(projectionMatrix);
		return projectionMatrix;
	}
}