package builderb0y.bigglobe.versions;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.AbstractTexture;

@Environment(EnvType.CLIENT)
public class RenderVersions {

	//frapi doesn't provide this for some reason,
	//so it's extracted via mixin instead.
	public static final ThreadLocal<Matrix4f> minecraftProjectionMatrix = ThreadLocal.withInitial(Matrix4f::new);

	public static int glID(Framebuffer framebuffer) {
		#if MC_VERSION >= MC_1_21_5
			return (
				(
					(net.minecraft.client.texture.GlTexture)(
						framebuffer.getColorAttachment()
					)
				)
				.getOrCreateFramebuffer(
					(
						(net.minecraft.client.gl.GlBackend)(
							RenderSystem.getDevice()
						)
					)
					#if MC_VERSION >= MC_1_21_6
						.getBufferManager(),
					#else
						.getFramebufferManager(),
					#endif
					framebuffer.getDepthAttachment()
				)
			);
		#else
			return framebuffer.fbo;
		#endif
	}

	public static int colorAttachment(Framebuffer framebuffer) {
		#if MC_VERSION >= MC_1_21_5
			return ((net.minecraft.client.texture.GlTexture)(framebuffer.getColorAttachment())).getGlId();
		#else
			return framebuffer.getColorAttachment();
		#endif
	}

	public static int depthAttachment(Framebuffer framebuffer) {
		#if MC_VERSION >= MC_1_21_5
			return ((net.minecraft.client.texture.GlTexture)(framebuffer.getDepthAttachment())).getGlId();
		#else
			return framebuffer.getDepthAttachment();
		#endif
	}

	public static int glID(AbstractTexture texture) {
		#if MC_VERSION >= MC_1_21_5
			return ((net.minecraft.client.texture.GlTexture)(texture.getGlTexture())).getGlId();
		#else
			return texture.getGlId();
		#endif
	}

	public static int glID(LightmapTextureManager manager) {
		#if MC_VERSION >= MC_1_21_6
			return ((net.minecraft.client.texture.GlTexture)(manager.glTexture)).getGlId();
		#elif MC_VERSION >= MC_1_21_5
			return ((net.minecraft.client.texture.GlTexture)(manager.getGlTexture())).getGlId();
		#elif MC_VERSION >= MC_1_21_2
			return manager.lightmapFramebuffer.getColorAttachment();
		#else
			return manager.texture.getGlId();
		#endif
	}

	public static Matrix4fc modelViewMatrix(
		#if MC_VERSION >= MC_1_21_9
			net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext context
		#else
			net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context
		#endif
	) {
		#if MC_VERSION >= MC_1_21_9
			return context.viewMatrix();
		#elif MC_VERSION >= MC_1_20_5
			return context.positionMatrix();
		#else
			return context.matrixStack().peek().getPositionMatrix();
		#endif
	}

	public static Matrix4fc projectionMatrix(
		#if MC_VERSION >= MC_1_21_9
			net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext context
		#else
			net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context
		#endif
	) {
		#if MC_VERSION >= MC_1_21_9
			return minecraftProjectionMatrix.get();
		#else
			return context.projectionMatrix();
		#endif
	}

	public static float partialTicks(
		#if MC_VERSION >= MC_1_21_9
			net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext context
		#else
			net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context
		#endif
	) {
		#if MC_VERSION >= MC_1_21_5
			return context.tickCounter().getTickProgress(false);
		#elif MC_VERSION >= MC_1_21_1
			return context.tickCounter().getTickDelta(false);
		#else
			return context.tickDelta();
		#endif
	}

	public static boolean isTranslucent(RenderLayer layer) {
		#if MC_VERSION >= MC_1_21_0
			return layer.isTranslucent();
		#else
			return layer.translucent;
		#endif
	}
}