package builderb0y.bigglobe.compat.satin;

import java.util.Iterator;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Vector4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.InstalledMods;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering.VisibleWaypointData;

#if MC_VERSION >= MC_1_21_0
	import org.ladysnake.satin.api.event.PostWorldRenderCallback;
	import org.ladysnake.satin.api.managed.ManagedShaderEffect;
	import org.ladysnake.satin.api.managed.ShaderEffectManager;
	import org.ladysnake.satin.api.managed.uniform.*;
#else
	import ladysnake.satin.api.event.PostWorldRenderCallback;
	import ladysnake.satin.api.managed.ManagedShaderEffect;
	import ladysnake.satin.api.managed.ShaderEffectManager;
	import ladysnake.satin.api.managed.uniform.*;
#endif

import static builderb0y.bigglobe.hyperspace.HyperspaceRendering.*;

@Environment(EnvType.CLIENT)
public class SatinCompat {

	public static final boolean ENABLED;

	static {
		boolean enabled = InstalledMods.SATIN;
		if (enabled && InstalledMods.IMMERSIVE_PORTALS) {
			BigGlobeMod.LOGGER.warn("Fancy waypoint renderer disabled because it conflicts with Immersive Portals.");
			enabled = false;
		}
		ENABLED = enabled;
	}

	public static void init() {
		#if MC_VERSION < MC_1_21_2
			if (ENABLED) try {
				SatinCode.init();
			}
			catch (LinkageError error) {
				BigGlobeMod.LOGGER.error("Failed to setup satin integration. Waypoints and hyperspace will look boring.", error);
			}
		#endif
	}

	@Environment(EnvType.CLIENT)
	public static class SatinCode {

		@Environment(EnvType.CLIENT)
		public static class WaypointWarp {

			public static final ManagedShaderEffect
				SHADER = ShaderEffectManager.getInstance().manage(BigGlobeMod.modID("shaders/post/waypoint_warp.json"));
			public static final UniformMat4
				ACTUAL_PROJ_MAT = SHADER.findUniformMat4("ActualProjMat"),
				MODEL_VIEW_MAT  = SHADER.findUniformMat4("ModelViewMat");
			public static final Uniform1f
				TIME = SHADER.findUniform1f("time");
			//satin does not provide uniform arrays, so I have to make 16 different uniforms instead.
			public static final Uniform1i
				COUNT = SHADER.findUniform1i("bigglobe_waypoint_count");
			public static final Uniform4f[] POSITIONS = {
				SHADER.findUniform4f("bigglobe_waypoint_0"),
				SHADER.findUniform4f("bigglobe_waypoint_1"),
				SHADER.findUniform4f("bigglobe_waypoint_2"),
				SHADER.findUniform4f("bigglobe_waypoint_3"),
				SHADER.findUniform4f("bigglobe_waypoint_4"),
				SHADER.findUniform4f("bigglobe_waypoint_5"),
				SHADER.findUniform4f("bigglobe_waypoint_6"),
				SHADER.findUniform4f("bigglobe_waypoint_7"),
				SHADER.findUniform4f("bigglobe_waypoint_8"),
				SHADER.findUniform4f("bigglobe_waypoint_9"),
				SHADER.findUniform4f("bigglobe_waypoint_10"),
				SHADER.findUniform4f("bigglobe_waypoint_11"),
				SHADER.findUniform4f("bigglobe_waypoint_12"),
				SHADER.findUniform4f("bigglobe_waypoint_13"),
				SHADER.findUniform4f("bigglobe_waypoint_14"),
				SHADER.findUniform4f("bigglobe_waypoint_15"),
			};
		}

		@Environment(EnvType.CLIENT)
		public static class HyperspaceSkybox {

			public static final ManagedShaderEffect
				SHADER = ShaderEffectManager.getInstance().manage(BigGlobeMod.modID("shaders/post/hyperspace_skybox.json"));
			public static final UniformMat4
				PROJ_MAT_INVERSE = SHADER.findUniformMat4("ProjMatInverse"),
				MODEL_VIEW_INVERSE = SHADER.findUniformMat4("ModelViewInverse");
			public static final Uniform3f
				CAMERA_POSITION = SHADER.findUniform3f("cameraPosition");
			public static final Uniform1f
				TIME = SHADER.findUniform1f("time");
		}

		public static void init() {
			PostWorldRenderCallback.EVENT.register((Camera camera, float tickDelta #if MC_VERSION < MC_1_21_0 , long nanoTime #endif) -> {
				if (!visibleWaypoints.isEmpty()) {
					WaypointWarp.TIME.set(time(tickDelta));
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
						RenderSystem.getModelViewMatrix().transform(position);
						WaypointWarp.POSITIONS[count++].set(position.x, position.y, position.z, waypoint.health() / WaypointEntity.MAX_HEALTH + (float)(Math.sin((waypoint.age() + tickDelta) * (Math.PI / 50.0D)) * 0.125D));
					}
					WaypointWarp.COUNT.set(count);
					WaypointWarp.ACTUAL_PROJ_MAT.set(projection);
					WaypointWarp.MODEL_VIEW_MAT.set(modelView);
					WaypointWarp.SHADER.render(tickDelta);
				}
			});
			DimensionRenderingRegistry.registerSkyRenderer(HyperspaceConstants.WORLD_KEY, (WorldRenderContext context) -> {
				HyperspaceSkybox.PROJ_MAT_INVERSE.set(projectionInverse);
				HyperspaceSkybox.MODEL_VIEW_INVERSE.set(modelViewInverse);
				Vec3d pos = context.camera().getPos();
				HyperspaceSkybox.CAMERA_POSITION.set((float)(pos.x), (float)(pos.y), (float)(pos.z));

				float tickDelta = (
					#if MC_VERSION >= MC_1_21_5
						context.tickCounter().getTickProgress(false)
					#elif MC_VERSION >= MC_1_21_0
						context.tickCounter().getTickDelta(false)
					#else
						context.tickDelta()
					#endif
				);
				HyperspaceSkybox.TIME.set(time(tickDelta));
				HyperspaceSkybox.SHADER.render(tickDelta);
			});
		}
	}
}