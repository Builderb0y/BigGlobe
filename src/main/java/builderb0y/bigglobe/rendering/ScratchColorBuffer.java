package builderb0y.bigglobe.rendering;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.util.SafeCloseable;

import static org.lwjgl.opengl.GL32C.*;

public class ScratchColorBuffer implements SafeCloseable {

	public int fbo;
	public int colorTex;
	public int width, height;

	public ScratchColorBuffer() {
		try {
			this.fbo = glGenFramebuffers();
			this.colorTex = glGenTextures();
			int oldFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
			try {
				int oldTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
				try {
					glBindTexture(GL_TEXTURE_2D, this.colorTex);
					GLException.check();
					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
					GLException.check();
					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
					GLException.check();

					glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
					GLException.check();
					glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorTex, 0);
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
		if (this.colorTex >= 0) { glDeleteTextures(this.colorTex); this.colorTex = -1; }
		this.width = this.height = 0;
	}

	public void ensureSize(int width, int height) {
		if (this.width != width || this.height != height) {
			this.width = width;
			this.height = height;
			int oldTexture2D = glGetInteger(GL_TEXTURE_BINDING_2D);
			try {
				glBindTexture(GL_TEXTURE_2D, this.colorTex);
				GLException.check();
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
				GLException.check();
				glGenerateMipmap(GL_TEXTURE_2D);
				GLException.check();
			}
			finally {
				glBindTexture(GL_TEXTURE_2D, oldTexture2D);
				GLException.check();
			}
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
				if (oldDrawBuffer != to) {
					glBindFramebuffer(GL_DRAW_FRAMEBUFFER, to);
					GLException.check();
				}
				glBlitFramebuffer(0, 0, this.width, this.height, 0, 0, this.width, this.height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
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