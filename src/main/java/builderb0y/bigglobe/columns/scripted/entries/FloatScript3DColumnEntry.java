package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Float3DAccessSchema;
import builderb0y.bigglobe.columns.scripted.Valids.Float3DValid;
import builderb0y.bigglobe.columns.scripted.Valids._3DValid;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class FloatScript3DColumnEntry extends Script3DColumnEntry {

	public final @VerifyNullable Float3DValid valid;

	public FloatScript3DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Float3DValid valid, boolean cache) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public _3DValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Float3DAccessSchema.INSTANCE;
	}
}