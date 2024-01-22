package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Double2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class DoubleConstantColumnEntry extends ConstantColumnEntry {

	public final double value;

	public DoubleConstantColumnEntry(double value) {
		this.value = value;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Double2DAccessSchema.INSTANCE;
	}
	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		return_(ldc(this.value)).emitBytecode(getterMethod);
		getterMethod.endCode();
	}
}