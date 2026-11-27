package builderb0y.bigglobe.rendering.hyperspace;

import org.lwjgl.system.*;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.versions.RenderVersions;

#if MC_VERSION >= MC_26_3_0
	import com.mojang.blaze3d.buffers.Std140Builder;
	import com.mojang.blaze3d.buffers.Std140SizeCalculator;
	import com.mojang.blaze3d.pipeline.RenderTarget;
	import com.mojang.blaze3d.pipeline.TextureTarget;
	import com.mojang.blaze3d.systems.RenderSystem;
	import com.mojang.renderpearl.api.GpuFormat;
	import com.mojang.renderpearl.api.buffers.GpuBuffer;
	import com.mojang.renderpearl.api.commands.RenderPass;
	import com.mojang.renderpearl.api.pipeline.RenderPipeline;
	import com.mojang.renderpearl.api.textures.FilterMode;
	import com.mojang.renderpearl.api.textures.GpuSampler;
#elif MC_VERSION >= MC_26_2_0
	import com.mojang.blaze3d.GpuFormat;
	import com.mojang.blaze3d.buffers.GpuBuffer;
	import com.mojang.blaze3d.buffers.Std140Builder;
	import com.mojang.blaze3d.buffers.Std140SizeCalculator;
	import com.mojang.blaze3d.pipeline.RenderPipeline;
	import com.mojang.blaze3d.pipeline.RenderTarget;
	import com.mojang.blaze3d.pipeline.TextureTarget;
	import com.mojang.blaze3d.systems.RenderPass;
	import com.mojang.blaze3d.systems.RenderSystem;
	import com.mojang.blaze3d.textures.FilterMode;
	import com.mojang.blaze3d.textures.GpuSampler;
#else
	import com.mojang.blaze3d.buffers.GpuBuffer;
	import com.mojang.blaze3d.buffers.Std140Builder;
	import com.mojang.blaze3d.buffers.Std140SizeCalculator;
	import com.mojang.blaze3d.pipeline.RenderPipeline;
	import com.mojang.blaze3d.pipeline.RenderTarget;
	import com.mojang.blaze3d.pipeline.TextureTarget;
	import com.mojang.blaze3d.systems.RenderPass;
	import com.mojang.blaze3d.systems.RenderSystem;
	import com.mojang.blaze3d.textures.FilterMode;
	import com.mojang.blaze3d.textures.GpuSampler;
#endif

public abstract class BlurryHyperspaceRenderer extends HyperspaceRenderer {

	public static final RenderPipeline
		HORIZONTAL_BLUR_PIPELINE = (
			RenderPipeline
			.builder(WITH_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/horizontal_blur"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/horizontal_blur"))
			.build()
		),
		VERTICAL_BLUR_PIPELINE = (
			RenderPipeline
			.builder(WITH_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/vertical_blur"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/vertical_blur"))
			.build()
		);
	public static final int
		BLUR_UNIFORM_SIZE = (
			new Std140SizeCalculator()
			.putIVec2()
			.align(16)
			.get()
		);

	public GpuBuffer blurUniforms;
	public RenderTarget swapFramebuffer;

	@Override
	public void close() {
		ResourceTracker.closeAll(super::close, this.blurUniforms, SafeCloseable.of(this.swapFramebuffer));
	}

	public BlurryHyperspaceRenderer() {
		try {
			this.blurUniforms = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe hyperspace blur uniforms",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				BLUR_UNIFORM_SIZE
			);
			this.swapFramebuffer = new TextureTarget(
				"Big Globe hyperspace swap framebuffer",
				1,
				1,
				#if MC_VERSION >= MC_26_3_0
					GpuFormat.RGBA8_UNORM,
					null
				#elif MC_VERSION >= MC_26_2_0
					false,
					GpuFormat.RGBA8_UNORM
				#else
					false
				#endif
			);
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	public void uploadBlurUniforms(int width, int height) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
				this.blurUniforms.slice(),
				Std140Builder
				.onStack(stack, BLUR_UNIFORM_SIZE)
				.putIVec2(width, height)
				.get()
			);
		}
	}

	public void renderBlur(boolean vertical, RenderTarget source, RenderTarget destination) {
		try (
			RenderPass pass = RenderVersions.createRenderPass(
				() -> "Big Globe hyperspace " + (vertical ? "vertical" : "horizontal") + " blur",
				destination.getColorTextureView()
			)
		) {
			RenderVersions.setPipeline(pass, vertical ? VERTICAL_BLUR_PIPELINE : HORIZONTAL_BLUR_PIPELINE);
			pass.setUniform("Uniforms", this.blurUniforms);
			RenderVersions.bindSampler(pass, "previousPass", source.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			RenderVersions.drawScreenTriangle(pass);
		}
	}
}