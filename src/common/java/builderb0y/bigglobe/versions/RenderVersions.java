package builderb0y.bigglobe.versions;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(EnvType.CLIENT)
public class RenderVersions {

	//frapi doesn't provide this for some reason,
	//so it's extracted via mixin instead.
	public static final ThreadLocal<Matrix4f> minecraftProjectionMatrix = ThreadLocal.withInitial(Matrix4f::new);

	public static int glID(RenderTarget framebuffer) {

		return (
			(
				(GlTexture)(
					framebuffer.getColorTexture()
				)
			)
				.getFbo(
					(
						(GlDevice)(
							RenderSystem.getDevice()
						)
					)

						.directStateAccess(),

					framebuffer.getDepthTexture()
				)
		);
	}

	public static int colorAttachment(RenderTarget framebuffer) {

		return ((GlTexture)(framebuffer.getColorTexture())).glId();
	}

	public static int depthAttachment(RenderTarget framebuffer) {

		return ((GlTexture)(framebuffer.getDepthTexture())).glId();
	}

	public static int glID(AbstractTexture texture) {

		return ((GlTexture)(texture.getTexture())).glId();
	}

	public static int glID(LightTexture manager) {

		return ((GlTexture)(manager.texture)).glId();
	}

	public static Matrix4fc modelViewMatrix(

		WorldExtractionContext context

	) {

		return context.viewMatrix();
	}

	public static Matrix4fc projectionMatrix(

		WorldExtractionContext context

	) {

		return minecraftProjectionMatrix.get();
	}

	public static float partialTicks(

		WorldExtractionContext context

	) {

		return context.tickCounter().getGameTimeDeltaPartialTick(false);
	}

	public static boolean isTranslucent(RenderType layer) {

		return layer.sortOnUpload();
	}

	public static Vec3 getCameraPosition(Camera camera) {

		return camera.position();
	}
}