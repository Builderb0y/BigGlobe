package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Double2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.Valids.Double2DValid;
import builderb0y.bigglobe.columns.scripted.Valids._2DValid;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class DoubleScript2DColumnEntry extends Script2DColumnEntry {

	public final @VerifyNullable Double2DValid valid;

	public DoubleScript2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Double2DValid valid, boolean cache) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public _2DValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Double2DAccessSchema.INSTANCE;
	}
}