package builderb0y.bigglobe.rendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.RenderVersions;

@Environment(EnvType.CLIENT)
public class Matrices {

	public static double
		cameraX,
		cameraY,
		cameraZ;
	public static Matrix4f
		modelView         = new Matrix4f(),
		modelViewInverse  = new Matrix4f(),
		projection        = new Matrix4f(),
		projectionInverse = new Matrix4f();
	public static float
		dayTimeInSeconds;

	public static void init() {
		#if MC_VERSION >= MC_1_21_9
			net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_EXTRACTION.register(Matrices::update);
		#else
			net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.START.register(Matrices::update);
		#endif
	}

	public static void update(
		#if MC_VERSION >= MC_1_21_9
			net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext context
		#else
			net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context
		#endif
	) {
		Vec3d cameraPos = context.camera().getPos();
		cameraX = cameraPos.x;
		cameraY = cameraPos.y;
		cameraZ = cameraPos.z;
		modelView.set(RenderVersions.modelViewMatrix(context)).invert(modelViewInverse);
		projection.set(RenderVersions.projectionMatrix(context)).invert(projectionInverse);
		dayTimeInSeconds = (
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