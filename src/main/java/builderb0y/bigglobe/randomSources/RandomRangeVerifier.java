package builderb0y.bigglobe.randomSources;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.verifiers.AutoVerifier;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;

public class RandomRangeVerifier implements AutoVerifier<RandomSource> {

	public final double min, max;
	public final boolean minInclusive, maxInclusive;

	public RandomRangeVerifier(double min, double max, boolean minInclusive, boolean maxInclusive) {
		this.min = min;
		this.max = max;
		this.minInclusive = minInclusive;
		this.maxInclusive = maxInclusive;
	}

	public RandomRangeVerifier(VerifyRandomRange annotation) {
		this(annotation.min(), annotation.max(), annotation.minInclusive(), annotation.maxInclusive());
	}

	public static @interface VerifyRandomRange {

		public double min() default Double.NEGATIVE_INFINITY;

		public double max() default Double.POSITIVE_INFINITY;

		public boolean minInclusive() default true;

		public boolean maxInclusive() default true;
	}

	@Override
	@OverrideOnly
	public <T_Encoded> void verify(@NotNull VerifyContext<T_Encoded, RandomSource> context) throws VerifyException {
		if (context.object == null) return;
		double min = context.object.minValue();
		double max = context.object.maxValue();
		if (Double.isNaN(min)) {
			throw new VerifyException(() -> context.pathToStringBuilder().append(".min cannot be NaN").toString());
		}
		if (Double.isNaN(max)) {
			throw new VerifyException(() -> context.pathToStringBuilder().append(".max cannot be NaN").toString());
		}
		if (
			(
				this.minInclusive
				? Double.compare(min, this.min) >= 0
				: Double.compare(min, this.min) > 0
			)
			&& (
				this.maxInclusive
				? Double.compare(max, this.max) <= 0
				: Double.compare(max, this.max) < 0
			)
		) {
			return;
		}
		else {
			throw new VerifyException(() -> {
				StringBuilder message = context.pathToStringBuilder();
				boolean haveMin = this.min != Double.NEGATIVE_INFINITY || !this.minInclusive;
				boolean haveMax = this.max != Double.POSITIVE_INFINITY || !this.maxInclusive;
				assert haveMin || haveMax : "No bounds, but still failed?";
				message.append(" must be ");
				if (haveMin) {
					message.append("greater than ");
					if (this.minInclusive) message.append("or equal to ");
					message.append(this.min);
				}
				if (haveMin && haveMax) {
					message.append(" and ");
				}
				if (haveMax) {
					message.append("less than ");
					if (this.maxInclusive) message.append("or equal to ");
					message.append(this.max);
				}
				return message.toString();
			});
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + (this.minInclusive ? '[' : '(') + this.min + ", " + this.max + (this.maxInclusive ? ']' : ')');
	}

	public static class Factory extends NamedVerifierFactory {

		public static final Factory INSTANCE = new Factory();

		@Override
		@OverrideOnly
		public <T_HandledType> @Nullable AutoVerifier<?> tryCreate(@NotNull FactoryContext<T_HandledType> context) throws FactoryException {
			if (context.type.getRawClass() != null && RandomSource.class.isAssignableFrom(context.type.getRawClass())) {
				VerifyRandomRange range = context.type.getAnnotations().getFirst(VerifyRandomRange.class);
				if (range != null) {
					return new RandomRangeVerifier(range);
				}
			}
			return null;
		}
	}
}