package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;

@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER, strict = false)
public abstract class ChangeRangeGrid implements UnaryGrid {

	public final double min, max;
	public final transient double multiplier, adder;

	public ChangeRangeGrid(Grid grid, double min, double max) {
		this.min = min;
		this.max = max;
		//mix(min, max, unmix(grid.min, grid.max, x))
		//mix(min, max, (x - grid.min) / (grid.max - grid.min))
		//(x - grid.min) / (grid.max - grid.min) * (max - min) + min
		//(x - grid.min) * ((max - min) / (grid.max - grid.min)) + min
		//x * ((max - min) / (grid.max - grid.min)) - grid.min * ((max - min) / (grid.max - grid.min)) + min
		//x * ((max - min) / (grid.max - grid.min)) + min - grid.min * ((max - min) / (grid.max - grid.min))
		this.multiplier = (max - min) / (grid.maxValue() - grid.minValue());
		this.adder = min - grid.minValue() * this.multiplier;
	}

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, ? extends ChangeRangeGrid> context) throws VerifyException {
		ChangeRangeGrid grid = context.object;
		if (
			grid != null && (
				grid.multiplier == 0.0D ||
				!Double.isFinite(grid.multiplier) ||
				!Double.isFinite(grid.adder)
			)
		) {
			throw new VerifyException(() ->
				context
				.pathToStringBuilder()
				.append(" cannot change range from [")
				.append(grid.getGrid().minValue())
				.append(", ")
				.append(grid.getGrid().maxValue())
				.append("] to [")
				.append(grid.min)
				.append(", ")
				.append(grid.max)
				.append(']')
				.toString()
			);
		}
	}

	@Override
	public double minValue() {
		return this.min;
	}

	@Override
	public double maxValue() {
		return this.max;
	}

	@Override
	public double operate(double value) {
		return value * this.multiplier + this.adder;
	}
}