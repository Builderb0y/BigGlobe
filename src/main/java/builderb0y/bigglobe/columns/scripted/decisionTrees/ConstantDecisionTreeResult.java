package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.codecs.Any;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ConstantDecisionTreeResult implements DecisionTreeResult {

	public final @Any Object value;

	public ConstantDecisionTreeResult(@Any Object value) {
		this.value = value;
	}

	@Override
	public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
		return Stream.empty();
	}

	@Override
	public InsnTree createResult(RegistryEntry<DecisionTreeSettings> selfEntry, DataCompileContext context, AccessSchema schema, @Nullable InsnTree loadY) throws ScriptParsingException {
		return schema.createConstant(this.value, context.root());
	}
}