package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Double3DAccessSchema;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class DoubleScript3DColumnEntry extends Script3DColumnEntry {

	public final @VerifyNullable Valid valid;
	public static record Valid(
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y,
		@DefaultDouble(0.0D) double fallback
	)
	implements IValid {

		@Override
		public ConstantValue getFallback() {
			return ConstantValue.of(this.fallback);
		}
	}

	public DoubleScript3DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Valid valid, boolean cache) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public IValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Double3DAccessSchema.INSTANCE;
	}
}