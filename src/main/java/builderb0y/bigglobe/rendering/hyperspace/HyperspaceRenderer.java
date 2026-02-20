package builderb0y.bigglobe.rendering.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.rendering.*;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.versions.RenderVersions;

import static org.lwjgl.opengl.GL32C.*;

@Environment(EnvType.CLIENT)
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

	public static void debug_reload() {
		if (INSTANCE != null) {
			INSTANCE.close();
		}
		try {
			INSTANCE = new HyperspaceRenderer();
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Hyperspace reload failed:", exception);
		}
	}

	public HyperspaceBackgroundShader shader;
	public EmptyVertexArray vertices;
	public MatrixStorageWorkaround matrices;
	public GlState glState;

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
			this.glState = new GlState();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	public void draw() {
		String existingMessage = GLException.checkMessage();
		if (existingMessage != null) {
			BigGlobeMod.LOGGER.warn("Caught GL exception from some other unknown mod right before hyperspace background rendering: " + existingMessage);
		}
		try {
			this.doDraw();
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("An exception occurred while rendering the hyperspace background effect. The hyperspace background effect will now disable itself to prevent further problems.", exception);
			this.close();
			INSTANCE = null;
		}
	}

	public void doDraw() {
		this.glState.capture();
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		int framebufferID = RenderVersions.glID(framebuffer);
		this.glState.setFramebuffer(framebufferID);
		this.glState.setViewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
		this.glState.setVao(this.vertices.vao);
		this.glState.setCullFace(false);
		this.glState.setDepthRead(false);
		this.glState.setDepthWrite(false);
		this.glState.setBlend(false);
		this.glState.setColorMask(true, true, true, true);
		this.glState.setProgram(this.shader.program);
		this.matrices.set(Matrices.modelViewInverse);
		nglUniformMatrix4fv(this.shader.inverseModelView, 1, false, this.matrices.address());
		this.matrices.set(Matrices.projectionInverse);
		nglUniformMatrix4fv(this.shader.inverseProjection, 1, false, this.matrices.address());
		glUniform3f(this.shader.cameraPosition, (float)(Matrices.cameraX), (float)(Matrices.cameraY), (float)(Matrices.cameraZ));
		glUniform1f(this.shader.time, Matrices.dayTimeInSeconds);
		float progress = 0;
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null) {
			PlayerWaypointManager manager = PlayerWaypointManager.get(player);
			if (manager != null) {
				progress = manager.collapseProgress + (
					manager.getAllWaypoints().isEmpty()
					? +Matrices.partialTicks
					: -Matrices.partialTicks
				);
			}
		}
		glUniform1f(this.shader.collapse, progress / PlayerWaypointManager.COLLAPSE_DURATION_TICKS);
		glDrawArrays(GL_TRIANGLES, 0, 3);
		this.glState.restore();
	}
}