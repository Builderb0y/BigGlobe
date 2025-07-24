package builderb0y.bigglobe.rendering.waypoints;

import org.lwjgl.system.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering.VisibleWaypointData;
import builderb0y.bigglobe.rendering.*;
import builderb0y.bigglobe.versions.RenderVersions;

import static org.lwjgl.opengl.GL32C.*;

public class WaypointWarpRenderer implements SafeCloseable {

	public static WaypointWarpRenderer INSTANCE;
	public static void init() {
		try {
			INSTANCE = new WaypointWarpRenderer();
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Waypoint warp renderer unavailable:", exception);
		}
	}

	public ScratchColorBuffer framebuffer;
	public WaypointWarpShader shader;
	public EmptyVertexArray vertices;
	public MatrixStorageWorkaround matrices;
	public NativeMemory waypointData;
	public WaypointWarpGlState state;

	@Override
	public void close() {
		ResourceTracker.closeAll(this.framebuffer, this.shader, this.vertices, this.matrices, this.waypointData);
	}

	public WaypointWarpRenderer() {
		try {
			this.framebuffer = new ScratchColorBuffer();
			this.shader = new WaypointWarpShader();
			this.vertices = new EmptyVertexArray();
			this.matrices = new MatrixStorageWorkaround();
			this.waypointData = new NativeMemory(16 * 4 * Float.BYTES);
			this.state = new WaypointWarpGlState();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	public void draw() {
		this.state.capture();
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		this.framebuffer.ensureSize(framebuffer.textureWidth, framebuffer.textureHeight);
		this.state.setFramebuffer(this.framebuffer.fbo);
		this.state.setViewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
		this.state.setVao(this.vertices.vao);
		this.state.setCullFace(false);
		this.state.setDepthRead(false);
		this.state.setDepthWrite(false);
		this.state.setBlend(false);
		this.state.setColorMask(true, true, true, true);
		this.state.setProgram(this.shader.program);
		this.state.colortex.set(RenderVersions.colorAttachment(framebuffer), GL_LINEAR, GL_LINEAR, GL_MIRRORED_REPEAT, GL_MIRRORED_REPEAT);
		glUniform1i(this.shader.colortex, 0);
		this.state.depthtex.set(RenderVersions.depthAttachment(framebuffer), GL_LINEAR, GL_LINEAR, GL_MIRRORED_REPEAT, GL_MIRRORED_REPEAT);
		glUniform1i(this.shader.depthtex, 1);
		this.matrices.set(HyperspaceRendering.modelView);
		nglUniformMatrix4fv(this.shader.modelViewMatrix, 1, false, this.matrices.address());
		this.matrices.set(HyperspaceRendering.modelViewInverse);
		nglUniformMatrix4fv(this.shader.inverseModelViewMatrix, 1, false, this.matrices.address());
		this.matrices.set(HyperspaceRendering.projection);
		nglUniformMatrix4fv(this.shader.projectionMatrix, 1, false, this.matrices.address());
		this.matrices.set(HyperspaceRendering.projectionInverse);
		nglUniformMatrix4fv(this.shader.inverseProjectionMatrix, 1, false, this.matrices.address());
		glUniform1f(this.shader.time, HyperspaceRendering.time);
		int waypointCount = HyperspaceRendering.visibleWaypoints.size();
		glUniform1i(this.shader.waypointCount, waypointCount);
		for (VisibleWaypointData waypoint : HyperspaceRendering.visibleWaypoints) {
			this.waypointData.appendFloat((float)(waypoint.x() - HyperspaceRendering.cameraPosition.x));
			this.waypointData.appendFloat((float)(waypoint.y() - HyperspaceRendering.cameraPosition.y + 1.0D));
			this.waypointData.appendFloat((float)(waypoint.z() - HyperspaceRendering.cameraPosition.z));
			this.waypointData.appendFloat(waypoint.health() / WaypointEntity.MAX_HEALTH + (float)(Math.sin((waypoint.age() + HyperspaceRendering.partialTicks) * (Math.PI / 50.0D)) * 0.125D));
		}
		nglUniform4fv(this.shader.waypoints, waypointCount, this.waypointData.address);
		this.waypointData.clear();
		GLException.check();
		glDrawArrays(GL_TRIANGLES, 0, 3);
		GLException.check();
		this.framebuffer.copyTo(RenderVersions.glID(framebuffer));
		GLException.check();
		this.state.restore();
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
}