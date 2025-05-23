package builderb0y.bigglobe.lods;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.AbstractTexture;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

import static org.lwjgl.opengl.GL32C.*;

@Environment(EnvType.CLIENT)
public abstract class AbstractLodRenderer implements LodRenderer {

	public VertexHeap heap;
	public ElementBuffer elementBuffer;
	public int vao;
	public MatrixStorageWorkaround matrixStorage;

	@Override
	public void close() {
		if (this.vao != 0) { glDeleteVertexArrays(this.vao); this.vao = 0; }
		ResourceTracker.closeAll(this.heap, this.elementBuffer, this.matrixStorage);
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
		glVertexAttribIPointer(0, 2, GL_UNSIGNED_BYTE, 16, 0L); //horizontalPosition
		glVertexAttribIPointer(1, 1, GL_SHORT, 16, 2L); //verticalPosition
		glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, 16, 4L); //color
		glVertexAttribPointer(3, 2, GL_UNSIGNED_SHORT, false, 16, 8L); //texcoord
		glVertexAttribPointer(4, 2, GL_UNSIGNED_BYTE, true, 16, 12L); //lmcoord
		//and 2 bytes of padding.
		glBindVertexArray(oldVao);

		GLException.check();
	}

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
					.getFramebufferManager(),
					framebuffer.getDepthAttachment()
				)
			);
		#else
			return framebuffer.fbo;
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
		#if MC_VERSION >= MC_1_21_5
			return ((net.minecraft.client.texture.GlTexture)(manager.getGlTexture())).getGlId();
		#elif MC_VERSION >= MC_1_21_2
			return manager.lightmapFramebuffer.getColorAttachment();
		#else
			return manager.texture.getGlId();
		#endif
	}

	public static float tickProgress(WorldRenderContext context) {
		#if MC_VERSION >= MC_1_21_5
			return context.tickCounter().getTickProgress(false);
		#elif MC_VERSION >= MC_1_21_1
			return context.tickCounter().getTickDelta(false);
		#else
			return context.tickDelta();
		#endif
	}

	public void setupOpaqueState(CapturedGlState state) {
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		state.setFramebuffer(glID(framebuffer));
		state.setViewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
		state.setCullFace(true);
		state.setCullFaceMode(GL_BACK);
		state.setDepthRead(true);
		state.setDepthWrite(true);
		state.setBlend(false);
		state.setColorMask(true, true, true, true);
	}

	public void setupUniforms(WorldRenderContext context, VanillaLodShader shader, FogParams fog) {
		this.matrixStorage.set(
			context
			.projectionMatrix()
			.mul(
				#if MC_VERSION >= MC_1_20_5
					context.positionMatrix(),
				#else
					context.matrixStack().peek().getPositionMatrix(),
				#endif
				this.matrixStorage.scratch
			)
		);
		nglUniformMatrix4fv(shader.modelViewProjectionMatrix, 1, false, this.matrixStorage.address());
		glUniform3f(shader.fogColor, fog.red, fog.green, fog.blue);
		float globalFogDensity = BigGlobeConfig.INSTANCE.get().lodRendering.fogDensity;
		float fogHeightScale = BigGlobeConfig.INSTANCE.get().lodRendering.fogHeightScale;
		ClientGeneratorParams params = ClientState.generatorParams;
		if (params != null && params.seaLevel != null && fogHeightScale != 0.0F) {
			double seaLevel = params.seaLevel.doubleValue();
			double cameraY = context.camera().getPos().y;
			double worldMaxY = HeightLimitViewVersions.getMaxY(context.world());
			float tickProgress = tickProgress(context);
			float rainStrength = context.world().getRainGradient(tickProgress);
			float thunderStrength = context.world().getThunderGradient(tickProgress);
			glUniform3f(
				shader.fogParams,
				(float)(cameraY - seaLevel),
				-fogHeightScale / ((float)(worldMaxY - seaLevel)),
				Interpolator.mixSmoothUnchecked(
					-1.0F,
					Interpolator.mixSmoothUnchecked(-2.0F, -4.0F, thunderStrength),
					rainStrength
				)
				* globalFogDensity / fog.farPlaneDistance
			);
		}
		else {
			glUniform3f(shader.fogParams, 0.0F, 0.0F, -globalFogDensity / fog.farPlaneDistance);
		}
	}

	@Override
	public void oom() {
		this.heap.cleanup();
	}

	@Override
	public void appendTextToF3Menu(List<String> lines) {
		long reallyUsed = this.heap.reallyUsed();
		long used = this.heap.used();
		long fragmentation = used == 0L ? 0L : 100L - reallyUsed * 100L / used;
		long capacity = this.heap.capacity;
		long percent = used * 100L / capacity;
		long elements = this.elementBuffer.capacity;
		lines.add("[BG] Vertices: U: " + reallyUsed + ", A: " + used + ", C: " + capacity + ", F: " + fragmentation + "%, P: " + percent + '%' + ", E: " + elements);
	}

	public static class CapturedGlState implements SafeCloseable {

		public static final int
			CHANGED_FRAMEBUFFER    = 1 << 0,
			CHANGED_VIEWPORT       = 1 << 1,
			CHANGED_CULL_FACE      = 1 << 2,
			CHANGED_DEPTH_TEST     = 1 << 3,
			CHANGED_DEPTH_MASK     = 1 << 4,
			CHANGED_BLEND          = 1 << 5,
			CHANGED_CULL_FACE_MODE = 1 << 6,
			CHANGED_BLEND_FUNC     = 1 << 7,
			CHANGED_COLOR_MASK     = 1 << 8,
			CHANGED_PROGRAM        = 1 << 9,
			CHANGED_VAO            = 1 << 10,
			CHANGED_ELEMENT_BUFFER = 1 << 11;

		public int changed;
		public int framebuffer;
		public int[] viewport = new int[4];
		public boolean cullFace, depthTest, depthMask, blend;
		public int cullFaceMode;
		public int blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha;
		public int[] colorMask = new int[4];
		public int activeTexture;
		public int program;
		public int vao, elementBuffer;
		public boolean inTranslucentPass;

		public void setChanged(int flag) {
			this.changed |= flag;
		}

		public boolean hasChanged(int flag) {
			return (this.changed & flag) != 0;
		}

		public void capture() {
			this.changed = 0;
			this.inTranslucentPass = false;
			this.framebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
			GLException.check();
			glGetIntegerv(GL_VIEWPORT, this.viewport);
			GLException.check();
			this.cullFace = glIsEnabled(GL_CULL_FACE);
			GLException.check();
			this.cullFaceMode = glGetInteger(GL_CULL_FACE_MODE);
			GLException.check();
			glGetIntegerv(GL_COLOR_WRITEMASK, this.colorMask);
			GLException.check();
			this.depthTest = glIsEnabled(GL_DEPTH_TEST);
			GLException.check();
			this.depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
			GLException.check();
			this.blend = glIsEnabled(GL_BLEND);
			GLException.check();
			this.blendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB);
			GLException.check();
			this.blendDstRgb = glGetInteger(GL_BLEND_DST_RGB);
			GLException.check();
			this.blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
			GLException.check();
			this.blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
			GLException.check();
			this.activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
			GLException.check();
			this.program = glGetInteger(GL_CURRENT_PROGRAM);
			GLException.check();
			this.vao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
			GLException.check();
			this.elementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
			GLException.check();
		}

		public void setVao(int vao) {
			if (this.hasChanged(CHANGED_VAO) || this.vao != vao) {
				this.setChanged(CHANGED_VAO);
				glBindVertexArray(vao);
				GLException.check();
			}
		}

		public void setElementBuffer(int elementBuffer) {
			if (this.hasChanged(CHANGED_ELEMENT_BUFFER) || this.elementBuffer != elementBuffer) {
				this.setChanged(CHANGED_ELEMENT_BUFFER);
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
				GLException.check();
			}
		}

		public void setFramebuffer(int framebuffer) {
			if (this.hasChanged(CHANGED_FRAMEBUFFER) || this.framebuffer != framebuffer) {
				this.setChanged(CHANGED_FRAMEBUFFER);
				glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
				GLException.check();
			}
		}

		public void setViewport(int x, int y, int width, int height) {
			if (this.hasChanged(CHANGED_VIEWPORT) || x != this.viewport[0] || y != this.viewport[1] || width != this.viewport[2] || height != this.viewport[3]) {
				this.setChanged(CHANGED_VIEWPORT);
				glViewport(x, y, width, height);
				GLException.check();
			}
		}

		public void setCullFace(boolean cullFace) {
			if (this.hasChanged(CHANGED_CULL_FACE) || this.cullFace != cullFace) {
				this.setChanged(CHANGED_CULL_FACE);
				setEnabled(GL_CULL_FACE, cullFace);
				GLException.check();
			}
		}

		public void setCullFaceMode(int mode) {
			if (this.hasChanged(CHANGED_CULL_FACE_MODE) || this.cullFaceMode != mode) {
				this.setChanged(CHANGED_CULL_FACE_MODE);
				glCullFace(mode);
				GLException.check();
			}
		}

		public void setDepthRead(boolean depthRead) {
			if (this.hasChanged(CHANGED_DEPTH_TEST) || this.depthTest != depthRead) {
				this.setChanged(CHANGED_DEPTH_TEST);
				setEnabled(GL_DEPTH_TEST, depthRead);
				GLException.check();
			}
		}

		public void setDepthWrite(boolean depthWrite) {
			if (this.hasChanged(CHANGED_DEPTH_MASK) || this.depthMask != depthWrite) {
				this.setChanged(CHANGED_DEPTH_MASK);
				glDepthMask(depthWrite);
				GLException.check();
			}
		}

		public void setBlend(boolean blend) {
			if (this.hasChanged(CHANGED_BLEND) || this.blend != blend) {
				this.setChanged(CHANGED_BLEND);
				setEnabled(GL_BLEND, blend);
				GLException.check();
			}
		}

		public void setBlendFunc(int blendSrcRgb, int blendDstRgb, int blendSrcAlpha, int blendDstAlpha) {
			if (this.hasChanged(CHANGED_BLEND_FUNC) || this.blendSrcRgb != blendSrcRgb || this.blendDstRgb != blendDstRgb || this.blendSrcAlpha != blendSrcAlpha || this.blendDstAlpha != blendDstAlpha) {
				this.setChanged(CHANGED_BLEND_FUNC);
				glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
				GLException.check();
			}
		}

		@SuppressWarnings("DoubleNegation")
		public void setColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
			if (this.hasChanged(CHANGED_COLOR_MASK) || (this.colorMask[0] != 0) != red || (this.colorMask[1] != 0) != green || (this.colorMask[2] != 0) != blue || (this.colorMask[3] != 0) != alpha) {
				this.setChanged(CHANGED_COLOR_MASK);
				glColorMask(red, green, blue, alpha);
				GLException.check();
			}
		}

		public void setProgram(int program) {
			if (this.hasChanged(CHANGED_PROGRAM) || this.program != program) {
				this.setChanged(CHANGED_PROGRAM);
				glUseProgram(program);
				GLException.check();
			}
		}

		public void restore() {
			GLException.check();
			if (this.hasChanged(CHANGED_ELEMENT_BUFFER)) {
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.elementBuffer);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_VAO)) {
				glBindVertexArray(this.vao);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_PROGRAM)) {
				glUseProgram(this.program);
				GLException.check();
			}
			glActiveTexture(this.activeTexture);
			GLException.check();
			if (this.hasChanged(CHANGED_BLEND_FUNC)) {
				glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_BLEND_FUNC)) {
				glColorMask(this.colorMask[0] != 0, this.colorMask[1] != 0, this.colorMask[2] != 0, this.colorMask[3] != 0);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_BLEND)) {
				setEnabled(GL_BLEND, this.blend);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_DEPTH_MASK)) {
				glDepthMask(this.depthMask);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_DEPTH_TEST)) {
				setEnabled(GL_DEPTH_TEST, this.depthTest);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_CULL_FACE_MODE)) {
				glCullFace(this.cullFaceMode);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_CULL_FACE)) {
				setEnabled(GL_CULL_FACE, this.cullFace);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_VIEWPORT)) {
				glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
				GLException.check();
			}
			if (this.hasChanged(CHANGED_FRAMEBUFFER)) {
				glBindFramebuffer(GL_FRAMEBUFFER, this.framebuffer);
				GLException.check();
			}
		}

		public static void setEnabled(int flag, boolean enabled) {
			if (enabled) glEnable(flag); else glDisable(flag);
		}

		@Override
		public void close() {
			if (this.inTranslucentPass) {
				glClear(GL_DEPTH_BUFFER_BIT);
				GLException.check();
				this.restore();
			}
		}
	}

	public static class TextureState {

		public int binder, bindQuery, index, texture;

		public TextureState(int binder, int bindQuery, int index) {
			this.binder = binder;
			this.bindQuery = bindQuery;
			this.index = index;
		}

		public static TextureState buffer(int index) {
			return new TextureState(GL_TEXTURE_BUFFER, GL_TEXTURE_BINDING_BUFFER, index);
		}

		public static TextureState _2D(int index) {
			return new FilteredTextureState(GL_TEXTURE_2D, GL_TEXTURE_BINDING_2D, index);
		}

		public void capture() {
			glActiveTexture(this.index);
			GLException.check();
			this.texture = glGetInteger(this.bindQuery);
			GLException.check();
		}

		public void restore() {
			glActiveTexture(this.index);
			GLException.check();
			glBindTexture(this.binder, this.texture);
			GLException.check();
		}
	}

	public static class FilteredTextureState extends TextureState {

		public int minFilter, magFilter;

		public FilteredTextureState(int binder, int bindQuery, int index) {
			super(binder, bindQuery, index);
		}

		@Override
		public void capture() {
			super.capture();
			this.minFilter = glGetTexParameteri(this.binder, GL_TEXTURE_MIN_FILTER);
			GLException.check();
			this.magFilter = glGetTexParameteri(this.binder, GL_TEXTURE_MAG_FILTER);
			GLException.check();
		}

		@Override
		public void restore() {
			super.restore();
			glTexParameteri(this.binder, GL_TEXTURE_MIN_FILTER, this.minFilter);
			GLException.check();
			glTexParameteri(this.binder, GL_TEXTURE_MAG_FILTER, this.magFilter);
			GLException.check();
		}
	}
}