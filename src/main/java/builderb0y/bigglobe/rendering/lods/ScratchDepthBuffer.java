package builderb0y.bigglobe.rendering.lods;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.rendering.GLException;
import builderb0y.bigglobe.util.SafeCloseable;

import static org.lwjgl.opengl.GL32C.*;

/**
the situation:

before LODs render, it is assumed that the depth
buffer is empty (containing 1.0 everywhere).
after LODs render, the depth buffer needs to be empty again,
so that vanilla terrain can render on top of the LODs.
it would be trivially easy to just clear the depth buffer for this.
however, immersive portals breaks these assumptions.
it requires some non-one data to be inside the depth buffer before rendering,
and I need to not mess with that data as an effect of rendering.
nevertheless, my rendering requires depth testing and depth writing to be enabled.
the solution I came up with was to simply copy the depth
buffer before rendering, and then copy it back after rendering.
but then this tactic started causing a small amount of graphics
corruption in MC 1.21.5, and it only got worse in 1.21.6.
so my new (temporary) solution is to use the old clear trick in MC 1.21.5+,
and use the copying trick in MC 1.21.4-.
immersive portals isn't updated past MC 1.21.1 yet anyway.
*/
public abstract class ScratchDepthBuffer implements SafeCloseable {

	public abstract void copyFrom(int from, int width, int height);

	public abstract void copyTo(int to);

	public static class RealScratchDepthBuffer extends ScratchDepthBuffer {

		public int fbo;
		public int depthTex;
		public int width, height;

		public RealScratchDepthBuffer() {
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
			if (this.fbo != 0) {
				glDeleteFramebuffers(this.fbo);
				this.fbo = 0;
			}
			if (this.depthTex >= 0) {
				glDeleteTextures(this.depthTex);
				this.depthTex = -1;
			}
			this.width = this.height = 0;
		}

		@Override
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

		@Override
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

	public static class FakeScratchDepthBuffer extends ScratchDepthBuffer {

		@Override
		public void copyFrom(int from, int width, int height) {
			//no-op.
		}

		@Override
		public void copyTo(int to) {
			int oldDrawBuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
			GLException.check();
			try {
				if (oldDrawBuffer != to) {
					glBindFramebuffer(GL_DRAW_FRAMEBUFFER, to);
					GLException.check();
				}
				glClear(GL_DEPTH_BUFFER_BIT);
				GLException.check();
			}
			finally {
				glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDrawBuffer);
				GLException.check();
			}
		}

		@Override
		public void close() {
			//no-op.
		}
	}
}