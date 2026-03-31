package builderb0y.bigglobe.rendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.RenderVersions;

@Environment(EnvType.CLIENT)
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
		LevelRenderEvents.END_EXTRACTION.register(Matrices::update);
	}

	public static void update(LevelExtractionContext context) {
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
							context.level().getGameTime(),
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