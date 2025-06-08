package builderb0y.bigglobe.lods;

import builderb0y.autocodec.util.AutoCodecUtil;

import static org.lwjgl.opengl.GL32C.*;

public class ScratchDepthBuffer implements SafeCloseable {

	public int fbo;
	public int depthTex;
	public int width, height;

	public ScratchDepthBuffer() {
		try {
			this.fbo = glGenFramebuffers();
			this.depthTex = glGenTextures();
			int oldFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
			try {
				int oldTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
				try {
					glBindTexture(GL_TEXTURE_2D, this.depthTex);
					GLException.check();
					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
					GLException.check();
					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
					GLException.check();

					glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
					GLException.check();
					glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.depthTex, 0);
					GLException.check();
				}
				finally {
					glBindTexture(GL_TEXTURE_2D, oldTexture);
					GLException.check();
				}
			}
			finally {
				glBindFramebuffer(GL_FRAMEBUFFER, oldFramebuffer);
				GLException.check();
			}
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	@Override
	public void close() {
		if (this.fbo != 0) { glDeleteFramebuffers(this.fbo); this.fbo = 0; }
		if (this.depthTex >= 0) { glDeleteTextures(this.depthTex); this.depthTex = -1; }
		this.width = this.height = 0;
	}

	public void copyFrom(int from, int width, int height) {
		int oldReadBuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
		GLException.check();
		try {
			if (oldReadBuffer != from) {
				glBindFramebuffer(GL_READ_FRAMEBUFFER, from);
				GLException.check();
			}
			int oldDrawBuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
			GLException.check();
			try {
				if (oldDrawBuffer != this.fbo) {
					glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fbo);
					GLException.check();
				}
				if (this.width != width || this.height != height) {
					this.width = width;
					this.height = height;
					int oldTexture2D = glGetInteger(GL_TEXTURE_BINDING_2D);
					try {
						glBindTexture(GL_TEXTURE_2D, this.depthTex);
						GLException.check();
						glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
						GLException.check();
					}
					finally {
						glBindTexture(GL_TEXTURE_2D, oldTexture2D);
						GLException.check();
					}
				}
				glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
				GLException.check();
			}
			finally {
				glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDrawBuffer);
				GLException.check();
			}
		}
		finally {
			glBindFramebuffer(GL_READ_FRAMEBUFFER, oldReadBuffer);
			GLException.check();
		}
	}

	public void copyTo(int to) {
		int oldReadBuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
		GLException.check();
		try {
			if (oldReadBuffer != this.fbo) {
				glBindFramebuffer(GL_READ_FRAMEBUFFER, this.fbo);
				GLException.check();
			}
			int oldDrawBuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
			GLException.check();
			try {
				if (oldDrawBuffer != this.fbo) {
					glBindFramebuffer(GL_DRAW_FRAMEBUFFER, to);
					GLException.check();
				}
				glBlitFramebuffer(0, 0, this.width, this.height, 0, 0, this.width, this.height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
				GLException.check();
			}
			finally {
				glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDrawBuffer);
				GLException.check();
			}
		}
		finally {
			glBindFramebuffer(GL_READ_FRAMEBUFFER, oldReadBuffer);
			GLException.check();
		}
	}
}