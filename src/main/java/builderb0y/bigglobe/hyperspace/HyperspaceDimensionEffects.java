package builderb0y.bigglobe.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;

@Environment(EnvType.CLIENT)
public class HyperspaceDimensionEffects extends DimensionEffects {

	public HyperspaceDimensionEffects() {
		#if MC_VERSION >= MC_1_21_6
			super(SkyType.NONE, true, false);
		#else
			super(Float.NaN, false, DimensionEffects.SkyType.NONE, true, false);
		#endif
	}

	public static void init() {
		#if MC_VERSION >= MC_1_21_9
			DimensionEffects.BY_IDENTIFIER.put(BigGlobeMod.modID("hyperspace"), new HyperspaceDimensionEffects());
		#else
			net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry.registerDimensionEffects(BigGlobeMod.modID("hyperspace"), new HyperspaceDimensionEffects());
			net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry.registerCloudRenderer(HyperspaceConstants.WORLD_KEY, context -> {});
		#endif
	}

	@Override
	public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
		return Vec3d.ZERO;
	}

	@Override
	public boolean useThickFog(int camX, int camY) {
		return false;
	}

	#if MC_VERSION < MC_1_21_2

		@Override
		public float @Nullable [] getFogColorOverride(float skyAngle, float tickDelta) {
			return null;
		}

	#endif
}