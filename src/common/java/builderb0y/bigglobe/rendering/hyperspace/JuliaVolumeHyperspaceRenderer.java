package builderb0y.bigglobe.rendering.hyperspace;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import org.lwjgl.system.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.FastMath;
import builderb0y.bigglobe.rendering.CommonState;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.versions.RenderVersions;

#if MC_VERSION >= MC_26_3_0
	import com.mojang.renderpearl.api.buffers.GpuBuffer;
	import com.mojang.renderpearl.api.commands.RenderPass;
	import com.mojang.renderpearl.api.pipeline.RenderPipeline;
#else
	import com.mojang.blaze3d.buffers.GpuBuffer;
	import com.mojang.blaze3d.buffers.Std140Builder;
	import com.mojang.blaze3d.buffers.Std140SizeCalculator;
	import com.mojang.blaze3d.pipeline.RenderPipeline;
	import com.mojang.blaze3d.pipeline.RenderTarget;
	import com.mojang.blaze3d.systems.RenderPass;
	import com.mojang.blaze3d.systems.RenderSystem;
#endif

public class JuliaVolumeHyperspaceRenderer extends BlurryHyperspaceRenderer {

	public static final RenderPipeline
		VOLUME_PIPELINE = (
			RenderPipeline
			.builder(WITHOUT_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/julia_2/volume"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/julia_2/volume"))
			.build()
		),
		FRACTAL_PIPELINE = (
			RenderPipeline
			.builder(WITH_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/julia_2/fractals"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/julia_2/fractals"))
			.build()
		),
		COLLAPSE_PIPELINE = (
			RenderPipeline
			.builder(WITH_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/collapse"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/collapse"))
			.build()
		);
	public static final int
		VOLUME_UNIFORM_SIZE = (
			new Std140SizeCalculator()
			.putMat4f()
			.putMat4f()
			.putVec4()
			.putVec4()
			.putFloat()
			.putFloat()
			.putFloat()
			.align(16)
			.get()
		),
		BLUR_UNIFORM_SIZE = (
			new Std140SizeCalculator()
			.putIVec2()
			.align(16)
			.get()
		),
		FRACTALS_UNIFORM_SIZE = (
			new Std140SizeCalculator()
			.putMat4f()
			.putMat4f()
			.putVec4()
			.putFloat()
			.putFloat()
			.putFloat()
			.align(16)
			.get()
		),
		COLLAPSE_UNIFORM_SIZE = (
			new Std140SizeCalculator()
			.putMat4f()
			.putMat4f()
			.putFloat()
			.align(16)
			.get()
		);

	public float
		dimensionSeed;
	public GpuBuffer
		volumeUniforms,
		fractalUniforms,
		collapseUniforms;
	public Beam
		beam;

	@Override
	public void close() {
		ResourceTracker.closeAll(super::close, this.volumeUniforms, this.fractalUniforms, this.collapseUniforms);
	}

	public JuliaVolumeHyperspaceRenderer() {
		try {
			this.dimensionSeed = HyperspaceRenderers.RANDOM.nextFloat();
			this.volumeUniforms = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe hyperspace volume uniforms",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				VOLUME_UNIFORM_SIZE
			);
			this.fractalUniforms = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe hyperspace fractal uniforms",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				FRACTALS_UNIFORM_SIZE
			);
			this.collapseUniforms = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe hyperspace collapse uniforms",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				COLLAPSE_UNIFORM_SIZE
			);
			this.beam = new Beam() {

				@Override
				public void playSound() {
					Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
					if (cameraEntity != null) {
						double distanceSquared = BigGlobeMath.squareD(
							cameraEntity.getX() - this.originX,
							cameraEntity.getY() - this.originY,
							cameraEntity.getZ() - this.originZ
						);
						Minecraft.getInstance().getSoundManager().play(
							new SimpleSoundInstance(
								SoundEvents.BREEZE_IDLE_GROUND.location(),
								SoundSource.AMBIENT,
								1.0F,
								(float)(FastMath.Exp.fastExp2(2.0D / (distanceSquared * (1.0D / 65536.0D) + 1.0D) - 1.0D)),
								RandomSource.create(),
								false,
								0,
								SoundInstance.Attenuation.NONE,
								this.originX,
								this.originY,
								this.originZ,
								false
							)
						);
					}
				}
			};
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	@Override
	public void tick() {
		this.beam.tick();
	}

	@Override
	public void doRender(LevelTerrainRenderContext context) {
		if (
			RenderVersions.isRenderPipelineValid(         VOLUME_PIPELINE) &&
			RenderVersions.isRenderPipelineValid(HORIZONTAL_BLUR_PIPELINE) &&
			RenderVersions.isRenderPipelineValid(  VERTICAL_BLUR_PIPELINE) &&
			RenderVersions.isRenderPipelineValid(        FRACTAL_PIPELINE) &&
			RenderVersions.isRenderPipelineValid(       COLLAPSE_PIPELINE)
		) {
			RenderTarget mainTarget = RenderVersions.getMainRenderTarget();
			if (this.swapFramebuffer.width != mainTarget.width || this.swapFramebuffer.height != mainTarget.height) {
				this.swapFramebuffer.resize(mainTarget.width, mainTarget.height);
			}
			this.uploadVolumeUniforms(context);
			this.uploadBlurUniforms(mainTarget.width, mainTarget.height);
			this.uploadFractalUniforms(context);
			if (this.collapse > 0.0F) {
				this.uploadCollapseUniforms();
				this.renderVolume(mainTarget);
				this.renderBlur(false, mainTarget, this.swapFramebuffer);
				this.renderBlur(true, this.swapFramebuffer, mainTarget);
				this.renderFractals(mainTarget, this.swapFramebuffer);
				this.renderCollapse(this.swapFramebuffer, mainTarget);
			}
			else {
				this.renderVolume(this.swapFramebuffer);
				this.renderBlur(false, this.swapFramebuffer, mainTarget);
				this.renderBlur(true, mainTarget, this.swapFramebuffer);
				this.renderFractals(this.swapFramebuffer, mainTarget);
			}
		}
		else {
			INSTANCE = null;
			this.close();
		}
	}

	public void uploadVolumeUniforms(LevelTerrainRenderContext context) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
				this.volumeUniforms.slice(),
				Std140Builder
				.onStack(stack, VOLUME_UNIFORM_SIZE)
				.putMat4f(CommonState.inverseModelViewMatrix)
				.putMat4f(CommonState.inverseProjectionMatrix)
				.putVec4(
					(float)(context.levelState().cameraRenderState.pos.x),
					(float)(context.levelState().cameraRenderState.pos.y),
					(float)(context.levelState().cameraRenderState.pos.z),
					0.0F
				)
				.putVec4(
					this.beam.originX,
					this.beam.originY,
					this.beam.originZ,
					0.0F
				)
				.putFloat(this.beam.directionSeed)
				.putFloat(this.beam.getBeamTime())
				.putFloat(CommonState.dayTimeInSeconds)
				.get()
			);
		}
	}

	public void renderVolume(RenderTarget target) {
		try (
			RenderPass pass = RenderVersions.createRenderPass(
				"Big Globe hyperspace volume",
				target.getColorTextureView()
			)
		) {
			RenderVersions.setPipeline(pass, VOLUME_PIPELINE);
			pass.setUniform("Uniforms", this.volumeUniforms);
			RenderVersions.drawScreenTriangle(pass);
		}
	}

	public void uploadFractalUniforms(LevelTerrainRenderContext context) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
				this.fractalUniforms.slice(),
				Std140Builder
				.onStack(stack, FRACTALS_UNIFORM_SIZE)
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
				.putFloat(this.dimensionSeed)
				.get()
			);
		}
	}

	public void renderFractals(RenderTarget source, RenderTarget destination) {
		try (
			RenderPass pass = RenderVersions.createRenderPass(
				"Big Globe hyperspace fractals",
				destination.getColorTextureView()
			)
		) {
			RenderVersions.setPipeline(pass, FRACTAL_PIPELINE);
			pass.setUniform("Uniforms", this.fractalUniforms);
			RenderVersions.bindSampler(pass, "previousPass", source.getColorTextureView(), this.previousPass);
			RenderVersions.drawScreenTriangle(pass);
		}
	}

	public void uploadCollapseUniforms() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
				this.collapseUniforms.slice(),
				Std140Builder
				.onStack(stack, COLLAPSE_UNIFORM_SIZE)
				.putMat4f(CommonState.inverseModelViewMatrix)
				.putMat4f(CommonState.inverseProjectionMatrix)
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
			pass.setUniform("Uniforms", this.collapseUniforms);
			RenderVersions.bindSampler(pass, "previousPass", source.getColorTextureView(), this.previousPass);
			RenderVersions.drawScreenTriangle(pass);
		}
	}

	public static record Pipelines(
		RenderPipeline volume,
		boolean blurVolume,
		RenderPipeline fractals,
		RenderPipeline collapse
	) {}
}