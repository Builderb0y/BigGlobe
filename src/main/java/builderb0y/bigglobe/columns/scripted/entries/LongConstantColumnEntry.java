package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Long2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class LongConstantColumnEntry implements ColumnEntry {

	public final long value;

	public LongConstantColumnEntry(long value) {
		this.value = value;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Long2DAccessSchema.INSTANCE;
	}

	@Override
	public boolean hasField() {
		return false;
	}

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		return_(ldc(this.value)).emitBytecode(getterMethod);
	}

	@Override
	public void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {
		//no-op.
	}

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		//no-op.
	}
}