package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.TypelessCoderRegistry;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted2.dependencies.CyclicDependencyException;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

@UseCoder(name = "createCoder", in = DecisionTreeSettings.class, usage = MemberUsage.METHOD_IS_FACTORY)
public abstract class DecisionTreeSettings implements SimpleDependencyView {

	public static final AutoCoder<DecisionTreeSettings> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(DecisionTreeSettings.class);

	public static AutoCoder<DecisionTreeSettings> createCoder(FactoryContext<DecisionTreeSettings> context) {
		TypelessCoderRegistry<DecisionTreeSettings> registry = new TypelessCoderRegistry<>(context.type, context.autoCodec);
		registry.register(ConditionBasedDecisionTreeSettings.class);
		registry.register(ResultBasedDecisionTreeSettings.class);
		return registry;
	}

	public InsnTree createInsnTree(
		Holder<DecisionTreeSettings> selfEntry,
		AccessSchema accessSchema,
		DataCompileContext context,
		@Nullable Map<Holder<DecisionTreeSettings>, Holder<DecisionTreeSettings>> patches,
		@Nullable InsnTree loadY
	) {
		return new Context(accessSchema, context, loadY, patches).createInsnTree(selfEntry);
	}

	public abstract InsnTree doCreateResult(Holder<DecisionTreeSettings> selfEntry, Context context) throws ScriptParsingException;

	public static class Context {

		public final AccessSchema accessSchema;
		public final DataCompileContext dataContext;
		public final @Nullable InsnTree loadY;
		public final Map<Holder<DecisionTreeSettings>, Holder<DecisionTreeSettings>> patches;
		/**
		under normal circumstances, I would use CyclicDependencyAnalyzer
		after all the decision trees had been converted to bytecode.
		this would have the advantage that all dependencies
		everywhere are checked at the same time.
		but due to the recursive nature of decision trees,
		a StackOverflowError is thrown before any bytecode gets emitted.
		so, I need a secondary dependency analysis specifically for decision trees.
		*/
		public final Set<Holder<DecisionTreeSettings>> stack;

		public Context(
			AccessSchema accessSchema,
			DataCompileContext dataContext,
			@Nullable InsnTree loadY,
			Map<Holder<DecisionTreeSettings>, Holder<DecisionTreeSettings>> patches
		) {
			this.accessSchema = accessSchema;
			this.dataContext = dataContext;
			this.loadY = loadY;
			this.patches = patches;
			this.stack = new ReferenceLinkedOpenHashSet<>(16);
		}

		public InsnTree createInsnTree(Holder<DecisionTreeSettings> selfEntry) {
			if (this.patches != null) selfEntry = this.patches.getOrDefault(selfEntry, selfEntry);
			if (!this.stack.add(selfEntry)) {
				Holder<DecisionTreeSettings> selfEntry_ = selfEntry;
				throw new CyclicDependencyException(
					Stream
						.concat(
							this.stack
								.stream()
								.dropWhile((Holder<? extends DependencyView> compare) -> compare != selfEntry_),
							Stream.of(selfEntry)
						)
						.map(UnregisteredObjectException::getID)
						.map(Identifier::toString)
						.collect(Collectors.joining(" -> "))
				);
			}
			try {
				return selfEntry.value().doCreateResult(selfEntry, this);
			}
			catch (Exception exception) {
				DecisionTreeException detailedException = exception instanceof DecisionTreeException e ? e : new DecisionTreeException(exception);
				detailedException.details.add("Used by " + UnregisteredObjectException.getKey(selfEntry));
				throw detailedException;
			}
			finally {
				this.stack.remove(selfEntry);
			}
		}
	}
}