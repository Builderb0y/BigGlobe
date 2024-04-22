package builderb0y.bigglobe.compat.satin;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.satin.api.event.PostWorldRenderCallback;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import ladysnake.satin.api.managed.uniform.Uniform1i;
import ladysnake.satin.api.managed.uniform.Uniform4f;
import ladysnake.satin.api.managed.uniform.UniformMat4;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.math.BigGlobeMath;

public class SatinCompat {

	public static final boolean ENABLED = FabricLoader.getInstance().isModLoaded("satin");
	public static Vec3d cameraPosition = Vec3d.ZERO;
	public static final TreeSet<WaypointEntity> visibleWaypoints = ENABLED ? new TreeSet<>(
		Comparator.comparingDouble((WaypointEntity entity) -> {
			return BigGlobeMath.squareD(
				entity.getX() - cameraPosition.x,
				entity.getY() + 1.0D - cameraPosition.y,
				entity.getZ() - cameraPosition.z
			);
		})
		.thenComparingInt(WaypointEntity::getId)
	)
	: null;

	public static void init() {
		if (ENABLED) try {
			SatinCode.init();
		}
		catch (LinkageError error) {
			BigGlobeMod.LOGGER.error("Failed to setup satin integration. Waypoints will be boring.", error);
		}
		else {
			BigGlobeMod.LOGGER.info("Satin is not installed. Waypoints will be boring.");
		}
	}

	public static void markWaypointRendered(WaypointEntity entity) {
		if (ENABLED) {
			visibleWaypoints.add(entity);
			if (visibleWaypoints.size() > 16) {
				visibleWaypoints.pollLast();
			}
		}
	}

	public static class SatinCode {

		public static final ManagedShaderEffect WAYPOINT_WARP = ShaderEffectManager.getInstance().manage(BigGlobeMod.modID("shaders/post/waypoint_warp.json"));
		public static final UniformMat4
			ACTUAL_PROJ_MAT = WAYPOINT_WARP.findUniformMat4("ActualProjMat"),
			MODEL_VIEW_MAT  = WAYPOINT_WARP.findUniformMat4("ModelViewMat");
		//satin does not provide uniform arrays, so I have to make 16 different uniforms instead.
		public static final Uniform1i COUNT = WAYPOINT_WARP.findUniform1i("bigglobe_waypoint_count");
		public static final Uniform4f[] POSITIONS = {
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_0"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_1"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_2"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_3"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_4"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_5"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_6"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_7"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_8"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_9"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_10"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_11"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_12"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_13"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_14"),
			WAYPOINT_WARP.findUniform4f("bigglobe_waypoint_15"),
		};

		public static void init() {
			WorldRenderEvents.BEFORE_ENTITIES.register((WorldRenderContext context) -> {
				cameraPosition = context.camera().getPos();
			});
			PostWorldRenderCallback.EVENT.register((Camera camera, float tickDelta, long nanoTime) -> {
				if (!visibleWaypoints.isEmpty()) {
					Vector4f position = new Vector4f();
					int count = 0;
					for (Iterator<WaypointEntity> iterator = visibleWaypoints.descendingIterator(); iterator.hasNext(); ) {
						WaypointEntity waypoint = iterator.next();
						position.set(
							waypoint.getX() - cameraPosition.x,
							waypoint.getY() + 1.0D - cameraPosition.y,
							waypoint.getZ() - cameraPosition.z,
							1.0F
						);
						RenderSystem.getModelViewMatrix().transform(position);
						POSITIONS[count++].set(position.x, position.y, position.z, waypoint.getHealth() / WaypointEntity.MAX_HEALTH);
					}
					COUNT.set(count);
					ACTUAL_PROJ_MAT.set(RenderSystem.getProjectionMatrix());
					MODEL_VIEW_MAT.set(RenderSystem.getModelViewMatrix());
					WAYPOINT_WARP.render(tickDelta);
					visibleWaypoints.clear();
				}
			});
		}
	}
}