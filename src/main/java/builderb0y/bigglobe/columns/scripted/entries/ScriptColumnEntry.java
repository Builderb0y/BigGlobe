package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.bigglobe.columns.scripted.Valid;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

public class ScriptColumnEntry extends AbstractColumnEntry {

	public final ScriptUsage script;

	public ScriptColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		@DefaultBoolean(true) boolean cache,
		ScriptUsage script,
		DecodeContext<?> decodeContext
	) {
		super(params, valid, cache, decodeContext);
		this.script = script;
		if (script.getTemplate() != null) {
			this.addDependency(script.getTemplate());
		}
	}

	@Override
	public void populateCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		context.setMethodCode(computeMethod, this.script, false, this, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));
	}

	@Override
	public void populateCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		context.setMethodCode(computeMethod, this.script, true, this, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));
	}
}