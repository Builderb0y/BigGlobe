package builderb0y.bigglobe.columns.scripted2.entries;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.Valid;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class DecisionTreeColumnEntry extends LoopColumnEntry {

	public final Holder<DecisionTreeSettings> root;
	public final @VerifyNullable Map<Holder<DecisionTreeSettings>, Holder<DecisionTreeSettings>> patches;

	public DecisionTreeColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		boolean cache,
		Holder<DecisionTreeSettings> root,
		@VerifyNullable Map<Holder<DecisionTreeSettings>, Holder<DecisionTreeSettings>> patches
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
	public InsnTree makeComputer(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ScriptParsingException {
		if (true) throw new UnsupportedOperationException("todo: implement decision trees");
		MethodCompileContext method = registry.columnCompileContext.clazz.newMethod(
			ACC_PUBLIC,
			"decisionTree_" + context.internalName,
			ElementSpec.asType(this.params.type()).getTypeInfo(),
			this.params.is_3d()
				? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) }
				: LazyVarInfo.ARRAY_FACTORY.empty()
		);
		//return_(this.root.value().createInsnTree()).emitBytecode(method);
		//method.endCode();
		return invokeInstance(
			registry.columnCompileContext.loadColumn(),
			method.info,
			this.params.is_3d()
				? new InsnTree[] { load("y", TypeInfos.INT) }
				: InsnTree.ARRAY_FACTORY.empty()
		);
	}
}