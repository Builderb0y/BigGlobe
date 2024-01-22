package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Long2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class LongConstantColumnEntry extends ConstantColumnEntry {

	public final long value;

	public LongConstantColumnEntry(long value) {
		this.value = value;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Long2DAccessSchema.INSTANCE;
	}

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		return_(ldc(this.value)).emitBytecode(getterMethod);
		getterMethod.endCode();
	}
}