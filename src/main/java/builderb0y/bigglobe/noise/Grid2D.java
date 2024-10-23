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
import builderb0y.bigglobe.noise.source.*;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

@UseCoder(name = "REGISTRY", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface Grid2D extends Grid, CoderRegistryTyped<Grid2D> {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public MethodInfo getValue, getBulkX, getBulkY;
	}

	@UnknownNullability
	@SuppressWarnings("TestOnlyProblems")
	public static final CoderRegistry<Grid2D> REGISTRY = Grid.TESTING.booleanValue() ? null : new CoderRegistry<>(BigGlobeMod.modID("grid_2d"));
	public static final Object INITIALIZER = new Object() {{
		if (REGISTRY != null) {
			REGISTRY.registerAuto(BigGlobeMod.modID("constant"),                                ConstantGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("white_noise"),                           WhiteNoiseGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("binary"),                                    BinaryGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("gaussian"),                                GaussianGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("linear"),                                    LinearGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smooth"),                                    SmoothGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smoother"),                                SmootherGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("cubic"),                                      CubicGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("worley"),                                    WorleyGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("linear_resample"),                   LinearResampleGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smooth_resample"),                   SmoothResampleGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smoother_resample"),               SmootherResampleGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("cubic_resample"),                     CubicResampleGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("dx_linear_resample"),     LinearDerivativeXResampleGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dx_smooth_resample"),     SmoothDerivativeXResampleGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dx_smoother_resample"), SmootherDerivativeXResampleGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dx_cubic_resample"),       CubicDerivativeXResampleGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("dy_linear_resample"),     LinearDerivativeYResampleGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dy_smooth_resample"),     SmoothDerivativeYResampleGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dy_smoother_resample"), SmootherDerivativeYResampleGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dy_cubic_resample"),       CubicDerivativeYResampleGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("offset"),                                    OffsetGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("negate"),                                    NegateGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("abs"),                                          AbsGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("square"),                                    SquareGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("change_range"),                         ChangeRangeGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("sum"),                                      SummingGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("product"),                                  ProductGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("project_x"),                              ProjectGrid2D_X.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("project_y"),                              ProjectGrid2D_Y.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("stalactites"),                           StalactiteGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("sine_sum"),                                SineWaveGrid2D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("script"),                                  ScriptedGrid2D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("template"),                                TemplateGrid2D.class);
		}
	}};

	/** returns the value at the specified coordinates. */
	public abstract double getValue(long seed, int x, int y);

	/**
	gets (samples.length()) values starting at (startX, y) and continuing in the
	+X direction, with a spacing of 1 between each sampled coordinate.
	the sampled values are stored in (samples), starting at index 0.
	if (samples.length()) is less than or equal to 0, then this method will do nothing.

	@implSpec this method must produce the same results as the following code: {@code
		for (int index = 0; index < samples.length(); index++) {
			samples.setD(index, this.getValue(seed, startX + index, y));
		}
	}
	to within the limits of floating point precision.
	in other words, the exact values stored in samples by this method can
	differ slightly from the values stored in samples by the above loop.
	this may happen if implementations perform interpolation differently in bulk vs. in repetition.
	implementations are encouraged to replace the above loop with a more efficient approach where applicable.
	*/
	public default void getBulkX(long seed, int startX, int y, NumberArray samples) {
		for (int index = 0, length = samples.length(); index < length; index++) {
			samples.setD(index, this.getValue(seed, startX + index, y));
		}
	}

	/**
	gets (sampleCount) values starting at (x, startY) and continuing in the
	+Y direction, with a spacing of 1 between each sampled coordinate.
	the sampled values are stored in (samples), starting at index 0.
	the indexes in (samples) that are greater than or equal to (sampleCount) will not be modified.
	if (samples.length()) is less than or equal to 0, then this method will do nothing.

	@implSpec this method must produce the same results as the following code: {@code
		for (int index = 0; index < samples.length(); index++) {
			samples.setD(index, this.getValue(seed, x, startY + index));
		}
	}
	to within the limits of floating point precision.
	in other words, the exact values stored in samples by this method can
	differ slightly from the values stored in samples by the above loop.
	this may happen if implementations perform interpolation differently in bulk vs. in repetition.
	implementations are encouraged to replace the above loop with a more efficient approach where applicable.
	*/
	public default void getBulkY(long seed, int x, int startY, NumberArray samples) {
		for (int index = 0, length = samples.length(); index < length; index++) {
			samples.setD(index, this.getValue(seed, x, startY + index));
		}
	}

	@Override
	public default int getDimensions() {
		return 2;
	}
}