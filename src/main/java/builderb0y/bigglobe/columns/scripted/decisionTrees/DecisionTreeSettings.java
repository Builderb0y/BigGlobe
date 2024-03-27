package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.ColumnValueDependencyHolder;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;

@UseVerifier(name = "verify", in = DecisionTreeSettings.class, usage = MemberUsage.METHOD_IS_HANDLER)
public class DecisionTreeSettings implements ColumnValueDependencyHolder {

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
		try {
			if (this.result != null) {
				return this.result.createResult(context, accessSchema, loadY);
			}
			else {
				ConditionTree condition = this.condition.createCondition(selfEntry, Permuter.permute(0L, UnregisteredObjectException.getID(selfEntry)), context, loadY);
				InsnTree ifTrue = this.if_true.value().createInsnTree(this.if_true, accessSchema, context, loadY);
				InsnTree ifFalse = this.if_false.value().createInsnTree(this.if_false, accessSchema, context, loadY);
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
	}

	@Override
	public void addDependency(RegistryEntry<? extends ColumnValueDependencyHolder> entry) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<RegistryEntry<? extends ColumnValueDependencyHolder>> getDependencies() {
		Set<RegistryEntry<? extends ColumnValueDependencyHolder>> dependencies = new HashSet<>(64);
		this.addDependencies(dependencies);
		return dependencies;
	}

	public void addDependencies(Set<RegistryEntry<? extends ColumnValueDependencyHolder>> dependencies) {
		if (this.result != null) {
			dependencies.addAll(this.result.getDependencies());
		}
		else {
			dependencies.addAll(this.condition.getDependencies());
			this.if_true.value().addDependencies(dependencies);
			this.if_false.value().addDependencies(dependencies);
		}
	}
}