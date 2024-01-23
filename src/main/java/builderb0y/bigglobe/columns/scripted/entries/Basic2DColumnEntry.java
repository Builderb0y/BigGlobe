package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema.TypeContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.Valids._2DValid;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class Basic2DColumnEntry implements ColumnEntry {

	public abstract _2DValid valid();

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		_2DValid valid = this.valid();
		TypeContext type = memory.getTyped(ColumnEntryMemory.TYPE);
		String internalName = memory.getTyped(ColumnEntryMemory.INTERNAL_NAME);
		if (this.hasField()) {
			FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
			int flagsIndex = memory.getTyped(ColumnEntryMemory.FLAGS_INDEX);
			MethodCompileContext computer = context.mainClass.newMethod(ACC_PUBLIC, "compute_" + internalName, type.commonType());
			getterMethod.setCode(
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
				MethodCompileContext actualComputer = context.mainClass.newMethod(ACC_PUBLIC, "actually_compute_" + internalName, type.exposedType());
				memory.putTyped(ColumnEntryMemory.COMPUTER, actualComputer);

				MethodCompileContext testMethod = context.mainClass.newMethod(ACC_PUBLIC, "test_" + internalName, TypeInfos.BOOLEAN);
				memory.putTyped(ColumnEntryMemory.VALID_WHERE, testMethod);

				computer.setCode(
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
					.addVariableConstant("fallback", valid.getFallback(type.exposedType()))
				);
			}
			else {
				memory.putTyped(ColumnEntryMemory.COMPUTER, computer);
			}
		}
		else {
			if (valid != null) {
				MethodCompileContext computer = context.mainClass.newMethod(ACC_PUBLIC, "compute_" + internalName, type.exposedType());
				memory.putTyped(ColumnEntryMemory.COMPUTER, computer);
				MethodCompileContext testMethod = context.mainClass.newMethod(ACC_PUBLIC, "test_" + internalName, TypeInfos.BOOLEAN);
				memory.putTyped(ColumnEntryMemory.VALID_WHERE, testMethod);

				getterMethod.setCode(
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
					.addVariableConstant("fallback", valid.getFallback(type.exposedType()))
				);
			}
			else {
				memory.putTyped(ColumnEntryMemory.COMPUTER, getterMethod);
			}
		}
	}

	@Override
	public void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {
		LazyVarInfo self = new LazyVarInfo("this", setterMethod.clazz.info);
		LazyVarInfo value = new LazyVarInfo("value", memory.getTyped(ColumnEntryMemory.TYPE).exposedType());
		return_(putField(load(self), memory.getTyped(ColumnEntryMemory.FIELD).info, load(value))).emitBytecode(setterMethod);
		setterMethod.endCode();
	}

	public abstract void populateCompute(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException;

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		_2DValid valid = this.valid();
		if (valid != null && valid.where() != null) {
			context.setMethodCode(memory.getTyped(ColumnEntryMemory.VALID_WHERE), valid.where(), false);
		}
		this.populateCompute(memory, context, memory.getTyped(ColumnEntryMemory.COMPUTER));
	}
}