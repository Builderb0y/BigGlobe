package builderb0y.bigglobe.rendering.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.rendering.*;
import builderb0y.bigglobe.rendering.ScratchColorBuffer;
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
			return;
		}
		ClientTickEvents.START_WORLD_TICK.register((ClientWorld world) -> {
			//instance can be re-nullified if it encounters an error while rendering.
			if (INSTANCE != null && world.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
				INSTANCE.noiseShader.tick();
			}
		});
	}

	public static void debug_reload() {
		if (INSTANCE != null) {
			INSTANCE.close();
			INSTANCE = null;
		}
		try {
			INSTANCE = new HyperspaceRenderer();
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Hyperspace reload failed:", exception);
		}
	}

	public HyperspaceVolumetricNoiseShader noiseShader;
	public HorizontalBlurPass horizontalBlur;
	public VerticalBlurPass verticalBlur;
	public HyperspaceBackgroundShader effects;
	public ScratchColorBuffer pingPong;
	public EmptyVertexArray vertices;
	public MatrixStorageWorkaround matrices;
	public HyperspaceGlState glState;

	@Override
	public void close() {
		ResourceTracker.closeAll(
			this.noiseShader,
			this.horizontalBlur,
			this.verticalBlur,
			this.effects,
			this.pingPong,
			this.vertices,
			this.matrices
		);
	}

	public HyperspaceRenderer() {
		String message = GLException.checkMessage();
		if (message != null) BigGlobeMod.LOGGER.warn("A GLException occurred just before setting up the hyperspace renderer: " + message);
		try {
			this.noiseShader = new HyperspaceVolumetricNoiseShader();
			this.horizontalBlur = new HorizontalBlurPass();
			this.verticalBlur = new VerticalBlurPass();
			this.effects = new HyperspaceBackgroundShader();
			this.pingPong = new ScratchColorBuffer();
			this.vertices = new EmptyVertexArray();
			this.matrices = new MatrixStorageWorkaround();
			this.glState = new HyperspaceGlState();
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
		//capture
		this.glState.capture();

		//setup
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		this.pingPong.ensureSize(framebuffer.textureWidth, framebuffer.textureHeight);
		int framebufferID = RenderVersions.glID(framebuffer);
		this.glState.setFramebuffer(this.pingPong.fbo);
		this.glState.setViewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
		this.glState.setVao(this.vertices.vao);
		this.glState.setCullFace(false);
		this.glState.setDepthRead(false);
		this.glState.setDepthWrite(false);
		this.glState.setBlend(false);
		this.glState.setColorMask(true, true, true, true);

		//volumetric noise pass
		this.glState.setProgram(this.noiseShader.program);
		this.matrices.set(Matrices.modelViewInverse);
		nglUniformMatrix4fv(this.noiseShader.inverseModelView, 1, false, this.matrices.address());
		this.matrices.set(Matrices.projectionInverse);
		nglUniformMatrix4fv(this.noiseShader.inverseProjection, 1, false, this.matrices.address());
		glUniform3f(this.noiseShader.cameraPosition, (float)(Matrices.cameraX), (float)(Matrices.cameraY), (float)(Matrices.cameraZ));
		glUniform1f(this.noiseShader.time, Matrices.dayTimeInSeconds);
		glUniform3f(this.noiseShader.beamOrigin, this.noiseShader.pendingX, this.noiseShader.pendingY, this.noiseShader.pendingZ);
		glUniform1f(this.noiseShader.beamTime, this.noiseShader.pendingTime());
		glUniform1f(this.noiseShader.beamDirectionSeed, this.noiseShader.pendingSeed);
		glDrawArrays(GL_TRIANGLES, 0, 3);

		//horizontal blur pass
		this.glState.setFramebuffer(framebufferID);
		this.glState.setProgram(this.horizontalBlur.program);
		this.glState.previousPass.set(this.pingPong.colorTex, GL_LINEAR, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_CLAMP_TO_EDGE);
		glUniform1i(this.horizontalBlur.previousPass, 0);
		glUniform2i(this.horizontalBlur.resolution, framebuffer.textureWidth, framebuffer.textureHeight);
		glDrawArrays(GL_TRIANGLES, 0, 3);

		//vertical blur pass
		this.glState.setFramebuffer(this.pingPong.fbo);
		this.glState.setProgram(this.verticalBlur.program);
		this.glState.previousPass.set(RenderVersions.colorAttachment(framebuffer), GL_LINEAR, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_CLAMP_TO_EDGE);
		glUniform1i(this.verticalBlur.previousPass, 0);
		glUniform2i(this.verticalBlur.resolution, framebuffer.textureWidth, framebuffer.textureHeight);
		glDrawArrays(GL_TRIANGLES, 0, 3);

		//fractal and collapse pass
		this.glState.setFramebuffer(framebufferID);
		this.glState.setProgram(this.effects.program);
		this.glState.previousPass.set(this.pingPong.colorTex, GL_LINEAR, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_CLAMP_TO_EDGE);
		this.matrices.set(Matrices.modelViewInverse);
		nglUniformMatrix4fv(this.effects.inverseModelView, 1, false, this.matrices.address());
		this.matrices.set(Matrices.projectionInverse);
		nglUniformMatrix4fv(this.effects.inverseProjection, 1, false, this.matrices.address());
		glUniform3f(this.effects.cameraPosition, (float)(Matrices.cameraX), (float)(Matrices.cameraY), (float)(Matrices.cameraZ));
		glUniform1f(this.effects.time, Matrices.dayTimeInSeconds);
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
		glUniform1f(this.effects.collapse, progress / PlayerWaypointManager.COLLAPSE_DURATION_TICKS);
		glDrawArrays(GL_TRIANGLES, 0, 3);

		//restore
		this.glState.restore();
	}

	public static class HyperspaceGlState extends GlState {

		public FilteredTextureState previousPass = TextureState._2D(GL_TEXTURE0);

		@Override
		public void capture() {
			super.capture();
			this.previousPass.capture();
		}

		@Override
		public void restore() {
			this.previousPass.restore();
			super.restore();
		}
	}
}