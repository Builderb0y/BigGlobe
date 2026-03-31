package builderb0y.bigglobe.rendering2;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import org.joml.Matrix4f;

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
			modelViewMatrix.set(context.levelState().cameraRenderState.viewRotationMatrix).invert(inverseModelViewMatrix);
			projectionMatrix.set(context.levelState().cameraRenderState.projectionMatrix).invert(inverseProjectionMatrix);
			partialTicks = context.deltaTracker().getGameTimeDeltaPartialTick(false);
			dayTimeInSeconds = (((float)(context.level().getGameTime() % 24000L)) + partialTicks) / 20.0F;
		});
	}
}