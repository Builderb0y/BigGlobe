package builderb0y.bigglobe.versions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import org.joml.Matrix4fc;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class RenderVersions {

	public static float partialTicks(LevelExtractionContext context) {
		return context.deltaTracker().getGameTimeDeltaPartialTick(false);
	}

	public static boolean isTranslucent(RenderType layer) {
		return layer.sortOnUpload();
	}

	public static Vec3 getCameraPosition(Camera camera) {
		return camera.position();
	}
}