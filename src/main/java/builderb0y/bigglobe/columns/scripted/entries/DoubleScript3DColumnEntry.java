package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Double3DAccessSchema;
import builderb0y.bigglobe.columns.scripted.Valids.Double3DValid;
import builderb0y.bigglobe.columns.scripted.Valids._3DValid;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class DoubleScript3DColumnEntry extends Script3DColumnEntry {

	public final @VerifyNullable Double3DValid valid;

	public DoubleScript3DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Double3DValid valid, boolean cache) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public _3DValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Double3DAccessSchema.INSTANCE;
	}
}