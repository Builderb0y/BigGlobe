package builderb0y.bigglobe.noise;

import org.jetbrains.annotations.UnknownNullability;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.noise.processing.*;
import builderb0y.bigglobe.noise.resample.*;
import builderb0y.bigglobe.noise.resample.derivatives.CubicDerivativeXResampleGrid1D;
import builderb0y.bigglobe.noise.resample.derivatives.LinearDerivativeXResampleGrid1D;
import builderb0y.bigglobe.noise.resample.derivatives.SmoothDerivativeXResampleGrid1D;
import builderb0y.bigglobe.noise.resample.derivatives.SmootherDerivativeXResampleGrid1D;
import builderb0y.bigglobe.noise.source.ConstantGrid1D;
import builderb0y.bigglobe.noise.source.GaussianGrid1D;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid1D;
import builderb0y.bigglobe.noise.source.WorleyGrid1D;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

@UseCoder(name = "REGISTRY", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface Grid1D extends Grid, CoderRegistryTyped<Grid1D> {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public MethodInfo getValue, getBulkX;
	}

	@UnknownNullability
	@SuppressWarnings("TestOnlyProblems")
	public static final CoderRegistry<Grid1D> REGISTRY = Grid.TESTING.booleanValue() ? null : new CoderRegistry<>(BigGlobeMod.modID("grid_1d"));
	public static final Object INITIALIZER = new Object() {{
		if (REGISTRY != null) {
			REGISTRY.registerAuto(BigGlobeMod.modID("constant"),                                ConstantGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("white_noise"),                           WhiteNoiseGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("binary"),                                    BinaryGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("gaussian"),                                GaussianGrid1D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("linear"),                                    LinearGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smooth"),                                    SmoothGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smoother"),                                SmootherGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("cubic"),                                      CubicGrid1D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("worley"),                                    WorleyGrid1D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("linear_resample"),                   LinearResampleGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smooth_resample"),                   SmoothResampleGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("smoother_resample"),               SmootherResampleGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("cubic_resample"),                     CubicResampleGrid1D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("dx_linear_resample"),     LinearDerivativeXResampleGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dx_smooth_resample"),     SmoothDerivativeXResampleGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dx_smoother_resample"), SmootherDerivativeXResampleGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("dx_cubic_resample"),       CubicDerivativeXResampleGrid1D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("offset"),                                    OffsetGrid1D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("negate"),                                    NegateGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("abs"),                                          AbsGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("square"),                                    SquareGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("change_range"),                         ChangeRangeGrid1D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("sum"),                                      SummingGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("product"),                                  ProductGrid1D.class);

			REGISTRY.registerAuto(BigGlobeMod.modID("script"),                                  ScriptedGrid1D.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("template"),                                TemplateGrid1D.class);
		}
	}};

	/** returns the value at the specified coordinate. */
	public abstract double getValue(long seed, int x);

	/**
	gets (samples.length()) values starting at (startX) and continuing in the
	+X direction, with a spacing of 1 between each sampled coordinate.
	the sampled values are stored in (samples), starting at index 0.
	if (samples.length()) is less than or equal to 0, then this method will do nothing.

	@implSpec this method must produce the same results as the following code: {@code
		for (int index = 0; index < samples.length(); index++) {
			samples.setD(index, this.getValue(seed, startX + index));
		}
	}
	to within the limits of floating point precision.
	in other words, the exact values stored in samples by this method can
	differ slightly from the values stored in samples by the above loop.
	this may happen if implementations perform interpolation differently in bulk vs. in repetition.
	implementations are encouraged to replace the above loop with a more efficient approach where applicable.
	*/
	public default void getBulkX(long seed, int startX, NumberArray samples) {
		for (int index = 0, length = samples.length(); index < length; index++) {
			samples.setD(index, this.getValue(seed, startX + index));
		}
	}

	@Override
	public default int getDimensions() {
		return 1;
	}
}