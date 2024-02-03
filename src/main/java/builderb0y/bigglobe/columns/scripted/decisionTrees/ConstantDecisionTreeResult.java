package builderb0y.bigglobe.columns.scripted.decisionTrees;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.codecs.Any;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ConstantDecisionTreeResult implements DecisionTreeResult {

	public final @Any Object value;

	public ConstantDecisionTreeResult(@Any Object value) {
		this.value = value;
	}

	@Override
	public InsnTree createResult(DataCompileContext context, AccessSchema schema, @Nullable InsnTree loadY) {
		return schema.createConstant(this.value, context.root());
	}
}