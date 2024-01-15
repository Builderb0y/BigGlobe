package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class Script2DColumnEntry extends Basic2DColumnEntry {

	public final ScriptUsage<GenericScriptTemplateUsage> value;
	public final @DefaultBoolean(true) boolean cache;

	public Script2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, boolean cache) {
		this.value = value;
		this.cache = cache;
	}

	@Override
	public boolean hasField() {
		return this.cache;
	}

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		context.setMethodCode(memory.getTyped(ColumnEntryMemory.COMPUTER), this.value);

		/*
		IValid valid = this.valid();
		if (this.cache) {
			if (valid != null) {
				MethodCompileContext testMethod = context.mainClass.newMethod(
					ACC_PUBLIC,
					"test_" + memory.getTyped(ColumnEntryMemory.INTERNAL_NAME),
					TypeInfos.BOOLEAN
				);
				MethodCompileContext actuallyCompute = context.mainClass.newMethod(
					ACC_PUBLIC,
					"actually_compute_" + memory.getTyped(ColumnEntryMemory.INTERNAL_NAME),
					memory.getTyped(ColumnEntryMemory.TYPE).type()
				);
				context.setMethodCode(testMethod, valid.where());
				context.generateGuardedComputer(
					memory.getTyped(ColumnEntryMemory.COMPUTER),
					testMethod.info,
					actuallyCompute.info,
					valid.getFallback()
				);
				context.setMethodCode(actuallyCompute, this.value);
			}
			else {
				context.setMethodCode(memory.getTyped(ColumnEntryMemory.COMPUTER), this.value);
			}
		}
		else {
			if (valid != null) {
				MethodCompileContext testMethod = context.mainClass.newMethod(
					ACC_PUBLIC,
					"test_" + memory.getTyped(ColumnEntryMemory.INTERNAL_NAME),
					TypeInfos.BOOLEAN
				);
				context.generateGuardedComputer(
					memory.getTyped(ColumnEntryMemory.GETTER),
					testMethod.info,
					memory.getTyped(ColumnEntryMemory.COMPUTER).info,
					valid.getFallback()
				);
				context.setMethodCode(testMethod, valid.where());
				context.setMethodCode(memory.getTyped(ColumnEntryMemory.COMPUTER), this.value);
			}
			else {
				context.setMethodCode(memory.getTyped(ColumnEntryMemory.GETTER), this.value);
			}
		}
		*/
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