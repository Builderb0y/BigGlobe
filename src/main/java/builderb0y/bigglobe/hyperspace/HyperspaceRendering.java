package builderb0y.bigglobe.hyperspace;

import java.util.TreeSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.RenderVersions;

@Environment(EnvType.CLIENT)
public class HyperspaceRendering {

	public static Vec3d cameraPosition = Vec3d.ZERO;
	public static Matrix4f
		modelView         = new Matrix4f(),
		modelViewInverse  = new Matrix4f(),
		projection        = new Matrix4f(),
		projectionInverse = new Matrix4f();
	public static float time, partialTicks;

	public static final TreeSet<VisibleWaypointData> visibleWaypoints = new TreeSet<>();

	static {
		WorldRenderEvents.START.register(HyperspaceRendering::beginFrame);
		WorldRenderEvents.END.register((WorldRenderContext context) -> endFrame());
	}

	public static void markWaypointVisible(double x, double y, double z, int age, float health) {
		visibleWaypoints.add(new VisibleWaypointData(x, y, z, age, health));
		if (visibleWaypoints.size() > 16) {
			visibleWaypoints.pollLast();
		}
	}

	public static void beginFrame(WorldRenderContext context) {
		cameraPosition = context.camera().getPos();
		modelView.set(RenderVersions.modelViewMatrix(context)).invert(modelViewInverse);
		projection.set(context.projectionMatrix()).invert(projectionInverse);
		time = computeTime(context);
		partialTicks = RenderVersions.partialTicks(context);
	}

	public static void endFrame() {
		visibleWaypoints.clear();
	}

	@Environment(EnvType.CLIENT)
	public static record VisibleWaypointData(double x, double y, double z, int age, float health) implements Comparable<VisibleWaypointData> {

		public double squareDistanceToCamera() {
			return BigGlobeMath.squareD(
				this.x - cameraPosition.x,
				this.y - cameraPosition.y + 1.0D,
				this.z - cameraPosition.z
			);
		}

		@Override
		public int compareTo(@NotNull VisibleWaypointData that) {
			return Double.compare(this.squareDistanceToCamera(), that.squareDistanceToCamera());
		}
	}

	public static float computeTime(WorldRenderContext context) {
		return (
			(
				(
					(float)(
						BigGlobeMath.modulus_BP(
							context.world().getTime(),
							24000L
						)
					)
				)
				+ RenderVersions.partialTicks(context)
			)
			/ 20.0F
		);
	}
}