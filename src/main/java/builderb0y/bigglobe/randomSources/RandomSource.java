package builderb0y.bigglobe.randomSources;

import java.util.random.RandomGenerator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.math.Interpolator;

@UseCoder(name = "REGISTRY", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface RandomSource extends CoderRegistryTyped<RandomSource> {

	public static final CoderRegistry<RandomSource> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("random_source")) {

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RandomSource> context) throws EncodeException {
			if (context.input instanceof ConstantRandomSource constant) {
				return context.createDouble(constant.value());
			}
			return super.encode(context);
		}

		@Override
		public @Nullable <T_Encoded> RandomSource decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			Number number = context.tryAsNumber();
			if (number != null) {
				return new ConstantRandomSource(number.doubleValue());
			}
			return super.decode(context);
		}
	};
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("constant"),              ConstantRandomSource.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("uniform"),                UniformRandomSource.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("linear_low"),           LinearLowRandomSource.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("linear_high"),         LinearHighRandomSource.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("linear_centered"), LinearCenteredRandomSource.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("gaussian"),              GaussianRandomSource.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("exponential"),        ExponentialRandomSource.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("average"),               AveragedRandomSource.class);
	}};

	public abstract double get(long seed);

	public abstract double get(RandomGenerator random);

	public abstract double minValue();

	public abstract double maxValue();

	public default double mix(double value) {
		return Interpolator.mixLinear(this.minValue(), this.maxValue(), value);
	}
}