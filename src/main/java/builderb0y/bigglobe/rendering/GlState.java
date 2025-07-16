package builderb0y.bigglobe.rendering;

import static org.lwjgl.opengl.GL32C.*;

public class GlState {

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

	public void setChanged(int flag) {
		this.changed |= flag;
	}

	public boolean hasChanged(int flag) {
		return (this.changed & flag) != 0;
	}

	public void capture() {
		this.changed = 0;
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
}