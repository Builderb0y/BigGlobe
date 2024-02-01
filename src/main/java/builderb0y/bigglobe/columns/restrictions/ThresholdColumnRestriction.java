package builderb0y.bigglobe.columns.restrictions;

import java.lang.invoke.MethodHandle;

import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.util.TypeFormatter;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.Interpolator;

public class ThresholdColumnRestriction extends PropertyColumnRestriction {

	public final double min, max;
	public final @DefaultBoolean(true) boolean smooth_min, smooth_max;

	public ThresholdColumnRestriction(
		Identifier property,
		double min,
		double max,
		@DefaultBoolean(true) boolean smooth_min,
		@DefaultBoolean(true) boolean smooth_max
	) {
		super(property);
		this.min = min;
		this.max = max;
		this.smooth_min = smooth_min;
		this.smooth_max = smooth_max;
	}

	@Override
	public double getRestriction(ScriptedColumn column, int y) {
		double value;
		try {
			value = (double)(this.getter.invokeExact(column, y));
		}
		catch (Throwable throwable) {
			this.onError(throwable);
			return 0.0D;
		}
		value = Interpolator.unmixClamp(this.min, this.max, value);
		if (this.smooth_min) {
			if (this.smooth_max) {
				return Interpolator.smooth(value);
			}
			else {
				return value * value;
			}
		}
		else {
			if (this.smooth_max) {
				return value * (2.0D - value);
			}
			else {
				return value;
			}
		}
	}

	@Override
	public int hashCode() {
		int hash = this.property.hashCode();
		hash = hash * 31 + Double.hashCode(this.min);
		hash = hash * 31 + Double.hashCode(this.max);
		hash = hash * 31 + Boolean.hashCode(this.smooth_min);
		hash = hash * 31 + Boolean.hashCode(this.smooth_max);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof ThresholdColumnRestriction that &&
			this.property.equals(that.property) &&
			this.min == that.min &&
			this.max == that.max
		);
	}

	@Override
	public String toString() {
		return TypeFormatter.getSimpleClassName(this.getClass()) + ": { property: " + this.property + ", min: " + this.min + ", max: " + this.max + ", smooth_min: " + this.smooth_min + ", smooth_max: " + this.smooth_max + " }";
	}
}