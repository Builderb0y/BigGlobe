package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.BlockState3DAccessSchema;
import builderb0y.bigglobe.columns.scripted.Valids.BlockState3DValid;
import builderb0y.bigglobe.columns.scripted.Valids._3DValid;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class BlockStateScript3DColumnEntry extends Script3DColumnEntry {

	public final @VerifyNullable BlockState3DValid valid;

	public BlockStateScript3DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, @VerifyNullable BlockState3DValid valid) {
		super(value, false);
		this.valid = valid;
	}

	@Override
	public _3DValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return BlockState3DAccessSchema.INSTANCE;
	}
}