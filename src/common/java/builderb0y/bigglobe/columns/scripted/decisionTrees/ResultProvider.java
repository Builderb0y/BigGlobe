package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.spec.MemberSpec;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

@UseCoder(name = "REGISTRY", in = ResultProvider.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface ResultProvider extends SimpleDependencyView, CoderRegistryTyped<ResultProvider> {

	public static final CoderRegistry<ResultProvider> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("decision_tree_result"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("constant"), ConstantResultProvider.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("script"),   ScriptedResultProvider.class);
	}};

	public abstract InsnTree emitValue(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException;

	public static record ConstantResultProvider(Data value) implements ResultProvider {

		@Override
		public InsnTree emitValue(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			return context.expectedType.parseConstant(context.columnEntryRegistry.classHierarchy, this.value);
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.empty();
		}
	}

	public static class ScriptedResultProvider implements ResultProvider, SetBasedMutableDependencyView {

		public final ScriptUsage script;
		public final transient Set<Holder<? extends DependencyView>> dependencies;

		public ScriptedResultProvider(ScriptUsage script) {
			this.script = script;
			this.dependencies = new HashSet<>();
			this.addAllDependencies(script);
		}

		@Override
		public InsnTree emitValue(DecisionTreeContext context, Holder<DecisionTreeSpec> caller, long seedSalt) throws ScriptParsingException, ConstantFormatException {
			MethodCompileContext method = context.newMethod("decision_tree_result_", caller, context.expectedTypeInfo());
			context.columnEntryRegistry.setMethodCode(method, this.script, context.loadColumn(), context.yArgument(), null, null, this, MemberSpec.NO_EXTRAS);
			return invokeInstance(context.loadColumn(), method.info, context.yArguments());
		}

		@Override
		public Set<Holder<? extends DependencyView>> getDependencies() {
			return this.dependencies;
		}
	}
}