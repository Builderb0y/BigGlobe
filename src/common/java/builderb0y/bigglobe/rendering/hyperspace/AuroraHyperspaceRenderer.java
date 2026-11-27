package builderb0y.bigglobe.rendering.hyperspace;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import org.lwjgl.system.*;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.rendering.CommonState;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.versions.RenderVersions;

#if MC_VERSION >= MC_26_3_0
	import com.mojang.blaze3d.buffers.Std140Builder;
	import com.mojang.blaze3d.buffers.Std140SizeCalculator;
	import com.mojang.blaze3d.pipeline.RenderTarget;
	import com.mojang.blaze3d.systems.RenderSystem;
	import com.mojang.renderpearl.api.buffers.GpuBuffer;
	import com.mojang.renderpearl.api.commands.RenderPass;
	import com.mojang.renderpearl.api.pipeline.RenderPipeline;
	import com.mojang.renderpearl.api.textures.FilterMode;
#else
	import com.mojang.blaze3d.buffers.GpuBuffer;
	import com.mojang.blaze3d.buffers.Std140Builder;
	import com.mojang.blaze3d.buffers.Std140SizeCalculator;
	import com.mojang.blaze3d.pipeline.RenderPipeline;
	import com.mojang.blaze3d.pipeline.RenderTarget;
	import com.mojang.blaze3d.systems.RenderPass;
	import com.mojang.blaze3d.systems.RenderSystem;
	import com.mojang.blaze3d.textures.FilterMode;
#endif

public class AuroraHyperspaceRenderer extends BlurryHyperspaceRenderer {

	public static final RenderPipeline
		AURORA_PIPELINE = (
			RenderPipeline
			.builder(WITHOUT_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/auroras/aurora"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/auroras/aurora"))
			.build()
		),
		STAR_PIPELINE = (
			RenderPipeline
			.builder(WITH_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/auroras/stars"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/auroras/stars"))
			.build()
		),
		CLOUD_PIPELINE = (
			RenderPipeline
			.builder(WITH_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/auroras/clouds"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/auroras/clouds"))
			.build()
		),
		COLLAPSE_PIPELINE = (
			RenderPipeline
			.builder(WITH_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/auroras/collapse"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/auroras/collapse"))
			.build()
		);
	public static final int
		AURORA_UNIFORM_SIZE = (
			new Std140SizeCalculator()
			.putMat4f()
			.putMat4f()
			.putVec4()
			.putFloat()
			.putFloat()
			.align(16)
			.get()
		),
		COLLAPSE_UNIFORM_SIZE = 4;

	public GpuBuffer auroraBuffer, collapseBuffer;

	@Override
	public void close() {
		ResourceTracker.closeAll(super::close, this.auroraBuffer, this.collapseBuffer);
	}

	public AuroraHyperspaceRenderer() {
		try {
			this.auroraBuffer = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe hyperspace aurora buffer",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				AURORA_UNIFORM_SIZE
			);
			this.collapseBuffer = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe hyperspace aurora collapse buffer",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				COLLAPSE_UNIFORM_SIZE
			);
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	@Override
	public void tick() {

	}

	@Override
	public void doRender(LevelTerrainRenderContext context) {
		if (
			RenderVersions.isRenderPipelineValid(         AURORA_PIPELINE) &&
			RenderVersions.isRenderPipelineValid(HORIZONTAL_BLUR_PIPELINE) &&
			RenderVersions.isRenderPipelineValid(  VERTICAL_BLUR_PIPELINE) &&
			RenderVersions.isRenderPipelineValid(       COLLAPSE_PIPELINE)
		) {
			RenderTarget mainTarget = RenderVersions.getMainRenderTarget();
			if (this.swapFramebuffer.width != mainTarget.width || this.swapFramebuffer.height != mainTarget.height) {
				this.swapFramebuffer.resize(mainTarget.width, mainTarget.height);
			}
			this.uploadUniforms(context);
			this.uploadBlurUniforms(mainTarget.width, mainTarget.height);
			if (this.collapse > 0.0F) {
				this.uploadCollapseUniforms();
				this.renderAurora(this.swapFramebuffer);
				this.renderBlur(false, this.swapFramebuffer, mainTarget);
				this.renderBlur(true, mainTarget, this.swapFramebuffer);
				this.renderStars(this.swapFramebuffer, mainTarget);
				this.renderCollapse(mainTarget, this.swapFramebuffer);
				this.renderClouds(this.swapFramebuffer, mainTarget);
			}
			else {
				this.renderAurora(mainTarget);
				this.renderBlur(false, mainTarget, this.swapFramebuffer);
				this.renderBlur(true, this.swapFramebuffer, mainTarget);
				this.renderStars(mainTarget, this.swapFramebuffer);
				this.renderClouds(this.swapFramebuffer, mainTarget);
			}
		}
		else {
			INSTANCE = null;
			this.close();
		}
	}

	public void uploadUniforms(LevelTerrainRenderContext context) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
				this.auroraBuffer.slice(),
				Std140Builder
				.onStack(stack, AURORA_UNIFORM_SIZE)
				.putMat4f(CommonState.inverseModelViewMatrix)
				.putMat4f(CommonState.inverseProjectionMatrix)
				.putVec4(
					(float)(context.levelState().cameraRenderState.pos.x),
					(float)(context.levelState().cameraRenderState.pos.y),
					(float)(context.levelState().cameraRenderState.pos.z),
					0.0F
				)
				.putFloat(CommonState.dayTimeInSeconds)
				.putFloat(this.collapse)
				.get()
			);
		}
	}

	public void renderAurora(RenderTarget target) {
		try (
			RenderPass pass = RenderVersions.createRenderPass(
				"Big Globe hyperspace auroras",
				target.getColorTextureView()
			)
		) {
			RenderVersions.setPipeline(pass, AURORA_PIPELINE);
			pass.setUniform("Uniforms", this.auroraBuffer);
			RenderVersions.drawScreenTriangle(pass);
		}
	}

	public void renderStars(RenderTarget source, RenderTarget destination) {
		try (
			RenderPass pass = RenderVersions.createRenderPass(
				"Big Globe hyperspace aurora stars",
				destination.getColorTextureView()
			)
		) {
			RenderVersions.setPipeline(pass, STAR_PIPELINE);
			pass.setUniform("Uniforms", this.auroraBuffer);
			RenderVersions.bindSampler(pass, "previousPass", source.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			RenderVersions.drawScreenTriangle(pass);
		}
	}

	public void renderClouds(RenderTarget source, RenderTarget destination) {
		try (
			RenderPass pass = RenderVersions.createRenderPass(
				"Big Globe hyperspace aurora clouds",
				destination.getColorTextureView()
			)
		) {
			RenderVersions.setPipeline(pass, CLOUD_PIPELINE);
			pass.setUniform("Uniforms", this.auroraBuffer);
			RenderVersions.bindSampler(pass, "previousPass", source.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			RenderVersions.drawScreenTriangle(pass);
		}
	}

	public void uploadCollapseUniforms() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
				this.collapseBuffer.slice(),
				Std140Builder
				.onStack(stack, COLLAPSE_UNIFORM_SIZE)
				.putFloat(this.collapse)
				.get()
			);
		}
	}

	public void renderCollapse(RenderTarget source, RenderTarget destination) {
		try (
			RenderPass pass = RenderVersions.createRenderPass(
				() -> "Big Globe hyperspace collapse",
				destination.getColorTextureView()
			)
		) {
			RenderVersions.setPipeline(pass, COLLAPSE_PIPELINE);
			pass.setUniform("Uniforms", this.collapseBuffer);
			RenderVersions.bindSampler(pass, "previousPass", source.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			RenderVersions.drawScreenTriangle(pass);
		}
	}
}