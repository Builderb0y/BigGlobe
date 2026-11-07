package builderb0y.bigglobe.rendering.waypoints;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.*;
import org.lwjgl.system.*;

import net.minecraft.client.Minecraft;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.entities.WaypointEntityRenderer;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.rendering.CommonState;
import builderb0y.bigglobe.rendering.NativeMemory;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.versions.RenderVersions;

#if MC_VERSION >= MC_26_2_0
	import com.mojang.blaze3d.GpuFormat;
	import com.mojang.blaze3d.pipeline.BindGroupLayout;
#else
	import com.mojang.blaze3d.textures.TextureFormat;
#endif

@Environment(EnvType.CLIENT)
public class WaypointRenderer implements SafeCloseable {

	public static final RenderPipeline PIPELINE;
	static {
		RenderPipeline.Builder builder = (
			RenderPipeline
			.builder()
			.withLocation(BigGlobeMod.modID("pipeline/waypoints"))
			.withFragmentShader(BigGlobeMod.modID("post/waypoints"))
			.withVertexShader(BigGlobeMod.mcID("core/screenquad"))
		);
		#if MC_VERSION >= MC_26_2_0
			builder
			.withBindGroupLayout(
				BindGroupLayout
				.builder()
				.withSampler("colortex")
				.withSampler("depthtex")
				.withUniform("waypoints", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT)
				.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
				.build()
			)
			.withShaderDefine("HAVE_RGBA32F_WAYPOINTS")
			.withVertexBinding(0, DefaultVertexFormat.POSITION)
			.withPrimitiveTopology(PrimitiveTopology.TRIANGLES);

			if (RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()) {
				builder.withShaderDefine("Z_ZERO_TO_ONE");
			}
		#else
			builder
			.withSampler("colortex")
			.withSampler("depthtex")
			.withUniform("waypoints", UniformType.TEXEL_BUFFER, TextureFormat.RED8I)
			.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
		#endif
		PIPELINE = builder.build();
	}
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
			LevelExtractionEvents.END_EXTRACTION.register((LevelExtractionContext context) -> {
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
				#if MC_VERSION >= MC_26_2_0
				, GpuFormat.RGBA8_UNORM
				#endif
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
			RenderTarget mainTarget = RenderVersions.getMainRenderTarget();
			if (this.swapFramebuffer.width != mainTarget.width || this.swapFramebuffer.height != mainTarget.height) {
				this.swapFramebuffer.resize(mainTarget.width, mainTarget.height);
			}
			this.setUniforms();
			this.uploadPositions(context);
			try (
				RenderPass pass = RenderVersions.createRenderPass(
					"Big Globe waypoints",
					this.swapFramebuffer.getColorTextureView()
				)
			) {
				pass.setPipeline(PIPELINE);
				pass.bindTexture("colortex", mainTarget.getColorTextureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR));
				pass.bindTexture("depthtex", mainTarget.getDepthTextureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR));
				pass.setUniform("Uniforms", this.uniformData);
				pass.setUniform("waypoints", this.waypointBuffer);
				RenderVersions.drawScreenTriangle(pass);
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
				double x = waypoint.x - context.levelState().cameraRenderState.pos.x;
				double y = waypoint.y + 1.0D - context.levelState().cameraRenderState.pos.y;
				double z = waypoint.z - context.levelState().cameraRenderState.pos.z;
				double w = (
					(waypoint.health / WaypointEntity.MAX_HEALTH)
					+ (
						Math.sin(
							waypoint.ageInTicks * (Math.PI / 50.0D)
						)
						* 0.125D
					)
				);
				#if MC_VERSION >= MC_26_2_0
					memory
					.appendFloat((float)(x), ByteOrder.nativeOrder())
					.appendFloat((float)(y), ByteOrder.nativeOrder())
					.appendFloat((float)(z), ByteOrder.nativeOrder())
					.appendFloat((float)(w), ByteOrder.nativeOrder());
				#else
					memory
					.appendInt(BigGlobeMath.floorI(x * 65536.0D), ByteOrder.LITTLE_ENDIAN)
					.appendInt(BigGlobeMath.floorI(y * 65536.0D), ByteOrder.LITTLE_ENDIAN)
					.appendInt(BigGlobeMath.floorI(z * 65536.0D), ByteOrder.LITTLE_ENDIAN)
					.appendInt(BigGlobeMath.floorI(w * 65536.0D), ByteOrder.LITTLE_ENDIAN);
				#endif
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