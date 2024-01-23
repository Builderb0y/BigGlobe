package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Float2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class FloatConstantColumnEntry extends ConstantColumnEntry {

	public final float value;

	public FloatConstantColumnEntry(float value) {
		this.value = value;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Float2DAccessSchema.INSTANCE;
	}

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		return_(ldc(this.value)).emitBytecode(getterMethod);
		getterMethod.endCode();
	}
}