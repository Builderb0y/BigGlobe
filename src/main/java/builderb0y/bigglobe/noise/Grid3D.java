package builderb0y.bigglobe.noise;

import org.jetbrains.annotations.UnknownNullability;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.noise.processing.*;
import builderb0y.bigglobe.noise.resample.*;
import builderb0y.bigglobe.noise.resample.derivatives.*;
import builderb0y.bigglobe.noise.source.ConstantGrid3D;
import builderb0y.bigglobe.noise.source.GaussianGrid3D;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid3D;
import builderb0y.bigglobe.noise.source.WorleyGrid3D;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

@UseCoder(name = "REGISTRY", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface Grid3D extends Grid, CoderRegistryTyped<Grid3D> {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public MethodInfo getValue, getBulkX, getBulkY, getBulkZ;
	}

	@UnknownNullability
	@SuppressWarnings("TestOnlyProblems")
	public static final CoderRegistry<Grid3D> REGISTRY = Grid.TESTING.booleanValue() ? null : new CoderRegistry<>(BigGlobeMod.modID("grid_3d"));
	public static final Object INITIALIZER = new Object() {{
		if (REGISTRY != null) {
			REGISTRY.registerAuto(BigGlobeMod.modID("constant"),                                ConstantGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("white_noise"),                           WhiteNoiseGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("binary"),                                    BinaryGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("gaussian"),                                GaussianGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("linear"),                                    LinearGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smooth"),                                    SmoothGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smoother"),                                SmootherGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("cubic"),                                      CubicGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("worley"),                                    WorleyGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("linear_resample"),                   LinearResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smooth_resample"),                   SmoothResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smoother_resample"),               SmootherResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("cubic_resample"),                     CubicResampleGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("dx_linear_resample"),     LinearDerivativeXResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dx_smooth_resample"),     SmoothDerivativeXResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dx_smoother_resample"), SmootherDerivativeXResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dx_cubic_resample"),       CubicDerivativeXResampleGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("dy_linear_resample"),     LinearDerivativeYResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dy_smooth_resample"),     SmoothDerivativeYResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dy_smoother_resample"), SmootherDerivativeYResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dy_cubic_resample"),       CubicDerivativeYResampleGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("dz_linear_resample"),     LinearDerivativeZResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dz_smooth_resample"),     SmoothDerivativeZResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dz_smoother_resample"), SmootherDerivativeZResampleGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dz_cubic_resample"),       CubicDerivativeZResampleGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("offset"),                                    OffsetGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("negate"),                                    NegateGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("abs"),                                          AbsGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("square"),                                    SquareGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("change_range"),                         ChangeRangeGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("sum"),                                      SummingGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("product"),                                  ProductGrid3D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("project_x"),                              ProjectGrid3D_X.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("project_y"),                              ProjectGrid3D_Y.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("project_z"),                              ProjectGrid3D_Z.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("project_xy"),                            ProjectGrid3D_XY.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("project_xz"),                            ProjectGrid3D_XZ.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("project_yz"),                            ProjectGrid3D_YZ.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("stacked_xy"),                              StackedGrid_XY.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("stacked_xz"),                              StackedGrid_XZ.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("stacked_yz"),                              StackedGrid_YZ.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("script"),                                  ScriptedGrid3D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("template"),                                TemplateGrid3D.class);
		}
	}};

	/** returns the value at the specified coordinates. */
	public abstract double getValue(long seed, int x, int y, int z);

	/**
	gets (samples.length()) values starting at (startX, y, z) and continuing in the
	+X direction, with a spacing of 1 between each sampled coordinate.
	the sampled values are stored in (samples), starting at index 0.
	if (samples.length()) is less than or equal to 0, then this method will do nothing.

	@implSpec this method must produce the same results as the following code: {@code
		for (int i = 0; i < sampleCount; i++) {
			samples[i] = this.getValue(seed, startX + i, y, z);
		}
	}
	to within the limits of floating point precision.
	in other words, the exact values stored in samples by this method can
	differ slightly from the values stored in samples by the above loop.
	this may happen if implementations perform interpolation differently in bulk vs. in repetition.
	implementations are encouraged to replace the above loop with a more efficient approach where applicable.
	*/
	public default void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		for (int index = 0, length = samples.length(); index < length; index++) {
			samples.setD(index, this.getValue(seed, startX + index, y, z));
		}
	}

	/**
	gets (samples.length()) values starting at (x, startY, z) and continuing in the
	+Y direction, with a spacing of 1 between each sampled coordinate.
	the sampled values are stored in (samples), starting at index 0.
	if (samples.length()) is less than or equal to 0, then this method will do nothing.

	@implSpec this method must produce the same results as the following code: {@code
		for (int index = 0; index < samples.length(); index++) {
			samples.setD(index, this.getValue(seed, x, startY + index, z));
		}
	}
	to within the limits of floating point precision.
	in other words, the exact values stored in samples by this method can
	differ slightly from the values stored in samples by the above loop.
	this may happen if implementations perform interpolation differently in bulk vs. in repetition.
	implementations are encouraged to replace the above loop with a more efficient approach where applicable.
	*/
	public default void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		for (int index = 0, length = samples.length(); index < length; index++) {
			samples.setD(index, this.getValue(seed, x, startY + index, z));
		}
	}

	/**
	gets (samples.length()) values starting at (x, y, startZ) and continuing in the
	+Z direction, with a spacing of 1 between each sampled coordinate.
	the sampled values are stored in (samples), starting at index 0.
	if (samples.length()) is less than or equal to 0, then this method will do nothing.

	@implSpec this method must produce the same results as the following code: {@code
		for (int index = 0; index < sampleCount; index++) {
			samples.setD(index, this.getValue(seed, x, y, startZ + index));
		}
	}
	to within the limits of floating point precision.
	in other words, the exact values stored in samples by this method can
	differ slightly from the values stored in samples by the above loop.
	this may happen if implementations perform interpolation differently in bulk vs. in repetition.
	implementations are encouraged to replace the above loop with a more efficient approach where applicable.
	*/
	public default void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		for (int index = 0, length = samples.length(); index < length; index++) {
			samples.setD(index, this.getValue(seed, x, y, startZ + index));
		}
	}

	@Override
	public default int getDimensions() {
		return 3;
	}
}