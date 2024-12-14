package builderb0y.bigglobe.hyperspace;

import java.util.Iterator;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering.VisibleWaypointData;
import builderb0y.bigglobe.mixins.PostEffectProcessor_PassesAccess;

import static builderb0y.bigglobe.hyperspace.HyperspaceRendering.*;

//I would use fabric's rendering API for this,
//but WorldRenderContext doesn't have the frameGraphBuilder, which I need.
public class VanillaHyperspaceRendering {

	public static void renderHyperspaceSkybox(
		FrameGraphBuilder frameGraphBuilder,
		float tickDelta,
		DefaultFramebufferSet framebufferSet
	) {
		PostEffectProcessor shader = MinecraftClient.getInstance().getShaderLoader().loadPostEffect(BigGlobeMod.modID("hyperspace_skybox"), DefaultFramebufferSet.MAIN_ONLY);
		if (shader != null) {
			Matrix4f
				inverseModelView = RenderSystem.getModelViewMatrix().invert(new Matrix4f()),
				inverseProjection = RenderSystem.getProjectionMatrix().invert(new Matrix4f());

			for (PostEffectPass pass : ((PostEffectProcessor_PassesAccess)(shader)).bigglobe_getPasses()) {
				GlUniform uniform;

				uniform = pass.getProgram().getUniform("ModelViewInverse");
				if (uniform != null) uniform.set(inverseModelView);

				uniform = pass.getProgram().getUniform("ProjMatInverse");
				if (uniform != null) uniform.set(inverseProjection);

				uniform = pass.getProgram().getUniform("cameraPosition");
				if (uniform != null) uniform.set((float)(cameraPosition.x), (float)(cameraPosition.y), (float)(cameraPosition.z));

				uniform = pass.getProgram().getUniform("time");
				if (uniform != null) uniform.set(time(tickDelta));
			}

			shader.render(frameGraphBuilder, MinecraftClient.getInstance().getFramebuffer().textureWidth, MinecraftClient.getInstance().getFramebuffer().textureHeight, framebufferSet);
		}
	}

	public static void renderWaypoints(
		FrameGraphBuilder frameGraphBuilder,
		float tickDelta,
		DefaultFramebufferSet frameBufferSet
	) {
		if (!visibleWaypoints.isEmpty()) {
			PostEffectProcessor shader = MinecraftClient.getInstance().getShaderLoader().loadPostEffect(BigGlobeMod.modID("waypoint_warp"), DefaultFramebufferSet.MAIN_ONLY);
			if (shader != null) {
				for (PostEffectPass pass : ((PostEffectProcessor_PassesAccess)(shader)).bigglobe_getPasses()) {
					GlUniform uniform;

					uniform = pass.getProgram().getUniform("ActualProjMat");
					if (uniform != null) uniform.set(projection);

					uniform = pass.getProgram().getUniform("ModelViewMat");
					if (uniform != null) uniform.set(modelView);

					uniform = pass.getProgram().getUniform("time");
					if (uniform != null) uniform.set(time(tickDelta));

					uniform = pass.getProgram().getUniform("bigglobe_waypoint_count");
					if (uniform != null) {
						Vector4f position = new Vector4f();
						int count = 0;
						for (Iterator<VisibleWaypointData> iterator = visibleWaypoints.descendingIterator(); iterator.hasNext(); ) {
							VisibleWaypointData waypoint = iterator.next();
							position.set(
								waypoint.x() - cameraPosition.x,
								waypoint.y() - cameraPosition.y + 1.0D,
								waypoint.z() - cameraPosition.z,
								1.0F
							);
							modelView.transform(position);
							GlUniform positionUniform = pass.getProgram().getUniform("bigglobe_waypoint_" + count++);
							if (positionUniform != null) {
								positionUniform.set(position.x, position.y, position.z, waypoint.health() / WaypointEntity.MAX_HEALTH + (float)(Math.sin((waypoint.age() + tickDelta) * (Math.PI / 50.0D)) * 0.125D));
							}
						}
						uniform.set(count);
					}
				}
				shader.render(frameGraphBuilder, MinecraftClient.getInstance().getFramebuffer().textureWidth, MinecraftClient.getInstance().getFramebuffer().textureHeight, frameBufferSet);
			}
		}
	}
}