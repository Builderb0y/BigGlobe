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
#else
	import com.mojang.blaze3d.buffers.GpuBuffer;
	import com.mojang.blaze3d.buffers.Std140Builder;
	import com.mojang.blaze3d.buffers.Std140SizeCalculator;
	import com.mojang.blaze3d.pipeline.RenderPipeline;
	import com.mojang.blaze3d.pipeline.RenderTarget;
	import com.mojang.blaze3d.systems.RenderPass;
	import com.mojang.blaze3d.systems.RenderSystem;
#endif

public class CloudyHyperspaceRenderer extends BlurryHyperspaceRenderer {

	public static final RenderPipeline
		CLOUDS_PIPELINE = (
			RenderPipeline
			.builder(WITHOUT_PREVIOUS)
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace/cloudy/clouds"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace/cloudy/clouds"))
			.build()
		);
	public static final int
		CLOUD_UNIFORM_SIZE = (
			new Std140SizeCalculator()
			.putMat4f()
			.putMat4f()
			.putVec4()
			.putVec4()
			.putFloat()
			.putFloat()
			.putFloat()
			.putFloat()
			.align(16)
			.get()
		);

	public GpuBuffer uniformBuffer;
	public Beam beam;

	@Override
	public void close() {
		ResourceTracker.closeAll(super::close, this.uniformBuffer);
	}

	public CloudyHyperspaceRenderer() {
		try {
			this.uniformBuffer = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe hyperspace cloud buffer",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				CLOUD_UNIFORM_SIZE
			);
			this.beam = new Beam() {

				@Override
				public void playSound() {
					Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
					if (cameraEntity != null) {
						Minecraft.getInstance().getSoundManager().play(
							new SimpleSoundInstance(
								SoundEvents.LIGHTNING_BOLT_THUNDER.location(),
								SoundSource.AMBIENT,
								0.25F,
								1.0F,
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
			RenderVersions.isRenderPipelineValid(CLOUDS_PIPELINE) &&
			RenderVersions.isRenderPipelineValid(HORIZONTAL_BLUR_PIPELINE) &&
			RenderVersions.isRenderPipelineValid(VERTICAL_BLUR_PIPELINE)
		) {
			RenderTarget mainTarget = RenderVersions.getMainRenderTarget();
			if (this.swapFramebuffer.width != mainTarget.width || this.swapFramebuffer.height != mainTarget.height) {
				this.swapFramebuffer.resize(mainTarget.width, mainTarget.height);
			}
			this.uploadUniforms(context);
			this.uploadBlurUniforms(mainTarget.width, mainTarget.height);
			this.renderClouds(mainTarget);
			this.renderBlur(false, mainTarget, this.swapFramebuffer);
			this.renderBlur(true, this.swapFramebuffer, mainTarget);
		}
		else {
			INSTANCE = null;
			this.close();
		}
	}

	public void uploadUniforms(LevelTerrainRenderContext context) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
				this.uniformBuffer.slice(),
				Std140Builder
				.onStack(stack, CLOUD_UNIFORM_SIZE)
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
				.putFloat(this.collapse)
				.get()
			);
		}
	}

	public void renderClouds(RenderTarget target) {
		try (
			RenderPass pass = RenderVersions.createRenderPass(
				"Big Globe hyperspace clouds",
				target.getColorTextureView()
			)
		) {
			RenderVersions.setPipeline(pass, CLOUDS_PIPELINE);
			pass.setUniform("Uniforms", this.uniformBuffer);
			RenderVersions.drawScreenTriangle(pass);
		}
	}
}