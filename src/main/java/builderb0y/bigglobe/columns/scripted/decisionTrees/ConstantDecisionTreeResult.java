package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.codecs.Any;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ConstantDecisionTreeResult implements DecisionTreeResult {

	public final @Any Object value;

	public ConstantDecisionTreeResult(@Any Object value) {
		this.value = value;
	}

	@Override
	public void addDependency(RegistryEntry<ColumnEntry> entry) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<RegistryEntry<ColumnEntry>> getDependencies() {
		return Collections.emptySet();
	}

	@Override
	public InsnTree createResult(DataCompileContext context, AccessSchema schema, @Nullable InsnTree loadY) {
		return schema.createConstant(this.value, context.root());
	}
}