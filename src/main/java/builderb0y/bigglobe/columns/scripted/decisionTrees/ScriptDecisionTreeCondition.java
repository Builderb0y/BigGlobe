package builderb0y.bigglobe.columns.scripted.decisionTrees;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.BooleanToConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptDecisionTreeCondition extends DecisionTreeCondition.Impl {

	public final ScriptUsage<GenericScriptTemplateUsage> script;

	public ScriptDecisionTreeCondition(ScriptUsage<GenericScriptTemplateUsage> script) {
		this.script = script;
	}

	@Override
	public ConditionTree createCondition(RegistryEntry<DecisionTreeSettings> selfEntry, long selfSeed, DataCompileContext context, @Nullable InsnTree loadY) throws ScriptParsingException {
		MethodCompileContext decisionTreeMethod = context.mainClass.newMethod(
			ACC_PUBLIC,
			"decision_tree_" + DataCompileContext.internalName(UnregisteredObjectException.getID(selfEntry), context.mainClass.memberUniquifier++),
			TypeInfos.BOOLEAN,
			loadY != null ? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) } : LazyVarInfo.ARRAY_FACTORY.empty()
		);
		context.setMethodCode(decisionTreeMethod, this.script, loadY != null, this);
		return new BooleanToConditionTree(invokeInstance(context.loadSelf(), decisionTreeMethod.info, loadY != null ? new InsnTree[] { loadY } : InsnTree.ARRAY_FACTORY.empty()));
	}
}