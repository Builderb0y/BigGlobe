package builderb0y.bigglobe.columns.restrictions;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;

public class ScriptColumnRestriction implements ColumnRestriction {

	public final ColumnYToDoubleScript.Holder script;
	public final transient ColumnValue<?>[] columnValues;

	public ScriptColumnRestriction(ColumnYToDoubleScript.Holder script) {
		this.script = script;
		this.columnValues = script.usedValues.toArray(ColumnValue.ARRAY_FACTORY);
	}

	@Override
	public double getRestriction(WorldColumn column, double y) {
		return this.script.evaluate(column, y);
	}

	/*
	@Override
	public boolean dependsOnY(WorldColumn column) {
		return this.dependsOnY;
	}
	*/

	@Override
	public void forEachValue(Consumer<? super ColumnValue<?>> action) {
		for (ColumnValue<?> value : this.columnValues) {
			action.accept(value);
		}
	}

	@Override
	public Stream<ColumnValue<?>> getValues() {
		return Arrays.stream(this.columnValues);
	}
}