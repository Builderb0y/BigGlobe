package builderb0y.bigglobe.rendering2.waypoints;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;
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
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import org.lwjgl.system.*;

import net.minecraft.client.Minecraft;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.entities.WaypointEntityRenderer;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.rendering2.CommonState;
import builderb0y.bigglobe.rendering2.NativeMemory;
import builderb0y.bigglobe.rendering2.ResourceTracker;
import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public class WaypointRenderer implements SafeCloseable {

	public static final RenderPipeline PIPELINE = (
		RenderPipeline
		.builder()
		.withLocation(BigGlobeMod.modID("pipeline/waypoints"))
		.withFragmentShader(BigGlobeMod.modID("post/waypoints"))
		.withVertexShader(BigGlobeMod.mcID("core/screenquad"))
		.withSampler("colortex")
		.withSampler("depthtex")
		.withUniform("waypoints", UniformType.TEXEL_BUFFER, TextureFormat.RED8I)
		.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
		.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
		.build()
	);
	public static final int UNIFORM_SIZE = (
		new Std140SizeCalculator()
		.putMat4f()
		.putMat4f()
		.putMat4f()
		.putMat4f()
		.putFloat()
		.putInt()
		.align(16)
		.get()
	);

	public static WaypointRenderer INSTANCE;
	static {
		nope: {
			try {
				INSTANCE = new WaypointRenderer();
			}
			catch (RuntimeException exception) {
				BigGlobeMod.LOGGER.warn("Fancy waypoints unavailable:", exception);
				break nope;
			}
			LevelRenderEvents.END_EXTRACTION.register((LevelExtractionContext context) -> {
				if (INSTANCE != null) INSTANCE.state.waypoints = context.levelState().entityRenderStates.stream().filter(WaypointEntityRenderer.State.class::isInstance).map(WaypointEntityRenderer.State.class::cast).toList();
			});
			LevelRenderEvents.END_MAIN.register((LevelRenderContext context) -> {
				if (INSTANCE != null) INSTANCE.render(context);
			});
		}
	}

	public static void init() {}

	public GpuBuffer uniformData, waypointBuffer;
	public RenderTarget swapFramebuffer;
	public State state;

	@Override
	public void close() {
		ResourceTracker.closeAll(this.uniformData, this.waypointBuffer, SafeCloseable.of(this.swapFramebuffer));
	}

	public WaypointRenderer() {
		try {
			this.uniformData = RenderSystem.getDevice().createBuffer(
				() -> "Big Globe waypoint shader uniforms",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				UNIFORM_SIZE
			);
			this.allocateWaypointBuffer(Float.BYTES * 4);
			this.swapFramebuffer = new TextureTarget(
				"Big Globe waypoint swap framebuffer",
				1,
				1,
				false
			);
			this.state = new State();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	public void allocateWaypointBuffer(long bytes) {
		this.waypointBuffer = RenderSystem.getDevice().createBuffer(
			() -> "Big Globe waypoint buffer",
			GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_COPY_DST,
			bytes
		);
	}

	public void render(LevelRenderContext context) {
		if (!this.state.waypoints.isEmpty()) try {
			this.doRender(context);
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("Exception rendering waypoints. Fancy waypoints will now be disabled to prevent more issues later.", exception);
			INSTANCE = null;
			this.close();
		}
	}

	public void doRender(LevelRenderContext context) {
		if (RenderSystem.getDevice().precompilePipeline(PIPELINE).isValid()) {
			RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
			if (this.swapFramebuffer.width != mainTarget.width || this.swapFramebuffer.height != mainTarget.height) {
				this.swapFramebuffer.resize(mainTarget.width, mainTarget.height);
			}
			this.setUniforms();
			this.uploadPositions(context);
			try (
				RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
					() -> "Big Globe waypoints",
					this.swapFramebuffer.getColorTextureView(),
					OptionalInt.empty()
				)
			) {
				pass.setPipeline(PIPELINE);
				pass.bindTexture("colortex", mainTarget.getColorTextureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR));
				pass.bindTexture("depthtex", mainTarget.getDepthTextureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR));
				pass.setUniform("Uniforms", this.uniformData);
				pass.setUniform("waypoints", this.waypointBuffer);
				pass.draw(0, 3);
			}
			RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(this.swapFramebuffer.getColorTexture(), mainTarget.getColorTexture(), 0, 0, 0, 0, 0, mainTarget.width, mainTarget.height);
		}
	}

	public void setUniforms() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
				this.uniformData.slice(),
				Std140Builder
				.onStack(stack, UNIFORM_SIZE)
				.putMat4f(CommonState.modelViewMatrix)
				.putMat4f(CommonState.inverseModelViewMatrix)
				.putMat4f(CommonState.projectionMatrix)
				.putMat4f(CommonState.inverseProjectionMatrix)
				.putFloat(CommonState.dayTimeInSeconds)
				.putInt(this.state.waypoints.size())
				.get()
			);
		}
	}

	public void uploadPositions(LevelTerrainRenderContext context) {
		try (NativeMemory memory = new NativeMemory(this.state.waypoints.size() * (Float.BYTES * 4))) {
			for (WaypointEntityRenderer.State waypoint : this.state.waypoints) {
				memory
				.appendInt(BigGlobeMath.floorI((waypoint.x        - context.levelState().cameraRenderState.pos.x) * 65536.0D), ByteOrder.LITTLE_ENDIAN)
				.appendInt(BigGlobeMath.floorI((waypoint.y + 1.0D - context.levelState().cameraRenderState.pos.y) * 65536.0D), ByteOrder.LITTLE_ENDIAN)
				.appendInt(BigGlobeMath.floorI((waypoint.z        - context.levelState().cameraRenderState.pos.z) * 65536.0D), ByteOrder.LITTLE_ENDIAN)
				.appendInt(
					BigGlobeMath.floorI(
						(
							(
								waypoint.health / WaypointEntity.MAX_HEALTH
							)
							+ (
								Math.sin(
									waypoint.ageInTicks * (Math.PI / 50.0D)
								)
								* 0.125D
							)
						)
						* 65536.0D
					),
					ByteOrder.LITTLE_ENDIAN
				);
			}
			if (this.waypointBuffer.size() < memory.used) {
				this.waypointBuffer.close();
				this.waypointBuffer = null;
				this.allocateWaypointBuffer(BigGlobeMath.smallestEncompassingPowerOfTwo(memory.used));
			}
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.waypointBuffer.slice(0L, memory.used), memory.toByteBuffer());
		}
	}

	@Environment(EnvType.CLIENT)
	public static class State {

		public List<WaypointEntityRenderer.State> waypoints = Collections.emptyList();
	}
}