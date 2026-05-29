package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import builderb0y.bigglobe.classes.spec.TypeSpec;
import builderb0y.bigglobe.columns.scripted.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.dependencies.CyclicDependencyException;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class DecisionTreeContext {

	public final ColumnEntryRegistry columnEntryRegistry;
	public final Map<Holder<DecisionTreeSpec>, Holder<DecisionTreeSpec>> patches;
	public final TypeSpec expectedType;
	public final boolean is3D;
	public final Map<Holder<DecisionTreeSpec>, InsnTree> decisionTreeInvokers;
	/**
	under normal circumstances, I would use CyclicDependencyAnalyzer
	after all the decision trees had been converted to bytecode.
	this would have the advantage that all dependencies
	everywhere are checked at the same time.
	but due to the recursive nature of decision trees,
	a StackOverflowError is thrown before any bytecode gets emitted.
	so, I need a secondary dependency analysis specifically for decision trees.
	*/
	public final Set<Holder<DecisionTreeSpec>> stack;

	public DecisionTreeContext(
		ColumnEntryRegistry columnEntryRegistry,
		Map<Holder<DecisionTreeSpec>, Holder<DecisionTreeSpec>> patches,
		TypeSpec type,
		boolean is3D
	) {
		this.columnEntryRegistry = columnEntryRegistry;
		this.patches = patches;
		this.expectedType = type;
		this.is3D = is3D;
		this.decisionTreeInvokers = new HashMap<>();
		this.stack = new LinkedHashSet<>();
	}

	public MethodCompileContext newMethod(String baseName, Holder<DecisionTreeSpec> self, TypeInfo returnType) {
		return this.columnEntryRegistry.columnCompileContext.clazz.newMethod(
			ACC_PUBLIC,
			baseName + ColumnCompileContext.internalName(
				UnregisteredObjectException.getID(self),
				this.columnEntryRegistry.columnCompileContext.clazz.memberUniquifier++
			),
			returnType,
			this.yParameters()
		);
	}

	public TypeInfo expectedTypeInfo() {
		return this.expectedType.getTypeInfo();
	}

	public LazyVarInfo[] yParameters() {
		return this.is3D ? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) } : LazyVarInfo.ARRAY_FACTORY.empty();
	}

	public static InsnTree[] yArguments(boolean is3D) {
		return is3D ? new InsnTree[] { load("y", TypeInfos.INT) } : InsnTree.ARRAY_FACTORY.empty();
	}

	public InsnTree[] yArguments() {
		return yArguments(this.is3D);
	}

	public InsnTree yArgument() {
		return this.is3D ? load("y", TypeInfos.INT) : null;
	}

	public InsnTree loadColumn() {
		return this.columnEntryRegistry.columnCompileContext.loadColumn();
	}

	public InsnTree loadSeed(InsnTree salt) {
		return (
			this.is3D
			? ScriptedColumn.INFO.saltedPositionedSeed3D(this.loadColumn(), salt, load("y", TypeInfos.INT))
			: ScriptedColumn.INFO.saltedPositionedSeed(this.loadColumn(), salt)
		);
	}

	public InsnTree emitTree(Holder<DecisionTreeSpec> holder) throws DecisionTreeException {
		Holder<DecisionTreeSpec> replacement = this.patches != null ? this.patches.getOrDefault(holder, holder) : holder;
		if (this.stack.add(replacement)) try {
			InsnTree tree = this.decisionTreeInvokers.get(replacement);
			if (tree == null) {
				this.decisionTreeInvokers.put(replacement, tree = replacement.value().emitTree(this, replacement));
			}
			return tree;
		}
		catch (Exception exception) {
			DecisionTreeException detailedException = exception instanceof DecisionTreeException e ? e : new DecisionTreeException(exception);
			detailedException.details.add("Used by " + UnregisteredObjectException.getKey(replacement));
			throw detailedException;
		}
		finally {
			this.stack.remove(replacement);
		}
		else {
			throw new CyclicDependencyException(
				Stream
				.concat(
					this
					.stack
					.stream()
					.dropWhile((Holder<DecisionTreeSpec> compare) -> compare != replacement),
					Stream.of(replacement)
				)
				.map(UnregisteredObjectException::getID)
				.map(Identifier::toString)
				.collect(Collectors.joining(" -> "))
			);
		}
	}
}