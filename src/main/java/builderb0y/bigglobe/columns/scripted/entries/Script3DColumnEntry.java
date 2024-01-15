package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

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
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		MethodCompileContext computeMethod = memory.getTyped(COMPUTE_ONE);
		computeMethod.prepareParameters("y").setCode(
			"""
			""",
			new MutableScriptEnvironment()
			.addAll(MathScriptEnvironment.INSTANCE)
			.addAll(context.environment)
			.addVariableLoad(computeMethod.getParameter("y"))
		);
	}
}