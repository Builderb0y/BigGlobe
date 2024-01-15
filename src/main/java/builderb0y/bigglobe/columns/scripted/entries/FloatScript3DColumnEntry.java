package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultFloat;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Float3DAccessSchema;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class FloatScript3DColumnEntry extends Script3DColumnEntry {

	public final @VerifyNullable Valid valid;
	public static record Valid(
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y,
		@DefaultFloat(0.0F) float fallback
	)
	implements IValid {

		@Override
		public ConstantValue getFallback() {
			return ConstantValue.of(this.fallback);
		}
	}

	public FloatScript3DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Valid valid, boolean cache) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public IValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Float3DAccessSchema.INSTANCE;
	}
}