package builderb0y.scripting.bytecode.tree.flow;

import java.util.Arrays;
import java.util.stream.Stream;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SequenceInsnTree implements InsnTree {

	public InsnTree[] statements;

	public SequenceInsnTree(InsnTree... statements) {
		this.statements = (
			Arrays
			.stream(statements)
			.flatMap(statement -> (
				statement instanceof SequenceInsnTree sequence
				? Arrays.stream(sequence.statements)
				: Stream.of(statement)
			))
			.toArray(InsnTree.ARRAY_FACTORY)
		);
		for (int index = 0, limit = this.statements.length - 1; index < limit; index++) {
			this.statements[index] = this.statements[index].asStatement();
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		for (InsnTree statement : this.statements) {
			statement.emitBytecode(method);
			method.node.visitLabel(label());
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.statements[this.statements.length - 1].getTypeInfo();
	}

	@Override
	public boolean jumpsUnconditionally() {
		return Arrays.stream(this.statements).anyMatch(InsnTree::jumpsUnconditionally);
	}

	@Override
	public boolean canBeStatement() {
		return this.statements[this.statements.length - 1].canBeStatement();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree newLast = this.statements[this.statements.length - 1].cast(parser, type, mode);
		if (newLast == null) return null;
		InsnTree[] newStatements = this.statements.clone();
		newStatements[newStatements.length - 1] = newLast;
		return new SequenceInsnTree(newStatements);
	}

	@Override
	public InsnTree asStatement() {
		InsnTree newLast = this.statements[this.statements.length - 1].asStatement();
		InsnTree[] newStatements = this.statements.clone();
		newStatements[newStatements.length - 1] = newLast;
		return new SequenceInsnTree(newStatements);
	}
}