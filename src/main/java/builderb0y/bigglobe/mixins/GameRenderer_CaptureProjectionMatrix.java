package builderb0y.bigglobe.mixins;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.versions.RenderVersions;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class GameRenderer_CaptureProjectionMatrix {

	@Inject(method = "resetProjectionMatrix", at = @At("HEAD"))
	private void bigglobe_captureProjectionMatrix(Matrix4f projectionMatrix, CallbackInfo callback) {
		RenderVersions.minecraftProjectionMatrix.set(projectionMatrix);
	}
}
