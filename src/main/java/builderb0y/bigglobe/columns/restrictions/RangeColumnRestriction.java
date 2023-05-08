package builderb0y.bigglobe.columns.restrictions;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.util.TypeFormatter;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.Interpolator;

@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER)
public class RangeColumnRestriction implements ColumnRestriction {

	public final ColumnValue<?> property;
	public final @VerifyNullable Double min;
	public final @VerifyNullable @VerifySorted(greaterThan = "min", lessThan = "max") Double mid;
	public final @VerifyNullable Double max;
	public final @DefaultBoolean(true) boolean smooth;
	public final transient DoubleUnaryOperator impl;

	public RangeColumnRestriction(ColumnValue<?> property, Double min, Double mid, Double max, boolean smooth) {
		this.property = property;
		this.min = min;
		this.mid = mid;
		this.max = max;
		this.smooth = smooth;
		double min_ = min != null ? min.doubleValue() : 0.0D;
		double mid_ = mid != null ? mid.doubleValue() : 0.0D;
		double max_ = max != null ? max.doubleValue() : 0.0D;

		if (min != null) {
			if (mid != null) {
				if (max != null) { //min, mid, max
					this.impl = all3(min_, mid_, max_);
				}
				else { //min, mid
					this.impl = value -> {
						if (value >= mid_) return 1.0D;
						if (value <= min_) return 0.0D;
						return Interpolator.unmixLinear(min_, mid_, value);
					};
				}
			}
			else {
				if (max != null) { //min, max
					double mid2_ = (min_ + max_) * 0.5D;
					this.impl = all3(min_, mid2_, max_);
				}
				else { //min
					this.impl = value -> {
						return value > min_ ? 1.0D : 0.0D;
					};
				}
			}
		}
		else {
			if (mid != null) {
				if (max != null) { //mid, max
					this.impl = value -> {
						if (value >= max_) return 0.0D;
						if (value <= mid_) return 1.0D;
						return Interpolator.unmixLinear(max_, mid_, value);
					};
				}
				else { //mid
					this.impl = null;
				}
			}
			else {
				if (max != null) { //max
					this.impl = value -> {
						return value < max_ ? 1.0D : 0.0D;
					};
				}
				else { //none
					this.impl = null;
				}
			}
		}
	}

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, RangeColumnRestriction> context) throws VerifyException {
		if (context.object != null && context.object.impl == null) {
			throw new VerifyException(() -> context.pathToStringBuilder().append(" must specify min or max (or both), and optionally, mid.").toString());
		}
	}

	public static DoubleUnaryOperator all3(double min, double mid, double max) {
		return value -> {
			if (value > mid) {
				return value >= max ? 0.0D : Interpolator.unmixLinear(max, mid, value);
			}
			else {
				return value <= min ? 0.0D : Interpolator.unmixLinear(min, mid, value);
			}
		};
	}

	@Override
	public double getRestriction(WorldColumn column, double y) {
		double value = this.property.getValue(column, y);
		if (Double.isNaN(value)) return 0.0D;
		double restriction = this.impl.applyAsDouble(value);
		if (this.smooth) restriction = Interpolator.smooth(restriction);
		return restriction;
	}

	/*
	@Override
	public boolean dependsOnY(WorldColumn column) {
		return this.property.dependsOnY();
	}
	*/

	@Override
	public void forEachValue(Consumer<? super ColumnValue<?>> action) {
		action.accept(this.property);
	}

	@Override
	public Stream<ColumnValue<?>> getValues() {
		return Stream.of(this.property);
	}

	@Override
	public int hashCode() {
		int hash = this.property.hashCode();
		hash = hash * 31 + Objects.hashCode(this.min);
		hash = hash * 31 + Objects.hashCode(this.mid);
		hash = hash * 31 + Objects.hashCode(this.max);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof RangeColumnRestriction that &&
			this.property == that.property &&
			Objects.equals(this.min, that.min) &&
			Objects.equals(this.mid, that.mid) &&
			Objects.equals(this.max, that.max)
		);
	}

	@Override
	public String toString() {
		return TypeFormatter.getSimpleClassName(this.getClass()) + ": { property: " + this.property + ", min: " + this.min + ", mid: " + this.mid + ", max: " + this.max + ", smooth: " + this.smooth + " }";
	}
}