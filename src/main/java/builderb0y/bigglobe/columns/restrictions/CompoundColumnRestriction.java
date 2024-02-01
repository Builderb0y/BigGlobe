package builderb0y.bigglobe.columns.restrictions;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import builderb0y.autocodec.annotations.VerifySizeRange;
import builderb0y.autocodec.util.HashStrategies;
import builderb0y.autocodec.util.TypeFormatter;

public abstract class CompoundColumnRestriction implements ColumnRestriction {

	public final ColumnRestriction @VerifySizeRange(min = 2) [] restrictions;

	public CompoundColumnRestriction(ColumnRestriction... restrictions) {
		this.restrictions = restrictions;
	}

	@Override
	public int hashCode() {
		return HashStrategies.unorderedArrayHashCode(HashStrategies.defaultStrategy(), (Object[])(this.restrictions)) ^ this.getClass().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj != null &&
			obj.getClass() == this.getClass() &&
			HashStrategies.unorderedArrayEqualsSmall(
				HashStrategies.defaultStrategy(),
				this.restrictions,
				((CompoundColumnRestriction)(obj)).restrictions
			)
		);
	}

	@Override
	public String toString() {
		return Arrays.stream(this.restrictions).map(Objects::toString).collect(Collectors.joining(", ", TypeFormatter.getSimpleClassName(this.getClass()) + '(', ")"));
	}
}