package builderb0y.bigglobe.columns.scripted.entries;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.Valid;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeContext;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeException;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSpec;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

public class DecisionTreeColumnEntry extends LoopColumnEntry {

	public final Holder<DecisionTreeSpec> root;
	public final @VerifyNullable Map<Holder<DecisionTreeSpec>, Holder<DecisionTreeSpec>> patches;

	public DecisionTreeColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		boolean cache,
		Holder<DecisionTreeSpec> root,
		@VerifyNullable Map<Holder<DecisionTreeSpec>, Holder<DecisionTreeSpec>> patches
	) {
		super(params, valid, cache);
		this.root = root;
		this.patches = patches;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		Stream<Holder<? extends DependencyView>> result = Stream.concat(super.streamDirectDependencies(), Stream.of(this.root));
		if (this.patches != null) {
			result = Stream.of(result, this.patches.keySet().stream(), this.patches.values().stream()).flatMap(Function.identity());
		}
		return result;
	}

	@Override
	public InsnTree makeComputer(ColumnEntryRegistry registry, ColumnEntryContext context, @Nullable InsnTree loadY) throws ScriptParsingException {
		try {
			return (
				new DecisionTreeContext(
					registry,
					this.patches,
					this.params.typeSpec(registry, this),
					this.params.is_3d()
				)
				.emitTree(this.root)
			);
		}
		catch (DecisionTreeException exception) {
			throw new ScriptParsingException(exception, null);
		}
	}
}