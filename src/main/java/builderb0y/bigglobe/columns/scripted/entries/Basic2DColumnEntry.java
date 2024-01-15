package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.Valids._2DValid;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class Basic2DColumnEntry implements ColumnEntry {

	public abstract _2DValid valid();

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		_2DValid valid = this.valid();
		TypeInfo type = memory.getTyped(ColumnEntryMemory.TYPE).type();
		String internalName = memory.getTyped(ColumnEntryMemory.INTERNAL_NAME);
		if (this.hasField()) {
			FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
			int flagsIndex = memory.getTyped(ColumnEntryMemory.FLAGS_INDEX);
			MethodCompileContext computer = context.mainClass.newMethod(ACC_PUBLIC, "compute_" + internalName, type);
			getterMethod.prepareParameters().setCode(
				"""
				int oldFlags = flagsField
				int newFlags = oldFlags | flagsBitmask
				if (oldFlags != newFlags:
					flagsField = newFlags
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
				.addVariableRenamedGetField(context.loadSelf(), "value", valueField.info)
			);

			if (valid != null) {
				MethodCompileContext actualComputer = context.mainClass.newMethod(ACC_PUBLIC, "actually_compute_" + internalName, type);
				memory.putTyped(ColumnEntryMemory.COMPUTER, actualComputer);

				MethodCompileContext testMethod = context.mainClass.newMethod(ACC_PUBLIC, "test_" + internalName, TypeInfos.BOOLEAN);
				memory.putTyped(ColumnEntryMemory.VALID_WHERE, testMethod);

				computer.prepareParameters().setCode(
					"""
					if (test():
						return(compute())
					)
					else (
						return(fallback)
					)
					""",
					new MutableScriptEnvironment()
					.addFunctionInvoke("test", context.loadSelf(), testMethod.info)
					.addFunctionInvoke("compute", context.loadSelf(), actualComputer.info)
					.addVariableConstant("fallback", valid.getFallback(type))
				);
			}
			else {
				memory.putTyped(ColumnEntryMemory.COMPUTER, computer);
			}
		}
		else {
			if (valid != null) {
				MethodCompileContext computer = context.mainClass.newMethod(ACC_PUBLIC, "compute_" + internalName, type);
				memory.putTyped(ColumnEntryMemory.COMPUTER, computer);
				MethodCompileContext testMethod = context.mainClass.newMethod(ACC_PUBLIC, "test_" + internalName, TypeInfos.BOOLEAN);
				memory.putTyped(ColumnEntryMemory.VALID_WHERE, testMethod);

				getterMethod.prepareParameters().setCode(
					"""
					if (test():
						return(compute())
					)
					else (
						return(fallback)
					)
					""",
					new MutableScriptEnvironment()
					.addFunctionInvoke("test", context.loadSelf(), testMethod.info)
					.addFunctionInvoke("compute", context.loadSelf(), computer.info)
					.addVariableConstant("fallback", valid.getFallback(type))
				);
			}
			else {
				memory.putTyped(ColumnEntryMemory.COMPUTER, getterMethod);
			}
		}
	}

	@Override
	public void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {
		setterMethod.scopes.withScope((MethodCompileContext setter) -> {
			VarInfo self = setter.addThis();
			VarInfo value = setter.newParameter("value", memory.getTyped(ColumnEntryMemory.TYPE).type());
			putField(load(self), memory.getTyped(ColumnEntryMemory.FIELD).info, load(value)).emitBytecode(setter);
		});
	}
}