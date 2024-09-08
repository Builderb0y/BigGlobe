package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedDecitionTreeResult implements DecisionTreeResult, SetBasedMutableDependencyView {

	public final ScriptUsage script;
	public final transient Set<RegistryEntry<? extends DependencyView>> dependencies = new HashSet<>();

	public ScriptedDecitionTreeResult(ScriptUsage script) {
		this.script = script;
	}

	@Override
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}

	@Override
	public InsnTree createResult(RegistryEntry<DecisionTreeSettings> selfEntry, DataCompileContext context, AccessSchema accessSchema, @Nullable InsnTree loadY) throws ScriptParsingException {
		MethodCompileContext decisionTreeMethod = context.mainClass.newMethod(
			ACC_PUBLIC,
			"decision_tree_result_" + DataCompileContext.internalName(UnregisteredObjectException.getID(selfEntry), context.mainClass.memberUniquifier++),
			context.root().getTypeContext(accessSchema.type()).type(),
			loadY != null ? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) } : LazyVarInfo.ARRAY_FACTORY.empty()
		);
		context.setMethodCode(decisionTreeMethod, this.script, loadY != null, this, null);
		return invokeInstance(context.loadSelf(), decisionTreeMethod.info, loadY != null ? new InsnTree[] { loadY } : InsnTree.ARRAY_FACTORY.empty());
	}
}