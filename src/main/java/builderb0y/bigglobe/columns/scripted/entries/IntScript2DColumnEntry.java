package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Int2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.Valids.Int2DValid;
import builderb0y.bigglobe.columns.scripted.Valids._2DValid;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class IntScript2DColumnEntry extends Script2DColumnEntry {

	public final @VerifyNullable Int2DValid valid;

	public IntScript2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Int2DValid valid, boolean cache) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public _2DValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Int2DAccessSchema.INSTANCE;
	}
}