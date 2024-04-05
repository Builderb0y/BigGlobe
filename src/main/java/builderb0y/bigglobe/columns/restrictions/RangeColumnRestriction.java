package builderb0y.bigglobe.columns.restrictions;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.autocodec.util.TypeFormatter;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.math.Interpolator;

public class RangeColumnRestriction extends PropertyColumnRestriction {

	public final double min;
	public final @VerifySorted(greaterThan = "min") double mid;
	public final @VerifySorted(greaterThan = "mid") double max;
	public final @DefaultBoolean(true) boolean smooth;

	public RangeColumnRestriction(
		RegistryEntry<ColumnEntry> property,
		double min,
		double mid,
		double max,
		boolean smooth
	) {
		super(property);
		this.min = min;
		this.mid = mid;
		this.max = max;
		this.smooth = smooth;
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
		return (
			this.smooth
			? bandSmooth(this.min, this.mid, this.max, value)
			: bandLinear(this.min, this.mid, this.max, value)
		);
	}

	public static double curve1(double value, double coefficient) {
		double product = value * coefficient;
		return (product + value) / (product + 1.0D);
	}

	public static double inverse(double target) {
		return (-2.0D * target + 1.0D) / (target - 1.0D);
	}

	public static double curve2(double value, double target) {
		return curve1(value, inverse(target));
	}

	public static double bandLinear(double min, double mid, double max, double value) {
		if (!(value > min && value < max)) return 0.0D;
		if (!(mid > min && mid < max)) return 0.0D;
		mid = Interpolator.unmixLinear(min, max, mid);
		value = Interpolator.unmixLinear(min, max, value);
		double part = curve2(value, 1.0D - mid);
		return part * (1.0D - part) * 4.0D;
	}

	public static double bandSmooth(double min, double mid, double max, double value) {
		if (!(value > min && value < max)) return 0.0D;
		if (!(mid > min && mid < max)) return 0.0D;
		mid = Interpolator.unmixLinear(min, max, mid);
		value = Interpolator.unmixLinear(min, max, value);
		double part1 = curve2(value, 1.0D - mid);
		double part2 = part1 * (1.0D - part1) * 4.0D;
		double power = 1.0D / Interpolator.mixLinear(part1, 1.0D - part1, mid) + 1.0D;
		return Math.pow(part2, power);
	}

	@Override
	public int hashCode() {
		int hash = this.property.hashCode();
		hash = hash * 31 + Double.hashCode(this.min);
		hash = hash * 31 + Double.hashCode(this.mid);
		hash = hash * 31 + Double.hashCode(this.max);
		hash = hash * 31 + Boolean.hashCode(this.smooth);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof RangeColumnRestriction that &&
			this.property.equals(that.property) &&
			this.min == that.min &&
			this.mid == that.mid &&
			this.max == that.max
		);
	}

	@Override
	public String toString() {
		return TypeFormatter.getSimpleClassName(this.getClass()) + ": { property: " + this.property + ", min: " + this.min + ", mid: " + this.mid + ", max: " + this.max + ", smooth: " + this.smooth + " }";
	}
}