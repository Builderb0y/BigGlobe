package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

public abstract class Script2DColumnEntry extends Basic2DColumnEntry {

	public final ScriptUsage<GenericScriptTemplateUsage> value;
	public final @DefaultBoolean(true) boolean cache;

	public Script2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, boolean cache) {
		this.value = value;
		this.cache = cache;
	}

	@Override
	public boolean hasField() {
		return this.cache;
	}

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		context.setMethodCode(memory.getTyped(ColumnEntryMemory.COMPUTER), this.value);
	}
}