package builderb0y.bigglobe.columns.scripted.entries;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.MappedRangeNumberArray;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class Numeric3DColumnEntry implements ColumnEntry {

	public abstract boolean isCached();

	public abstract IValid valid();

	@Override
	public void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		int uniqueIndex = context.mainClass.memberUniquifier++;
		int flagIndex = context.flagsIndex++;
		memory.putTyped(ColumnEntryMemory.FLAGS_INDEX, flagIndex);
		Identifier accessID = memory.getTyped(ColumnEntryMemory.ACCESSOR_ID);
		TypeInfo type = memory.getTyped(ColumnEntryMemory.TYPE).type();
		String internalName = DataCompileContext.internalName(accessID, uniqueIndex);
		memory.putTyped(ColumnEntryMemory.INTERNAL_NAME, internalName);
		MethodCompileContext getterMethod = context.mainClass.newMethod(ACC_PUBLIC, "get_" + internalName, type);
		memory.putTyped(ColumnEntryMemory.GETTER, getterMethod);

		IValid valid = this.valid();
		if (this.isCached()) {
			FieldCompileContext valueField = context.mainClass.newField(ACC_PUBLIC, internalName, type(MappedRangeNumberArray.class));

			MethodCompileContext computeOneMethod = context.mainClass.newMethod(ACC_PUBLIC, "compute_" + internalName, type, TypeInfos.INT);
			memory.putTyped(ColumnEntryMemory.COMPUTER, computeOneMethod);
			MethodCompileContext computeAllMethod = context.mainClass.newMethod(ACC_PUBLIC, "compute_" + internalName, TypeInfos.VOID);
			MethodCompileContext extractMethod = context.mainClass.newMethod(ACC_PUBLIC, "extract_" + internalName, type, TypeInfos.INT);

			getterMethod.prepareParameters().setCode(
				"""
				int oldFlags = flagsField
				int newFlags = oldFlags | flagsBitmask
				if (oldFlags != newFlags:
					flagsField = newFlags
					compute()
				)
				return(extract(y))
				""",
				new MutableScriptEnvironment()
				.addVariableRenamedGetField(context.loadSelf(), "flagsField", context.flagsField(flagIndex))
				.addVariableConstant("flagsBitmask", DataCompileContext.flagsFieldBitmask(flagIndex))
				.addFunctionInvoke("compute", context.loadSelf(), computeAllMethod.info)
				.addFunctionInvoke("extract", context.loadSelf(), extractMethod.info)
			);

			computeAllMethod.prepareParameters().setCode(
				"""
				
				""",
				new MutableScriptEnvironment()
			);

			extractMethod.prepareParameters("y").setCode(
				"""
				unless (array.valid: return(fallback))
				if (y >= array.minCached && y < array.maxCached:
					return array.array.get(y - array.minCached)
				)
				"""
				+ (
					valid != null
					? (
						valid.min_y() != null
						? (
							valid.max_y() != null
							? "if (y >= array.minAccessible && y < array.maxAccessible: return(compute(y))\nreturn(fallback)"
							: "if (y >= array.minAccessible: return(compute(y))\nreturn(fallback)"
						)
						: (
							valid.max_y() != null
							? "if (y < array.maxAccessible: return(compute(y)))\nreturn(fallback)"
							: "return(compute(y))"
						)
					)
					: "return(compute(y))"
				),
				new MutableScriptEnvironment()
				.addVariableLoad("y", extractMethod.getParameter("y"))
				.addVariable("array", getField(load(extractMethod.getParameter("this")), valueField.info))
				.addFieldGet("valid", MappedRangeNumberArray.VALID)
				.addFieldGet("minCached", MappedRangeNumberArray.MIN_CACHED)
				.addFieldGet("maxCached", MappedRangeNumberArray.MAX_CACHED)
				.addFieldGet("minAccessible", MappedRangeNumberArray.MIN_ACCESSIBLE)
				.addFieldGet("maxAccessible", MappedRangeNumberArray.MAX_ACCESSIBLE)
				.addFieldGet("array", MappedRangeNumberArray.ARRAY)
				.addMethodInvoke("get", switch (type.getSort()) {
					case INT -> MappedRangeNumberArray.GET_I;
					case LONG -> MappedRangeNumberArray.GET_L;
					case FLOAT -> MappedRangeNumberArray.GET_F;
					case DOUBLE -> MappedRangeNumberArray.GET_D;
					default -> throw new IllegalStateException("Unsupported type: " + type);
				})
				.addVariableConstant("fallback", valid != null ? valid.getFallback() : ConstantValue.of(0))
				.addFunctionInvoke("compute", context.loadSelf(), computeAllMethod.info)
			);
		}
		else {
			if (valid != null) {

			}
			else {

			}
		}
	}

	@Override
	public void setupEnvironment(ColumnEntryMemory memory, DataCompileContext context) {

	}

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {

	}

	public static interface IValid {

		public abstract ScriptUsage<GenericScriptTemplateUsage> where();

		public abstract ScriptUsage<GenericScriptTemplateUsage> min_y();

		public abstract ScriptUsage<GenericScriptTemplateUsage> max_y();

		public abstract ConstantValue getFallback();
	}
}