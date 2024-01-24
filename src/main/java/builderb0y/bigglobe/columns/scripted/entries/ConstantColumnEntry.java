package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.codecs.Any;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantColumnEntry implements ColumnEntry {

	public final AccessSchema params;
	public final @Any Object value;

	public ConstantColumnEntry(AccessSchema params, Object value) {
		this.params = params;
		this.value = value;
	}

	@Override
	public boolean hasField() {
		return false;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return this.params;
	}

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		return_(this.params.createConstant(this.value, context.root())).emitBytecode(getterMethod);
		getterMethod.endCode();
	}

	@Override
	public void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {

	}

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {

	}
}