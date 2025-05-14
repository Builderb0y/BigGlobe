package builderb0y.bigglobe.lods;

import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;

import static org.lwjgl.opengl.GL32C.*;

@Environment(EnvType.CLIENT)
public class DefaultLodRenderer implements LodRenderer {

	public int program, fragmentStage, vertexStage;
	public VertexHeap heap;
	public ElementBuffer elementBuffer;
	public int vao;
	public int modelOffset, modelViewProjectionMatrix, blockAtlas, lightmap;
	public NativeMemory matrixStorage;
	public CapturedGlState state;

	@Override
	public void close() {
		if (this.fragmentStage != 0) { glDeleteShader(this.fragmentStage); this.fragmentStage = 0; }
		if (this.vertexStage != 0) { glDeleteShader(this.vertexStage); this.vertexStage = 0; }
		if (this.program != 0) { glDeleteProgram(this.program); this.program = 0; }
		if (this.vao != 0) { glDeleteVertexArrays(this.vao); this.vao = 0; }
		ResourceTracker.closeAll(Arrays.asList(this.heap, this.elementBuffer, this.matrixStorage));
	}

	public DefaultLodRenderer(int quadCount) {
		if (quadCount < 0 || quadCount >= ((int)((1L << 32) / 4L))) {
			throw new IllegalArgumentException("Quad count out of range: " + quadCount);
		}
		this.fragmentStage = glCreateShader(GL_FRAGMENT_SHADER);
		this.vertexStage = glCreateShader(GL_VERTEX_SHADER);
		this.program = glCreateProgram();
		this.heap = new VertexHeap(LodVertexFormat.FORMAT, quadCount);
		this.elementBuffer = new ElementBuffer();
		this.vao = glGenVertexArrays();
		this.matrixStorage = new NativeMemory(16 * Float.BYTES);
		this.state = new CapturedGlState();

		try {
			this.recompile();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}

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
	}

	/** separate method to allow me to hotswap and call it with a debugger. */
	public void recompile() {
		glShaderSource(
			this.fragmentStage,
			//language=glsl
			"""
			#version 150
			
			uniform sampler2D blockAtlas;
			uniform sampler2D lightmap;
			
			in vec4 tint;
			in vec2 texcoord;
			in vec2 lmcoord;
			
			out vec4 color;
			
			void main() {
				color = (
					//texture(lightmap, gl_FragCoord.xy / vec2(1536.0, 896.0))
					texture(blockAtlas, texcoord) *
					texture(lightmap, lmcoord) *
					tint
				);
				if (color.a < 0.1) discard;
			}
			"""
		);
		glCompileShader(this.fragmentStage);
		String log = glGetShaderInfoLog(this.fragmentStage);
		if (glGetShaderi(this.fragmentStage, GL_COMPILE_STATUS) != GL_TRUE) {
			throw new RuntimeException(log);
		}
		else if (!log.isEmpty()) {
			BigGlobeMod.LOGGER.warn("Possible problems compiling LOD fragment shader:");
			BigGlobeMod.LOGGER.warn(log);
		}

		glShaderSource(
			this.vertexStage,
			//language=glsl
			"#version 150\n" +
			"#define MIN_LOD " + LodQuadTree.MIN_LEVEL + '\n' +
			"""
			uniform mat4 modelViewProjectionMatrix;
			uniform vec4 modelOffset;
			
			in uvec2 horizontalPosition;
			in int verticalPosition;
			in vec4 colorData;
			in vec2 texcoordData;
			in vec2 lightData;
			
			out vec4 tint;
			out vec2 texcoord;
			out vec2 lmcoord;
			
			void main() {
				vec3 modelPos;
				modelPos.xz = vec2(horizontalPosition) * (float(1 << MIN_LOD) / 128.0) - 64.0 * (float(1 << MIN_LOD) / 128.0);
				modelPos.y = float(verticalPosition) * (4096.0 / 32768.0);
				gl_Position = modelViewProjectionMatrix * vec4(modelPos * modelOffset.w + modelOffset.xyz, 1.0);
				tint = colorData;
				texcoord = texcoordData * (1.0 / 65536.0);
				lmcoord = lightData;
			}
			"""
		);
		glCompileShader(this.vertexStage);
		log = glGetShaderInfoLog(this.vertexStage);
		if (glGetShaderi(this.vertexStage, GL_COMPILE_STATUS) != GL_TRUE) {
			throw new RuntimeException(log);
		}
		else if (!log.isEmpty()) {
			BigGlobeMod.LOGGER.warn("Possible problems compiling LOD vertex shader:");
			BigGlobeMod.LOGGER.warn(log);
		}

		glAttachShader(this.program, this.fragmentStage);
		glAttachShader(this.program, this.vertexStage);
		glLinkProgram(this.program);
		log = glGetProgramInfoLog(this.program);
		if (glGetProgrami(this.program, GL_LINK_STATUS) != GL_TRUE) {
			throw new RuntimeException(log);
		}
		else if (!log.isEmpty()) {
			BigGlobeMod.LOGGER.warn("Possible problems linking LOD shader:");
			BigGlobeMod.LOGGER.warn(log);
		}

		this.modelViewProjectionMatrix = glGetUniformLocation(this.program, "modelViewProjectionMatrix");
		this.blockAtlas = glGetUniformLocation(this.program, "blockAtlas");
		this.lightmap = glGetUniformLocation(this.program, "lightmap");
		this.modelOffset = glGetUniformLocation(this.program, "modelOffset");
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

	@Override
	public SafeCloseable bind(WorldRenderContext context, boolean translucent) {
		if (translucent) {
			this.state.inTranslucentPass = true;
			glEnable(GL_BLEND);
			glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
		}
		else {
			this.state.capture();

			glBindVertexArray(this.vao);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.elementBuffer.glID);

			Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
			glBindFramebuffer(GL_FRAMEBUFFER, glID(framebuffer));
			glViewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
			glEnable(GL_CULL_FACE);
			glCullFace(GL_BACK);
			glEnable(GL_DEPTH_TEST);
			glDisable(GL_BLEND);
			glColorMask(true, true, true, true);
			glDepthMask(true);

			glUseProgram(this.program);
			glUniform1i(this.blockAtlas, 0);
			glUniform1i(this.lightmap, 2);

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, glID(MinecraftClient.getInstance().getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)));
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, glID(MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager()));
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			context
			.projectionMatrix()
			.mul(
				#if MC_VERSION >= MC_1_20_5
					context.positionMatrix(),
				#else
					context.matrixStack().peek().getPositionMatrix(),
				#endif
				new Matrix4f()
			)
			.getToAddress(this.matrixStorage.address);
			nglUniformMatrix4fv(this.modelViewProjectionMatrix, 1, false, this.matrixStorage.address);
		}
		return this.state;
	}

	@Override
	public VertexConsumerProvider beginMeshing() {
		return new LodPasses.Builder(LodVertexFormat.FORMAT, LodGenerator.RENDER_AREA /* expected columns */ * 6 /* expected quads per column */ * 4 /* vertices per quad */);
	}

	@Override
	public MeshUploader finishMeshing() {
		SafeCloseable unbinder = this.heap.bind();
		return new MeshUploader() {

			@Override
			public SafeCloseable upload(VertexConsumerProvider provider) {
				LodPasses.Builder builder = (LodPasses.Builder)(provider);
				return builder.build(DefaultLodRenderer.this.heap);
			}

			@Override
			public void close() {
				unbinder.close();
			}
		};
	}

	@Override
	public void endMeshing(VertexConsumerProvider provider) {
		LodPasses.Builder builder = (LodPasses.Builder)(provider);
		builder.close();
	}

	@Override
	public void oom() {
		this.heap.cleanup();
	}

	@Override
	public void draw(
		SafeCloseable token,
		float modelOffsetX,
		float modelOffsetY,
		float modelOffsetZ,
		float scale
	) {
		LodPasses passes = (LodPasses)(token);
		LodPasses.Geometry geometry = passes.getGeometry(this.state.inTranslucentPass);
		if (geometry != null) {
			glUniform4f(this.modelOffset, modelOffsetX, modelOffsetY, modelOffsetZ, scale);
			this.elementBuffer.ensureCapacity(geometry.indexCount());
			nglDrawElementsBaseVertex(GL_TRIANGLES, geometry.indexCount(), GL_UNSIGNED_INT, 0L, geometry.baseVertex());
		}
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

		public int framebuffer;
		public int[] viewport = new int[4];
		public boolean cullFace, depthTest, depthMask, blend;
		public int cullFaceMode;
		public int blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha;
		public int[] colorMask = new int[4];
		public int activeTexture, texture0, texture2;
		public int tex0Min, tex0Mag, tex2Min, tex2Mag;
		public int program;
		public int vao, elementBuffer;
		public boolean inTranslucentPass;

		public void capture() {
			this.inTranslucentPass = false;
			this.framebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
			glGetIntegerv(GL_VIEWPORT, this.viewport);
			this.cullFace = glIsEnabled(GL_CULL_FACE);
			this.cullFaceMode = glGetInteger(GL_CULL_FACE_MODE);
			glGetIntegerv(GL_COLOR_WRITEMASK, this.colorMask);
			this.depthTest = glIsEnabled(GL_DEPTH_TEST);
			this.depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
			this.blend = glIsEnabled(GL_BLEND);
			this.blendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB);
			this.blendDstRgb = glGetInteger(GL_BLEND_DST_RGB);
			this.blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
			this.blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
			this.activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
			glActiveTexture(GL_TEXTURE0);
			this.texture0 = glGetInteger(GL_TEXTURE_BINDING_2D);
			this.tex0Min = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER);
			this.tex0Mag = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER);
			glActiveTexture(GL_TEXTURE2);
			this.texture2 = glGetInteger(GL_TEXTURE_BINDING_2D);
			this.tex2Min = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER);
			this.tex2Mag = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER);
			this.program = glGetInteger(GL_CURRENT_PROGRAM);
			this.vao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
			this.elementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
		}

		public void restore() {
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.elementBuffer);
			glBindVertexArray(this.vao);
			glUseProgram(this.program);
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, this.texture2);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, this.tex2Min);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, this.tex2Mag);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, this.texture0);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, this.tex0Min);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, this.tex0Mag);
			glActiveTexture(this.activeTexture);
			glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
			setEnabled(GL_BLEND, this.blend);
			glDepthMask(this.depthMask);
			setEnabled(GL_DEPTH_TEST, this.depthTest);
			glColorMask(this.colorMask[0] != 0, this.colorMask[1] != 0, this.colorMask[2] != 0, this.colorMask[3] != 0);
			glCullFace(this.cullFaceMode);
			setEnabled(GL_CULL_FACE, this.cullFace);
			glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
			glBindFramebuffer(GL_FRAMEBUFFER, this.framebuffer);
		}

		public static void setEnabled(int flag, boolean enabled) {
			if (enabled) glEnable(flag); else glDisable(flag);
		}

		@Override
		public void close() {
			if (this.inTranslucentPass) {
				glClear(GL_DEPTH_BUFFER_BIT);
				this.restore();
			}
		}
	}
}