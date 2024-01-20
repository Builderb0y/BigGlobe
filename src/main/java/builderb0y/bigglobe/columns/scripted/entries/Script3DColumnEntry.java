package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.MappedRangeNumberArray;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

public abstract class Script3DColumnEntry extends Basic3DColumnEntry {

	public final ScriptUsage<GenericScriptTemplateUsage> value;
	public final @DefaultBoolean(true) boolean cache;

	public Script3DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, boolean cache) {
		this.value = value;
		this.cache = cache;
	}

	@Override
	public boolean hasField() {
		return this.cache;
	}

	@Override
	public void populateComputeOne(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeOneMethod) throws ScriptParsingException {
		context.setMethodCode(computeOneMethod, this.value, true);
	}

	@Override
	public void populateComputeAll(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeAllMethod) {
		TypeContext type = memory.getTyped(ColumnEntryMemory.TYPE);
		computeAllMethod.setCode(
			"""
			var array = valueField
			int minY = array.minCached
			int maxY = array.maxCached
			var actualArray = array.array
			for (int y = minY, y < maxY, ++y:
				actualArray.set(y - minY, compute(y))
			)
			""",
			new MutableScriptEnvironment()
			.addVariableRenamedGetField(context.loadSelf(), "valueField", memory.getTyped(ColumnEntryMemory.FIELD).info)
			.addVariableLoad("y", TypeInfos.INT)
			.addFieldGet("minCached", MappedRangeNumberArray.MIN_CACHED)
			.addFieldGet("maxCached", MappedRangeNumberArray.MAX_CACHED)
			.addFieldGet("array", MappedRangeNumberArray.ARRAY)
			.addMethodInvoke("set", switch (type.exposedType().getSort()) {
				case BYTE    -> MappedRangeNumberArray.SET_B;
				case SHORT   -> MappedRangeNumberArray.SET_S;
				case INT     -> MappedRangeNumberArray.SET_I;
				case LONG    -> MappedRangeNumberArray.SET_L;
				case FLOAT   -> MappedRangeNumberArray.SET_F;
				case DOUBLE  -> MappedRangeNumberArray.SET_D;
				case BOOLEAN -> MappedRangeNumberArray.SET_Z;
				default -> throw new IllegalStateException("Unsupported type: " + type);
			})
			.addFunctionInvoke("compute", context.loadSelf(), memory.getTyped(COMPUTE_ONE).info)
		);
	}
}