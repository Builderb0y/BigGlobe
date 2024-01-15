package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultInt;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Int3DAccessSchema;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class IntScript3DColumnEntry extends Script3DColumnEntry {

	public final @VerifyNullable Valid valid;
	public static record Valid(
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y,
		@DefaultInt(0) int fallback
	)
	implements IValid {

		@Override
		public ConstantValue getFallback() {
			return ConstantValue.of(this.fallback);
		}
	}

	public IntScript3DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Valid valid, boolean cache) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public IValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Int3DAccessSchema.INSTANCE;
	}
}