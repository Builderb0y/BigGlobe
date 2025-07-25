package builderb0y.bigglobe.rendering.hyperspace;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering;
import builderb0y.bigglobe.rendering.*;
import builderb0y.bigglobe.versions.RenderVersions;

import static org.lwjgl.opengl.GL32C.*;

public class HyperspaceRenderer implements SafeCloseable {

	public static HyperspaceRenderer INSTANCE;

	public static void init() {
		try {
			INSTANCE = new HyperspaceRenderer();
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Hyperspace rendering unavailable:", exception);
		}
	}

	public HyperspaceBackgroundShader shader;
	public EmptyVertexArray vertices;
	public MatrixStorageWorkaround matrices;
	public GlState state;

	@Override
	public void close() {
		ResourceTracker.closeAll(this.shader, this.vertices, this.matrices);
	}

	public HyperspaceRenderer() {
		String message = GLException.checkMessage();
		if (message != null) BigGlobeMod.LOGGER.warn("A GLException occurred just before setting up the hyperspace renderer: " + message);
		try {
			this.shader = new HyperspaceBackgroundShader();
			this.vertices = new EmptyVertexArray();
			this.matrices = new MatrixStorageWorkaround();
			this.state = new GlState();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	public void draw() {
		this.state.capture();
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		int framebufferID = RenderVersions.glID(framebuffer);
		this.state.setFramebuffer(framebufferID);
		this.state.setViewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
		this.state.setVao(this.vertices.vao);
		this.state.setCullFace(false);
		this.state.setDepthRead(false);
		this.state.setDepthWrite(false);
		this.state.setBlend(false);
		this.state.setColorMask(true, true, true, true);
		this.state.setProgram(this.shader.program);
		this.matrices.set(HyperspaceRendering.modelViewInverse);
		nglUniformMatrix4fv(this.shader.inverseModelView, 1, false, this.matrices.address());
		this.matrices.set(HyperspaceRendering.projectionInverse);
		nglUniformMatrix4fv(this.shader.inverseProjection, 1, false, this.matrices.address());
		glUniform3f(this.shader.cameraPosition, (float)(HyperspaceRendering.cameraPosition.x), (float)(HyperspaceRendering.cameraPosition.y), (float)(HyperspaceRendering.cameraPosition.z));
		glUniform1f(this.shader.time, HyperspaceRendering.time);
		glDrawArrays(GL_TRIANGLES, 0, 3);
		this.state.restore();
	}
}