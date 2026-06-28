package builderb0y.bigglobe.rendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(EnvType.CLIENT)
public class CommonState {

	public static final Matrix4f
		modelViewMatrix         = new Matrix4f(),
		inverseModelViewMatrix  = new Matrix4f(),
		projectionMatrix        = new Matrix4f(),
		inverseProjectionMatrix = new Matrix4f();
	public static float
		partialTicks,
		dayTimeInSeconds;

	static {
		LevelRenderEvents.END_EXTRACTION.register((LevelExtractionContext context) -> {
			partialTicks = context.deltaTracker().getGameTimeDeltaPartialTick(false);
			dayTimeInSeconds = (((float)(context.level().getGameTime() % 24000L)) + partialTicks) / 20.0F;
		});
	}

	public static void setMatrices(Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix) {
		CommonState.modelViewMatrix.set(modelViewMatrix).invert(inverseModelViewMatrix);
		CommonState.projectionMatrix.set(projectionMatrix).invert(inverseProjectionMatrix);
	}
}