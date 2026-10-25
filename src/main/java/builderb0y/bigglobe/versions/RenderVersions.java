package builderb0y.bigglobe.versions;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(EnvType.CLIENT)
public class RenderVersions {

	public static final ThreadLocal<Matrix4f> minecraftProjectionMatrix = ThreadLocal.withInitial(Matrix4f::new);

	public static int glID(RenderTarget framebuffer) {
		return framebuffer.frameBufferId;
	}

	public static int colorAttachment(RenderTarget framebuffer) {
		return framebuffer.getColorTextureId();
	}

	public static int depthAttachment(RenderTarget framebuffer) {
		return framebuffer.getDepthTextureId();
	}

	public static int glID(AbstractTexture texture) {
		return texture.getId();
	}

	public static int glID(LightTexture manager) {
		return manager.lightTexture.getId();
	}

	public static Matrix4fc modelViewMatrix(WorldRenderContext context) {
		return context.positionMatrix();
	}

	public static Matrix4fc projectionMatrix(WorldRenderContext context) {
		return context.projectionMatrix();
	}

	public static float partialTicks(WorldRenderContext context) {
		return context.tickDelta();
	}

	public static boolean isTranslucent(RenderType layer) {
		return layer == RenderType.translucent() || layer == RenderType.tripwire();
	}

	public static Vec3 getCameraPosition(Camera camera) {
		return camera.getPosition();
	}
}
