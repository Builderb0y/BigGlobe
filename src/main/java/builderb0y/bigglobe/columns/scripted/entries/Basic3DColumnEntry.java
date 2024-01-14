package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.MappedRangeNumberArray;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class Basic3DColumnEntry implements ColumnEntry {

	public static final ColumnEntryMemory.Key<MethodCompileContext>
		COMPUTE_ONE = new ColumnEntryMemory.Key<>("computeOne"),
		COMPUTE_ALL = new ColumnEntryMemory.Key<>("computeAll"),
		EXTRACT     = new ColumnEntryMemory.Key<>("extract"),
		VALID_WHERE = new ColumnEntryMemory.Key<>("validWhere"),
		VALID_MIN_Y = new ColumnEntryMemory.Key<>("validMinY"),
		VALID_MAX_Y = new ColumnEntryMemory.Key<>("validMaxY");

	public abstract IValid valid();

	@Override
	public void populateField(ColumnEntryMemory memory, DataCompileContext context, FieldCompileContext getterMethod) {
		ColumnEntry.super.populateField(memory, context, getterMethod);
	}

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		IValid valid = this.valid();
		if (this.hasField()) {
			TypeInfo type = memory.getTyped(ColumnEntryMemory.TYPE).type();
			FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
			int flagIndex = memory.getTyped(ColumnEntryMemory.FLAGS_INDEX);
			String internalName = memory.getTyped(ColumnEntryMemory.INTERNAL_NAME);
			MethodCompileContext computeAllMethod = context.mainClass.newMethod(ACC_PUBLIC, "compute_" + internalName, TypeInfos.VOID);
			MethodCompileContext actualComputeAll = context.mainClass.newMethod(ACC_PUBLIC, "actually_compute_" + internalName, TypeInfos.VOID);
			memory.putTyped(COMPUTE_ALL, actualComputeAll);
			MethodCompileContext extractMethod = context.mainClass.newMethod(ACC_PUBLIC, "extract_" + internalName, type, TypeInfos.INT);
			memory.putTyped(EXTRACT, extractMethod);

			getterMethod.prepareParameters("y").setCode(
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
			String source;
			MutableScriptEnvironment environment = (
				new MutableScriptEnvironment()
				.addVariableRenamedGetField(context.loadSelf(), "valueField", valueField.info)
				.addMethodInvokes(MappedRangeNumberArray.class, "reallocateNone", "reallocateMin", "reallocateMax", "reallocateNone", "invalidate")
				.addFunctionInvoke("actuallyCompute", context.loadSelf(), actualComputeAll.info)
			);
			if (valid != null) {
				if (valid.where() != null) {
					MethodCompileContext test = context.mainClass.newMethod(ACC_PUBLIC, "test_" + internalName, TypeInfos.BOOLEAN);
					memory.putTyped(VALID_WHERE, test);
					environment.addFunctionInvoke("test", context.loadSelf(), test.info);
				}
				if (valid.min_y() != null) {
					MethodCompileContext minY = context.mainClass.newMethod(ACC_PUBLIC, "minY_" + internalName, TypeInfos.INT);
					memory.putTyped(VALID_MIN_Y, minY);
					environment.addFunctionInvoke("minY", context.loadSelf(), minY.info);
				}
				if (valid.max_y() != null) {
					MethodCompileContext maxY = context.mainClass.newMethod(ACC_PUBLIC, "maxY_" + internalName, TypeInfos.INT);
					memory.putTyped(VALID_MAX_Y, maxY);
					environment.addFunctionInvoke("maxY", context.loadSelf(), maxY.info);
				}
				if (valid.where() != null) {
					if (valid.min_y() != null) {
						if (valid.max_y() != null) {
							source = """
								if (test():
									valueField.reallocateBoth(minY(), maxY())
									actuallyCompute()
								)
								else (
									valueField.invalidate()
								)
							""";
						}
						else {
							source = """
								if (test():
									valueField.reallocateMin(minY())
									actuallyCompute()
								)
								else (
									valueField.invalidate()
								)
							""";
						}
					}
					else {
						if (valid.max_y() != null) {
							source = """
								if (test():
									valueField.reallocateMax(maxY())
									actuallyCompute()
								)
								else (
									valueField.invalidate()
								)
							""";
						}
						else {
							source = """
								if (test():
									valueField.reallocateNone()
									actuallyCompute()
								)
								else (
									valueField.invalidate()
								)
							""";
						}
					}
				}
				else {
					if (valid.min_y() != null) {
						if (valid.max_y() != null) {
							source = """
								valueField.reallocateBoth(minY(), maxY())
								actuallyCompute()
							""";
						}
						else {
							source = """
								valueField.reallocateMin(minY())
								actuallyCompute()
							""";
						}
					}
					else {
						if (valid.max_y() != null) {
							source = """
								valueField.reallocateMax(maxY())
								actuallyCompute()
							""";
						}
						else {
							source = """
								valueField.reallocateNone()
								actuallyCompute()
							""";
						}
					}
				}
			}
			else {
				source = """
					valueField.reallocateNone()
					actuallyCompute()
				""";
			}
			computeAllMethod.prepareParameters().setCode(source, environment);
			extractMethod.prepareParameters("y").setCode(
				"""
				var array = arrayField
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
				.addVariable("arrayField", getField(load(extractMethod.getParameter("this")), valueField.info))
				.addFieldGet("valid", MappedRangeNumberArray.VALID)
				.addFieldGet("minCached", MappedRangeNumberArray.MIN_CACHED)
				.addFieldGet("maxCached", MappedRangeNumberArray.MAX_CACHED)
				.addFieldGet("minAccessible", MappedRangeNumberArray.MIN_ACCESSIBLE)
				.addFieldGet("maxAccessible", MappedRangeNumberArray.MAX_ACCESSIBLE)
				.addFieldGet("array", MappedRangeNumberArray.ARRAY)
				.addMethodInvoke("get", switch (type.getSort()) {
					case BYTE -> MappedRangeNumberArray.GET_B;
					case SHORT -> MappedRangeNumberArray.GET_S;
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
				ConditionTree tree = ConstantConditionTree.TRUE;
				if (valid.where() != null) {

				}
			}
		}
	}

	@Override
	public void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {

	}

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {

	}

	public static interface IValid {

		public abstract @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where();

		public abstract @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y();

		public abstract @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y();

		public abstract ConstantValue getFallback();
	}

	public static abstract class Basic3DAccessSchema implements AccessSchema {

		@Override
		public boolean requiresYLevel() {
			return true;
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