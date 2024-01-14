package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;

import static org.objectweb.asm.Opcodes.*;

public abstract class Basic2DColumnEntry implements ColumnEntry {

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		if (this.hasField()) {
			int flagsIndex = memory.getTyped(ColumnEntryMemory.FLAGS_INDEX);
			MethodCompileContext computer = context.mainClass.newMethod(ACC_PUBLIC, "compute_" + memory.getTyped(ColumnEntryMemory.INTERNAL_NAME), memory.getTyped(ColumnEntryMemory.TYPE).type());
			memory.putTyped(ColumnEntryMemory.COMPUTER, computer);
			memory.getTyped(ColumnEntryMemory.GETTER).prepareParameters().setCode(
				"""
				int oldFlags = flagsField
				int newFlags = oldFlags | flagsBitmask
				if (oldFlags != newFlags:
					flatsField = newFlags
					return(value := compute())
				)
				else (
					return(value)
				)
				""",
				new MutableScriptEnvironment()
				.addVariableRenamedGetField(context.loadSelf(), "flagsField", context.flagsField(flagsIndex))
				.addVariableConstant("flagsBitmask", DataCompileContext.flagsFieldBitmask(flagsIndex))
				.addFunctionInvoke("compute", context.loadSelf(), computer.info)
			);
		}
	}

	public static abstract class Basic2DAccessSchema implements AccessSchema {

		@Override
		public boolean requiresYLevel() {
			return false;
		}

		@Override
		public boolean equals(Object other) {
			return other != null && other.getClass() == this.getClass();
		}

		@Override
		public int hashCode() {
			return this.getClass().hashCode();
		}
	}
}