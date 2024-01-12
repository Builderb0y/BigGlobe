package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.DefaultInt;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static org.objectweb.asm.Opcodes.*;

public abstract class Script2DColumnEntry extends Basic2DColumnEntry {

	public final ScriptUsage<GenericScriptTemplateUsage> value;
	public final @DefaultBoolean(true) boolean cache;

	public Script2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, boolean cache) {
		this.value = value;
		this.cache = cache;
	}

	@Override
	public boolean isCached() {
		return this.cache;
	}

	public abstract Valid valid();

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		Valid valid = this.valid();
		if (this.cache) {
			if (valid != null) {
				/*
				public int get() {
					flags and stuff
					return this.value = compute();
				}

				public boolean test() {
					return script;
				}

				public int compute() {
					if (this.test()) {
						return this.actuallyCompute();
					}
					else {
						return fallback;
					}
				}

				public int actuallyCompute() {
					return script;
				}
				*/
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
				context.generateComputer(testMethod, valid.where());
				context.generateGuardedComputer(
					memory.getTyped(ColumnEntryMemory.COMPUTER),
					testMethod.info,
					actuallyCompute.info,
					valid.getFallback()
				);
				context.generateComputer(actuallyCompute, this.value);
			}
			else {
				/*
				public int get() {
					flags and stuff
					return this.value = compute();
				}

				public int compute() {
					return script;
				}
				*/
				context.generateComputer(memory.getTyped(ColumnEntryMemory.COMPUTER), this.value);
			}
		}
		else {
			if (valid != null) {
				/*
				public int get() {
					if (this.test()) {
						return this.compute();
					}
					else {
						return fallback;
					}
				}

				public boolean test() {
					return script;
				}

				public int compute() {
					return script;
				}
				*/
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
				context.generateComputer(testMethod, valid.where());
				context.generateComputer(memory.getTyped(ColumnEntryMemory.COMPUTER), this.value);
			}
			else {
				/*
				public int get() {
					return script;
				}
				*/
				context.generateComputer(memory.getTyped(ColumnEntryMemory.GETTER), this.value);
			}
		}
	}

	public static interface Valid {

		public abstract ScriptUsage<GenericScriptTemplateUsage> where();

		public abstract ConstantValue getFallback();
	}
}