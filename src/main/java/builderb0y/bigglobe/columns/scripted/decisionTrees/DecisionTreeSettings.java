package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.CyclicDependencyException;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;

@UseVerifier(name = "verify", in = DecisionTreeSettings.class, usage = MemberUsage.METHOD_IS_HANDLER)
public class DecisionTreeSettings implements DependencyView {

	public static final AutoCoder<DecisionTreeSettings> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(DecisionTreeSettings.class);

	public final @VerifyNullable DecisionTreeResult result;
	public final @VerifyNullable DecisionTreeCondition condition;
	public final @VerifyNullable RegistryEntry<DecisionTreeSettings> if_true, if_false;

	public DecisionTreeSettings(
		@VerifyNullable DecisionTreeResult result,
		@VerifyNullable DecisionTreeCondition condition,
		@VerifyNullable RegistryEntry<DecisionTreeSettings> if_true,
		@VerifyNullable RegistryEntry<DecisionTreeSettings> if_false
	) {
		this.result    = result;
		this.condition = condition;
		this.if_true   = if_true;
		this.if_false  = if_false;
	}

	@SuppressWarnings("deprecation")
	public static <T_Encoded> void verify(VerifyContext<T_Encoded, DecisionTreeSettings> context) throws VerifyException {
		DecisionTreeSettings object = context.object;
		if (object == null) return;
		if (object.result != null) {
			if (object.condition != null || object.if_true != null || object.if_false != null) {
				throw new VerifyException("Must specify EITHER condition, if_true, and if_false, OR result. But not both.");
			}
		}
		else {
			if (object.condition == null || object.if_true == null || object.if_false == null) {
				throw new VerifyException("Must specify EITHER condition, if_true, and if_false, OR result. But not both.");
			}
		}
	}

	public InsnTree createInsnTree(
		RegistryEntry<DecisionTreeSettings> selfEntry,
		AccessSchema accessSchema,
		DataCompileContext context,
		@Nullable InsnTree loadY
	) {
		return this.createInsnTree(selfEntry, accessSchema, context, loadY, new ReferenceLinkedOpenHashSet<>(16));
	}

	/**
	under normal circumstances, I would use CyclicDependencyAnalyzer
	after all the decision trees had been converted to bytecode.
	this would have the advantage that all dependencies
	everywhere are checked at the same time.
	but due to the recursive nature of decision trees,
	a StackOverflowError is thrown before any bytecode gets emitted.
	so, I need a secondary dependency analysis specifically for decision trees.
	*/
	public InsnTree createInsnTree(
		RegistryEntry<DecisionTreeSettings> selfEntry,
		AccessSchema accessSchema,
		DataCompileContext context,
		@Nullable InsnTree loadY,
		Set<RegistryEntry<DecisionTreeSettings>> stack
	) {
		if (!stack.add(selfEntry)) {
			throw new CyclicDependencyException(
				Stream
				.concat(
					stack
					.stream()
					.dropWhile((RegistryEntry<? extends DependencyView> compare) -> compare != selfEntry),
					Stream.of(selfEntry)
				)
				.map(UnregisteredObjectException::getID)
				.map(Identifier::toString)
				.collect(Collectors.joining(" -> "))
			);
		}
		try {
			if (this.result != null) {
				return this.result.createResult(selfEntry, context, accessSchema, loadY);
			}
			else {
				ConditionTree condition = this.condition.createCondition(selfEntry, Permuter.permute(0L, UnregisteredObjectException.getID(selfEntry)), context, loadY);
				InsnTree ifTrue = this.if_true.value().createInsnTree(this.if_true, accessSchema, context, loadY, stack);
				InsnTree ifFalse = this.if_false.value().createInsnTree(this.if_false, accessSchema, context, loadY, stack);
				if (!ifTrue.getTypeInfo().equals(ifFalse.getTypeInfo())) {
					throw new DecisionTreeException(UnregisteredObjectException.getKey(this.if_true) + " and " + UnregisteredObjectException.getKey(this.if_false) + " do not have the same return type.");
				}
				return new IfElseInsnTree(condition, ifTrue, ifFalse, ifTrue.getTypeInfo());
			}
		}
		catch (Exception exception) {
			DecisionTreeException detailedException = exception instanceof DecisionTreeException e ? e : new DecisionTreeException(exception);
			detailedException.details.add("Used by " + UnregisteredObjectException.getKey(selfEntry));
			throw detailedException;
		}
		finally {
			stack.remove(selfEntry);
		}
	}

	@Override
	public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
		if (this.result != null) {
			return this.result.streamDirectDependencies();
		}
		else {
			return Stream.of(this.if_true, this.if_false);
		}
	}
}