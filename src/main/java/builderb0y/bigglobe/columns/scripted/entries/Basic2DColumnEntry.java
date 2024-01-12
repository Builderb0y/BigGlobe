package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.DataCompileContext.ColumnCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;

import static org.objectweb.asm.Opcodes.*;

public abstract class Basic2DColumnEntry implements ColumnEntry {

	public abstract boolean isCached();

	@Override
	public void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		if (this.isCached()) {
			context.generateFlaggedGetterSetter(memory);
		}
		else {
			memory.putTyped(
				ColumnEntryMemory.GETTER,
				context.mainClass.newMethod(
					ACC_PUBLIC,
					"get_" + DataCompileContext.internalName(memory.getTyped(ColumnEntryMemory.ACCESSOR_ID), context.mainClass.memberUniquifier++),
					memory.getTyped(ColumnEntryMemory.TYPE).type()
				)
			);
		}
	}

	@Override
	public void setupEnvironment(ColumnEntryMemory memory, DataCompileContext context) {
		MethodCompileContext method = memory.getTyped(ColumnEntryMemory.GETTER);
		context.environment.addVariableInvoke(context.loadSelf(), method.info);
	}

	public static interface _2DAccessSchema extends AccessSchema {

		@Override
		public default boolean requiresYLevel() {
			return false;
		}
	}

	public static abstract class Basic2DAccessSchema implements _2DAccessSchema {

		public abstract TypeInfo type();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(this.type(), null);
		}

		@Override
		public boolean equals(Object other) {
			return other != null && other.getClass() == this.getClass();
		}

		@Override
		public int hashCode() {
			return this.type().hashCode();
		}
	}
}