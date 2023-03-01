package builderb0y.scripting.bytecode.tree.flow;

import java.util.Arrays;
import java.util.stream.Stream;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.NoopInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

public class SequenceInsnTree implements InsnTree {

	public InsnTree[] statements;

	public SequenceInsnTree(ExpressionParser parser, InsnTree... statements) {
		this.statements = (
			Arrays
			.stream(statements)
			.flatMap(statement -> (
				statement instanceof SequenceInsnTree sequence
				? Arrays.stream(sequence.statements)
				: Stream.of(statement)
			))
			.filter(statement -> !(statement instanceof NoopInsnTree))
			.toArray(InsnTree.ARRAY_FACTORY)
		);
		for (int index = 0, limit = statements.length - 1; index < limit; index++) {
			this.statements[index] = this.statements[index].cast(parser, TypeInfos.VOID, CastMode.EXPLICIT_THROW);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		for (InsnTree statement : this.statements) {
			statement.emitBytecode(method);
			method.node.visitLabel(new Label());
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.statements[this.statements.length - 1].getTypeInfo();
	}

	@Override
	public boolean returnsUnconditionally() {
		return Arrays.stream(this.statements).anyMatch(InsnTree::returnsUnconditionally);
	}

	@Override
	public boolean canBeStatement() {
		return this.statements[this.statements.length - 1].canBeStatement();
	}

	@Override
	public InsnTree then(ExpressionParser parser, InsnTree nextStatement) {
		InsnTree[] newStatements = this.statements.clone();
		newStatements[newStatements.length - 1] = newStatements[newStatements.length - 1].then(parser, nextStatement);
		return new SequenceInsnTree(parser, newStatements);
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree newLast = this.statements[this.statements.length - 1].cast(parser, type, mode);
		if (newLast == null) return null;
		InsnTree[] newStatements = this.statements.clone();
		newStatements[newStatements.length - 1] = newLast;
		return new SequenceInsnTree(parser, newStatements);
	}
}