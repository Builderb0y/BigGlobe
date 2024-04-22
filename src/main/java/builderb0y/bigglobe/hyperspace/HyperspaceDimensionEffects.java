package builderb0y.bigglobe.hyperspace;

import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;

public class HyperspaceDimensionEffects extends DimensionEffects {

	public HyperspaceDimensionEffects() {
		super(Float.NaN, false, DimensionEffects.SkyType.NONE, true, false);
	}

	public static void init() {
		DimensionRenderingRegistry.registerDimensionEffects(BigGlobeMod.modID("hyperspace"), new HyperspaceDimensionEffects());
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