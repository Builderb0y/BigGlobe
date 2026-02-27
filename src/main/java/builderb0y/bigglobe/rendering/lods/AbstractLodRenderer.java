package builderb0y.bigglobe.rendering.lods;

import java.util.Collections;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.rendering.*;
import builderb0y.bigglobe.rendering.lods.ScratchDepthBuffer.FakeScratchDepthBuffer;
import builderb0y.bigglobe.rendering.lods.ScratchDepthBuffer.RealScratchDepthBuffer;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.bigglobe.versions.RenderVersions;

import static org.lwjgl.opengl.GL32C.*;

@Environment(EnvType.CLIENT)
public abstract class AbstractLodRenderer implements LodRenderer {

	public VertexHeap heap;
	public ElementBuffer elementBuffer;
	public int vao;
	public ScratchDepthBuffer depthBuffer;
	public MatrixStorageWorkaround matrixStorage;

	@Override
	public void close() {
		if (this.vao != 0) { glDeleteVertexArrays(this.vao); this.vao = 0; }
		ResourceTracker.closeAll(this.heap, this.elementBuffer, this.depthBuffer, this.matrixStorage);
	}

	public AbstractLodRenderer(int quadCount) {
		BigGlobeMod.LOGGER.info("Using " + this.getClass().getSimpleName());
		if (quadCount < 0 || quadCount >= ((int)((1L << 32) / 4L))) {
			throw new IllegalArgumentException("Quad count out of range: " + quadCount);
		}
		try {
			this.heap = new VertexHeap(LodVertexFormat.FORMAT, quadCount);
			this.elementBuffer = new ElementBuffer();
			this.vao = glGenVertexArrays();
			#if MC_VERSION >= MC_1_21_5
				this.depthBuffer = new FakeScratchDepthBuffer();
			#else
				this.depthBuffer = new RealScratchDepthBuffer();
			#endif
			this.matrixStorage = new MatrixStorageWorkaround();

			this.setupVao();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	public void setupVao() {
		int oldVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
		glBindVertexArray(this.vao);
		glBindBuffer(GL_ARRAY_BUFFER, this.heap.glID);
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glEnableVertexAttribArray(3);
		glEnableVertexAttribArray(4);
		glVertexAttribIPointer(0, 2, GL_UNSIGNED_BYTE, 12, 0L); //horizontalPosition
		glVertexAttribIPointer(1, 1, GL_SHORT, 12, 2L); //verticalPosition
		glVertexAttribIPointer(2, 2, GL_UNSIGNED_SHORT, 12, 4L); //texcoordData
		glVertexAttribIPointer(3, 3, GL_UNSIGNED_BYTE, 12, 8L); //colorData
		glVertexAttribIPointer(4, 1, GL_UNSIGNED_BYTE, 12, 11L); //lightData
		glBindVertexArray(oldVao);

		GLException.check();
	}

	public void setupOpaqueState(LodGlState state) {
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		int framebufferID = RenderVersions.glID(framebuffer);
		state.setFramebuffer(framebufferID);
		this.depthBuffer.copyFrom(framebufferID, framebuffer.textureWidth, framebuffer.textureHeight);
		state.setViewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
		state.setCullFace(true);
		state.setCullFaceMode(GL_BACK);
		state.setDepthRead(true);
		state.setDepthWrite(true);
		state.setDepthFunc(GL_LEQUAL);
		state.setBlend(false);
		state.setColorMask(true, true, true, true);
	}

	public void setupUniforms(LodRenderState state, VanillaLodShader shader) {
		this.matrixStorage.set(state.frustum.modelViewProjectionMatrix);
		nglUniformMatrix4fv(shader.modelViewProjectionMatrix, 1, false, this.matrixStorage.address());
		glUniform3f(shader.fogColor, state.fogR, state.fogG, state.fogB);
		float globalFogDensity = BigGlobeConfig.INSTANCE.get().lodRendering.fogDensity;
		float fogHeightScale = BigGlobeConfig.INSTANCE.get().lodRendering.fogHeightScale;
		ClientGeneratorParams params;
		if (
			state.clientState != null &&
			(params = state.clientState.generatorParams) != null &&
			params.seaLevel != null &&
			fogHeightScale != 0.0F
		) {
			glUniform3f(
				shader.fogParams,
				(float)(state.frustum.y - params.seaLevel.doubleValue()),
				-fogHeightScale / ((float)(state.worldMaxY - params.seaLevel)),
				Interpolator.mixSmoothUnchecked(
					-1.0F,
					Interpolator.mixSmoothUnchecked(-2.0F, -4.0F, state.thunderStrength),
					state.rainStrength
				)
				* globalFogDensity / state.frustum.farClippingPlane
			);
		}
		else {
			glUniform3f(shader.fogParams, 0.0F, 0.0F, -globalFogDensity / state.frustum.farClippingPlane);
		}
	}

	@Override
	public void oom() {
		this.heap.cleanup();
	}

	@Override
	public List<String> getF3MenuText() {
		long capacity = this.heap.capacity;
		long usedBytes = this.heap.used();
		long reallyUsedBytes = this.heap.reallyUsed();
		long percent = usedBytes * 100L / capacity;
		long fragmentation = usedBytes == 0L ? 0L : 100L - reallyUsedBytes * 100L / usedBytes;
		long reallyUsedFraction = reallyUsedBytes * 100L / capacity;
		return Collections.singletonList("[BG] LOD Geometry: P: " + percent + "%, F: " + fragmentation + "%" + ", R: " + reallyUsedFraction);
	}

	public static class LodGlState extends GlState {

		public boolean inTranslucentPass;

		@Override
		public void capture() {
			this.inTranslucentPass = false;
			super.capture();
		}
	}
}