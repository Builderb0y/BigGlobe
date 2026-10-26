package builderb0y.bigglobe.rendering;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;
import builderb0y.bigglobe.platform.fabric.fabric.api.client.rendering.v1.WorldRenderContext;
import builderb0y.bigglobe.platform.fabric.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.RenderVersions;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public class Matrices {

	public static double
		cameraX,
		cameraY,
		cameraZ;
	public static Matrix4f
		modelView = new Matrix4f(),
		modelViewInverse = new Matrix4f(),
		projection = new Matrix4f(),
		projectionInverse = new Matrix4f();
	public static float
		partialTicks,
		dayTimeInSeconds;

	public static void init() {

		WorldRenderEvents.AFTER_SETUP.register(Matrices::update);
	}

	public static void update(

		WorldRenderContext context

	) {
		Vec3 cameraPos = RenderVersions.getCameraPosition(context.camera());
		cameraX = cameraPos.x;
		cameraY = cameraPos.y;
		cameraZ = cameraPos.z;
		modelView.set(RenderVersions.modelViewMatrix(context)).invert(modelViewInverse);
		projection.set(RenderVersions.projectionMatrix(context)).invert(projectionInverse);
		partialTicks = RenderVersions.partialTicks(context);
		dayTimeInSeconds = (
			(
				(
					(float)(
						BigGlobeMath.modulus_BP(
							context.world().getGameTime(),
							24000L
						)
					)
				)
				+ partialTicks
			)
			/ 20.0F
		);
	}
}