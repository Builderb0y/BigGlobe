package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.DataCompileContext.ColumnCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

public class DoubleScript2DColumnEntry extends Script2DColumnEntry {

	public final @VerifyNullable Valid valid;
	public static record Valid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultDouble(Double.NaN) double fallback) implements IValid {

		@Override
		public ConstantValue getFallback() {
			return ConstantValue.of(this.fallback);
		}
	}

	public DoubleScript2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Valid valid, boolean cache) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public IValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return new Double2DAccessSchema();
	}

	@RecordLike({})
	public static class Double2DAccessSchema extends Basic2DAccessSchema {

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.DOUBLE, null);
		}
	}
}