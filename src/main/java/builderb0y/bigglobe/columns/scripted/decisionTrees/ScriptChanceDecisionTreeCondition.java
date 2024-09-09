package builderb0y.bigglobe.columns.scripted.decisionTrees;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.BooleanToConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptChanceDecisionTreeCondition extends DecisionTreeCondition.Impl {

	public final ScriptUsage script;

	public ScriptChanceDecisionTreeCondition(ScriptUsage script) {
		this.script = script;
	}

	@Override
	public ConditionTree createCondition(RegistryEntry<DecisionTreeSettings> selfEntry, long selfSeed, DataCompileContext context, @Nullable InsnTree loadY) throws ScriptParsingException {
		MethodCompileContext decisionTreeMethod = context.mainClass.newMethod(
			ACC_PUBLIC,
			"decision_tree_chance_" + DataCompileContext.internalName(UnregisteredObjectException.getID(selfEntry), context.mainClass.memberUniquifier++),
			TypeInfos.DOUBLE,
			loadY != null ? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) } : LazyVarInfo.ARRAY_FACTORY.empty()
		);
		context.setMethodCode(decisionTreeMethod, this.script, loadY != null, this, null);
		return new BooleanToConditionTree(
			RandomScriptEnvironment.PERMUTER_INFO.toChancedBooleanD(
				ScriptedColumn.INFO.saltedPositionedSeed(
					context.loadColumn(),
					ldc(selfSeed)
				),
				invokeInstance(
					context.loadSelf(),
					decisionTreeMethod.info,
					loadY != null ? new InsnTree[] { loadY } : InsnTree.ARRAY_FACTORY.empty()
				)
			)
		);
	}
}