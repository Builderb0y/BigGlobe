package builderb0y.bigglobe.columns.scripted.decisionTrees.results;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted2.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedDecitionTreeResult implements DecisionTreeResult, SetBasedMutableDependencyView {

	public final ScriptUsage script;
	public final transient Set<Holder<? extends DependencyView>> dependencies = new HashSet<>();

	public ScriptedDecitionTreeResult(ScriptUsage script) {
		this.script = script;
		this.addAllDependencies(script);
	}

	@Override
	public Set<Holder<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}

	@Override
	public InsnTree createResult(Holder<DecisionTreeSettings> selfEntry, DataCompileContext context, AccessSchema accessSchema, @Nullable InsnTree loadY) throws ScriptParsingException {
		MethodCompileContext decisionTreeMethod = context.mainClass.newMethod(
			ACC_PUBLIC,
			"decision_tree_result_" + DataCompileContext.internalName(UnregisteredObjectException.getID(selfEntry), context.mainClass.memberUniquifier++),
			context.root().getTypeContext(accessSchema.type()).type(),
			loadY != null ? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) } : LazyVarInfo.ARRAY_FACTORY.empty()
		);
		context.setMethodCode(decisionTreeMethod, this.script, loadY != null, this, null, context.root().registry.parserFlags());
		return invokeInstance(context.loadSelf(), decisionTreeMethod.info, loadY != null ? new InsnTree[] { loadY } : InsnTree.ARRAY_FACTORY.empty());
	}
}