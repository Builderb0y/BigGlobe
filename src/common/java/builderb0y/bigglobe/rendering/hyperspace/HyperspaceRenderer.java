package builderb0y.bigglobe.rendering.hyperspace;

import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import org.lwjgl.system.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.FastMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.rendering.CommonState;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public class HyperspaceRenderer implements SafeCloseable {

	public static final RenderPipeline
		VOLUME_PIPELINE = (
			RenderPipeline
			.builder()
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace_volume"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace_volume"))
			.withVertexShader(BigGlobeMod.mcID("core/screenquad"))
			.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			.build()
		),
		HORIZONTAL_BLUR_PIPELINE = (
			RenderPipeline
			.builder()
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace_horizontal_blur"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace_horizontal_blur"))
			.withVertexShader(BigGlobeMod.mcID("core/screenquad"))
			.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
			.withSampler("previousPass")
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			.build()
		),
		VERTICAL_BLUR_PIPELINE = (
			RenderPipeline
			.builder()
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace_vertical_blur"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace_vertical_blur"))
			.withVertexShader(BigGlobeMod.mcID("core/screenquad"))
			.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
			.withSampler("previousPass")
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			.build()
		),
		FRACTAL_PIPELINE = (
			RenderPipeline
			.builder()
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace_fractals"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace_fractals"))
			.withVertexShader(BigGlobeMod.mcID("core/screenquad"))
			.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
			.withSampler("previousPass")
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			.build()
		),
		COLLAPSE_PIPELINE = (
			RenderPipeline
			.builder()
			.withLocation(BigGlobeMod.modID("pipeline/hyperspace_collapse"))
			.withFragmentShader(BigGlobeMod.modID("post/hyperspace_collapse"))
			.withVertexShader(BigGlobeMod.mcID("core/screenquad"))
			.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
			.withSampler("previousPass")
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
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

	public static HyperspaceRenderer INSTANCE;
	static {
		nope: {
			try {
				INSTANCE = new HyperspaceRenderer();
			}
			catch (RuntimeException exception) {
				BigGlobeMod.LOGGER.warn("Fancy hyperspace background unavailable:", exception);
				break nope;
			}
			//the instance can become null again if it encounters an exception while rendering.
			//and since you can't un-register an event handler, it needs to check for null instead.
			ClientTickEvents.START_LEVEL_TICK.register((ClientLevel level) -> {
				if (INSTANCE != null && level.dimension() == HyperspaceConstants.WORLD_KEY) INSTANCE.beam.tick();
			});
			LevelRenderEvents.END_EXTRACTION.register((LevelExtractionContext context) -> {
				if (INSTANCE != null) INSTANCE.extract(context);
			});
			LevelRenderEvents.START_MAIN.register((LevelTerrainRenderContext context) -> {
				if (INSTANCE != null) INSTANCE.render(context);
			});
		}
	}

	public static void init() {}

	public GpuBuffer
		volumeUniforms,
		blurUniforms,
		fractalUniforms,
		collapseUniforms;
	public RenderTarget
		swapFramebuffer;
	public GpuSampler
		sampler;
	public Beam
		beam;
	public float
		collapse;
	public boolean
		playerIsInHyperspace;

	@Override
	public void close() {
		ResourceTracker.closeAll(this.volumeUniforms, this.blurUniforms, this.fractalUniforms, this.collapseUniforms, SafeCloseable.of(this.swapFramebuffer));
	}

	public HyperspaceRenderer() {
		try {
			this.volumeUniforms = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe hyperspace volume uniforms",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				VOLUME_UNIFORM_SIZE
			);
			this.blurUniforms = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe hyperspace blur uniforms",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				BLUR_UNIFORM_SIZE
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
			this.swapFramebuffer = new TextureTarget(
				"Big Globe hyperspace swap framebuffer",
				1,
				1,
				false
			);
			this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
			this.beam = new Beam();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	public void extract(LevelExtractionContext context) {
		this.playerIsInHyperspace = context.level().dimension() == HyperspaceConstants.WORLD_KEY;
		if (this.playerIsInHyperspace) {
			float collapse = 0.0F;
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				PlayerWaypointManager manager = PlayerWaypointManager.get(player);
				if (manager != null) {
					collapse = manager.collapseProgress + (
						manager.getAllWaypoints().isEmpty()
						? +context.deltaTracker().getGameTimeDeltaPartialTick(false)
						: -context.deltaTracker().getGameTimeDeltaPartialTick(false)
					);
				}
			}
			this.collapse = collapse / PlayerWaypointManager.COLLAPSE_DURATION_TICKS;
		}
	}

	public void render(LevelTerrainRenderContext context) {
		if (this.playerIsInHyperspace) try {
			this.doRender(context);
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("Exception rendering hyperspace background. The fancy hyperspace background will now be disabled to prevent more issues later.", exception);
			INSTANCE = null;
			this.close();
		}
	}

	public void doRender(LevelTerrainRenderContext context) {
		if (
			RenderSystem.getDevice().precompilePipeline(         VOLUME_PIPELINE).isValid() &&
			RenderSystem.getDevice().precompilePipeline(HORIZONTAL_BLUR_PIPELINE).isValid() &&
			RenderSystem.getDevice().precompilePipeline(  VERTICAL_BLUR_PIPELINE).isValid() &&
			RenderSystem.getDevice().precompilePipeline(        FRACTAL_PIPELINE).isValid() &&
			RenderSystem.getDevice().precompilePipeline(       COLLAPSE_PIPELINE).isValid()
		) {
			RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
			if (this.swapFramebuffer.width != mainTarget.width || this.swapFramebuffer.height != mainTarget.height) {
				this.swapFramebuffer.resize(mainTarget.width, mainTarget.height);
			}
			this.uploadVolumeUniforms(context);
			this.uploadBlurUniforms(mainTarget.width, mainTarget.height);
			this.uploadFractalUniforms(context);
			if (this.collapse > 0.0F) {
				this.uploadCollapseUniforms(context);
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
			RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "Big Globe hyperspace volume",
				target.getColorTextureView(),
				OptionalInt.empty()
			)
		) {
			pass.setPipeline(VOLUME_PIPELINE);
			pass.setUniform("Uniforms", this.volumeUniforms);
			pass.draw(0, 3);
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
			RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "Big Globe hyperspace " + (vertical ? "vertical" : "horizontal") + " blur",
				destination.getColorTextureView(),
				OptionalInt.empty()
			)
		) {
			pass.setPipeline(vertical ? VERTICAL_BLUR_PIPELINE : HORIZONTAL_BLUR_PIPELINE);
			pass.setUniform("Uniforms", this.blurUniforms);
			pass.bindTexture("previousPass", source.getColorTextureView(), this.sampler);
			pass.draw(0, 3);
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
				.get()
			);
		}
	}

	public void renderFractals(RenderTarget source, RenderTarget destination) {
		try (
			RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "Big Globe hyperspace fractals",
				destination.getColorTextureView(),
				OptionalInt.empty()
			)
		) {
			pass.setPipeline(FRACTAL_PIPELINE);
			pass.setUniform("Uniforms", this.fractalUniforms);
			pass.bindTexture("previousPass", source.getColorTextureView(), this.sampler);
			pass.draw(0, 3);
		}
	}

	public void uploadCollapseUniforms(LevelTerrainRenderContext context) {
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
			RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "Big Globe hyperspace collapse",
				destination.getColorTextureView(),
				OptionalInt.empty()
			)
		) {
			pass.setPipeline(COLLAPSE_PIPELINE);
			pass.setUniform("Uniforms", this.collapseUniforms);
			pass.bindTexture("previousPass", source.getColorTextureView(), this.sampler);
			pass.draw(0, 3);
		}
	}

	@Environment(EnvType.CLIENT)
	public static class Beam {

		public static final int maxBeamTicks = 10 * 20;

		public float
			originX,
			originY,
			originZ,
			directionSeed;
		public int
			beamTicks = -1;
		public Permuter
			random = new Permuter(Permuter.stafford(System.currentTimeMillis() ^ System.nanoTime()));

		public float getBeamTime() {
			return (this.beamTicks + CommonState.partialTicks) / maxBeamTicks * 2.0F - 1.0F;
		}

		public void tick() {
			if (this.beamTicks >= 0) {
				this.beamTicks++;
				if (this.beamTicks >= maxBeamTicks) {
					this.beamTicks = -1;
					this.directionSeed = -1.0F;
					this.originX = 0.0F;
					this.originY = 0.0F;
					this.originZ = 0.0F;
				}
				else if (this.beamTicks == maxBeamTicks >> 1) {
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
			}
			else if (this.random.nextInt(100) == 0) {
				LocalPlayer player = Minecraft.getInstance().player;
				if (player != null) {
					PlayerWaypointManager manager = PlayerWaypointManager.get(player);
					if (manager != null && manager.collapseProgress < 0) {
						this.beamTicks = 0;
						this.directionSeed = this.random.nextFloat();
						this.originX = (float)(this.random.nextGaussian() * 16.0D);
						this.originY = (float)(this.random.nextGaussian() * 16.0D);
						this.originZ = (float)(this.random.nextGaussian() * 16.0D);
					}
				}
			}
		}
	}
}