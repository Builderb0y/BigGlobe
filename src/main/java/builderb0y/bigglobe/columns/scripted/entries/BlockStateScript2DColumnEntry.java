package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.BlockState2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.Valids.BlockState2DValid;
import builderb0y.bigglobe.columns.scripted.Valids._2DValid;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class BlockStateScript2DColumnEntry extends Script2DColumnEntry {

	public final @VerifyNullable BlockState2DValid valid;

	public BlockStateScript2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, boolean cache, @VerifyNullable BlockState2DValid valid) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public _2DValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return BlockState2DAccessSchema.INSTANCE;
	}
}