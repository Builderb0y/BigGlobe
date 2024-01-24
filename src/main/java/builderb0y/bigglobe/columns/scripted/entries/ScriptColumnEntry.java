package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.MappedRangeArray;
import builderb0y.bigglobe.columns.scripted.MappedRangeNumberArray;
import builderb0y.bigglobe.columns.scripted.Valid;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema.TypeContext;
import builderb0y.bigglobe.columns.scripted.schemas.PrimitiveAccessSchema;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.BooleanToConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptColumnEntry extends AbstractColumnEntry {

	public final ScriptUsage<GenericScriptTemplateUsage> script;

	public ScriptColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		@DefaultBoolean(true) boolean cache,
		ScriptUsage<GenericScriptTemplateUsage> script
	) {
		super(params, valid, cache);
		this.script = script;
	}

	@Override
	public void populateCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		context.setMethodCode(computeMethod, this.script, false);
	}

	@Override
	public void populateCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		context.setMethodCode(computeMethod, this.script, true);
	}
}