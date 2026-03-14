package builderb0y.bigglobe.rendering.waypoints;

import java.util.TreeSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.entity.Entity;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.entities.WaypointEntityRenderer;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.rendering.*;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.versions.RenderVersions;

import static org.lwjgl.opengl.GL32C.*;

public class WaypointWarpRenderer implements SafeCloseable {

	public static WaypointWarpRenderer INSTANCE;
	public static void init() {
		try {
			INSTANCE = new WaypointWarpRenderer();
			#if MC_VERSION >= MC_1_21_9
				net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_EXTRACTION.register(context -> {
					WaypointWarpRenderer renderer = INSTANCE;
					if (renderer != null) {
						for (net.minecraft.client.render.entity.state.EntityRenderState entity : context.worldState().entityRenderStates) {
							if (entity instanceof WaypointEntityRenderer.State waypoint) {
								renderer.markWaypointVisible(waypoint.x, waypoint.y, waypoint.z, waypoint.age, waypoint.health);
							}
						}
					}
				});
				net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_MAIN.register(context -> {
					WaypointWarpRenderer renderer = INSTANCE;
					if (renderer != null) {
						renderer.draw();
						renderer.endFrame();
					}
				});
			#else
				net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
					WaypointWarpRenderer renderer = INSTANCE;
					if (renderer != null) {
						#if MC_VERSION >= MC_1_21_2
							for (Entity entity : context.worldRenderer().renderedEntities) {
								if (entity instanceof WaypointEntity waypoint) {
									renderer.markWaypointVisible(waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.age, waypoint.health);
								}
							}
						#endif
						if (!renderer.visibleWaypoints.isEmpty()) {
							renderer.draw();
							renderer.endFrame();
						}
					}
				});
			#endif
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Waypoint warp renderer unavailable:", exception);
		}
	}

	public static void debug_reload() {
		if (INSTANCE != null) {
			INSTANCE.close();
			INSTANCE = null;
		}
		try {
			INSTANCE = new WaypointWarpRenderer();
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Waypoint warp reload failed:", exception);
		}
	}

	public ScratchColorBuffer framebuffer;
	public WaypointWarpShader shader;
	public EmptyVertexArray vertices;
	public MatrixStorageWorkaround matrices;
	public NativeMemory waypointData;
	public WaypointWarpGlState glState;
	public TreeSet<VisibleWaypointData> visibleWaypoints;

	@Override
	public void close() {
		ResourceTracker.closeAll(this.framebuffer, this.shader, this.vertices, this.matrices, this.waypointData);
	}

	public WaypointWarpRenderer() {
		String message = GLException.checkMessage();
		if (message != null) BigGlobeMod.LOGGER.warn("A GLException occurred just before setting up the waypoint warp renderer: " + message);
		try {
			this.framebuffer = new ScratchColorBuffer();
			this.shader = new WaypointWarpShader();
			this.vertices = new EmptyVertexArray();
			this.matrices = new MatrixStorageWorkaround();
			this.waypointData = new NativeMemory(16 * 4 * Float.BYTES);
			this.glState = new WaypointWarpGlState();
			this.visibleWaypoints = new TreeSet<>();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	public void draw() {
		String existingMessage = GLException.checkMessage();
		if (existingMessage != null) {
			BigGlobeMod.LOGGER.warn("Caught GL exception from some other unknown mod right before waypoint warp rendering: " + existingMessage);
		}
		try {
			this.doDraw();
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("An exception occurred while rendering the waypoint warp effect. The waypoint warp effect will now disable itself to prevent further problems.", exception);
			this.close();
			INSTANCE = null;
		}
	}

	public void doDraw() {
		this.glState.capture();
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		this.framebuffer.ensureSize(framebuffer.textureWidth, framebuffer.textureHeight);
		this.glState.setFramebuffer(this.framebuffer.fbo);
		this.glState.setViewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
		this.glState.setVao(this.vertices.vao);
		this.glState.setCullFace(false);
		this.glState.setDepthRead(false);
		this.glState.setDepthWrite(false);
		this.glState.setBlend(false);
		this.glState.setColorMask(true, true, true, true);
		this.glState.setProgram(this.shader.program);
		this.glState.colortex.set(RenderVersions.colorAttachment(framebuffer), GL_LINEAR, GL_LINEAR, GL_MIRRORED_REPEAT, GL_MIRRORED_REPEAT);
		glUniform1i(this.shader.colortex, 0);
		this.glState.depthtex.set(RenderVersions.depthAttachment(framebuffer), GL_LINEAR, GL_LINEAR, GL_MIRRORED_REPEAT, GL_MIRRORED_REPEAT);
		glUniform1i(this.shader.depthtex, 1);
		this.matrices.set(Matrices.modelView);
		nglUniformMatrix4fv(this.shader.modelViewMatrix, 1, false, this.matrices.address());
		this.matrices.set(Matrices.modelViewInverse);
		nglUniformMatrix4fv(this.shader.inverseModelViewMatrix, 1, false, this.matrices.address());
		this.matrices.set(Matrices.projection);
		nglUniformMatrix4fv(this.shader.projectionMatrix, 1, false, this.matrices.address());
		this.matrices.set(Matrices.projectionInverse);
		nglUniformMatrix4fv(this.shader.inverseProjectionMatrix, 1, false, this.matrices.address());
		glUniform1f(this.shader.time, Matrices.dayTimeInSeconds);
		int waypointCount = this.visibleWaypoints.size();
		glUniform1i(this.shader.waypointCount, waypointCount);
		for (VisibleWaypointData waypoint : this.visibleWaypoints) {
			this.waypointData.appendFloat((float)(waypoint.x()));
			this.waypointData.appendFloat((float)(waypoint.y() + 1.0D));
			this.waypointData.appendFloat((float)(waypoint.z()));
			this.waypointData.appendFloat(waypoint.health() / WaypointEntity.MAX_HEALTH + (float)(Math.sin(waypoint.age() * (Math.PI / 50.0D)) * 0.125D));
		}
		nglUniform4fv(this.shader.waypoints, waypointCount, this.waypointData.address);
		this.waypointData.clear();
		GLException.check();
		glDrawArrays(GL_TRIANGLES, 0, 3);
		GLException.check();
		this.framebuffer.copyTo(RenderVersions.glID(framebuffer));
		GLException.check();
		this.glState.restore();
	}

	public void markWaypointVisible(double x, double y, double z, float age, float health) {
		this.visibleWaypoints.add(new VisibleWaypointData(x - Matrices.cameraX, y - Matrices.cameraY, z - Matrices.cameraZ, age, health));
		if (this.visibleWaypoints.size() > 16) {
			this.visibleWaypoints.pollLast();
		}
	}

	public void endFrame() {
		this.visibleWaypoints.clear();
	}

	public static class WaypointWarpGlState extends GlState {

		public FilteredTextureState
			colortex = TextureState._2D(GL_TEXTURE0),
			depthtex = TextureState._2D(GL_TEXTURE1);

		@Override
		public void capture() {
			super.capture();
			this.colortex.capture();
			this.depthtex.capture();
		}

		@Override
		public void restore() {
			this.depthtex.restore();
			this.colortex.restore();
			super.restore();
		}
	}

	@Environment(EnvType.CLIENT)
	public static record VisibleWaypointData(double x, double y, double z, float age, float health) implements Comparable<VisibleWaypointData> {

		public double squareDistanceToCamera() {
			return BigGlobeMath.squareD(this.x, this.y + 1.0D, this.z);
		}

		@Override
		public int compareTo(@NotNull VisibleWaypointData that) {
			return Double.compare(this.squareDistanceToCamera(), that.squareDistanceToCamera());
		}
	}
}