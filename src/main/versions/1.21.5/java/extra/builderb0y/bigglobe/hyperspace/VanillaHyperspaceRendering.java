package builderb0y.bigglobe.hyperspace;

import java.util.Iterator;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering.VisibleWaypointData;

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
			shader.render(
				frameGraphBuilder,
				MinecraftClient.getInstance().getFramebuffer().textureWidth,
				MinecraftClient.getInstance().getFramebuffer().textureHeight,
				framebufferSet,
				(RenderPass pass) -> {
					pass.setUniform("ModelViewInverse", inverseModelView);
					pass.setUniform("ProjMatInverse", inverseProjection);
					pass.setUniform("cameraPosition", (float)(cameraPosition.x), (float)(cameraPosition.y), (float)(cameraPosition.z));
					pass.setUniform("time", time(tickDelta));
				}
			);
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
				shader.render(
					frameGraphBuilder,
					MinecraftClient.getInstance().getFramebuffer().textureWidth,
					MinecraftClient.getInstance().getFramebuffer().textureHeight,
					frameBufferSet,
					(RenderPass pass) -> {
						pass.setUniform("ActualProjMat", projection);
						pass.setUniform("ModelViewMat", modelView);
						pass.setUniform("time", time(tickDelta));
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
							pass.setUniform("bigglobe_waypoint_" + count++, position.x, position.y, position.z, waypoint.health() / WaypointEntity.MAX_HEALTH + (float)(Math.sin((waypoint.age() + tickDelta) * (Math.PI / 50.0D)) * 0.125D));
						}
						pass.setUniform("bigglobe_waypoint_count", count);
					}
				);
			}
		}
	}
}