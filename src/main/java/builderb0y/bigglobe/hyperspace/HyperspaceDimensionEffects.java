package builderb0y.bigglobe.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;

@Environment(EnvType.CLIENT)
public class HyperspaceDimensionEffects extends DimensionEffects {

	public HyperspaceDimensionEffects() {
		super(Float.NaN, false, DimensionEffects.SkyType.NONE, true, false);
	}

	public static void init() {
		DimensionRenderingRegistry.registerDimensionEffects(BigGlobeMod.modID("hyperspace"), new HyperspaceDimensionEffects());
		DimensionRenderingRegistry.registerCloudRenderer(HyperspaceConstants.WORLD_KEY, (WorldRenderContext context) -> {});
	}

	@Override
	public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
		return Vec3d.ZERO;
	}

	@Override
	public boolean useThickFog(int camX, int camY) {
		return false;
	}

	@Override
	public float @Nullable [] getFogColorOverride(float skyAngle, float tickDelta) {
		return null;
	}
}