package builderb0y.bigglobe.columns.scripted.decisionTrees.results;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted2.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ConstantDecisionTreeResult implements DecisionTreeResult {

	public final Data value;

	public ConstantDecisionTreeResult(Data value) {
		this.value = value;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.empty();
	}

	@Override
	public InsnTree createResult(Holder<DecisionTreeSettings> selfEntry, DataCompileContext context, AccessSchema schema, @Nullable InsnTree loadY) throws ScriptParsingException {
		return schema.createConstant(this.value, context.root());
	}
}