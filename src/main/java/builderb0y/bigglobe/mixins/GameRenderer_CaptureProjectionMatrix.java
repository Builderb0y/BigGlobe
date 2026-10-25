package builderb0y.bigglobe.mixins;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import builderb0y.bigglobe.versions.RenderVersions;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class GameRenderer_CaptureProjectionMatrix {

	@ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PerspectiveProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
	private Matrix4f bigglobe_captureProjectionMatrix(Matrix4f projectionMatrix) {
		RenderVersions.minecraftProjectionMatrix.set(projectionMatrix);
		return projectionMatrix;
	}
}