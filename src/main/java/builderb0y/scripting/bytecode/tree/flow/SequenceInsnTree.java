package builderb0y.scripting.bytecode.tree.flow;

import java.util.Arrays;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SequenceInsnTree implements InsnTree {

	public InsnTree[] statements;

	public SequenceInsnTree(InsnTree... statements) {
		this(statements, false);
	}

	public SequenceInsnTree(InsnTree[] statements, boolean trusted) {
		this.statements = trusted ? statements : flatten(statements);
		assert isArrayValid(this.statements) : "Invalid statement array after flattening.";
	}

	/** I expect this method to be a bit of a hot spot, so optimize the hell out of it! */
	public static InsnTree[] flatten(InsnTree[] statements) {
		//count elements so we only need to allocate an array once.
		int flattenedLength = 0;
		for (int index = 0, length = statements.length; index < length; index++) {
			InsnTree statement = statements[index];
			if (statement == null) {
				throw new NullPointerException("Null statement at index " + index);
			}
			flattenedLength += statement instanceof SequenceInsnTree sequence ? sequence.statements.length : 1;
		}
		//now flatten all the elements.
		InsnTree[] result = new InsnTree[flattenedLength];
		int writeIndex = 0;
		for (int readIndex = 0, length = statements.length; readIndex < length; readIndex++) {
			InsnTree statement = statements[readIndex];
			if (statement instanceof SequenceInsnTree sequence) {
				System.arraycopy(sequence.statements, 0, result, writeIndex, sequence.statements.length);
				writeIndex += sequence.statements.length;
			}
			else {
				result[writeIndex++] = statement;
			}
			//when appending a sequence, only the last statement in that sequence
			//needs to have asStatement() called on it. all the other statements
			//in the sequence are guaranteed to already be statements.
			//
			//when appending a single element, that element
			//always needs to have asStatement() called on it.
			//
			//in either case, after an append operation is complete,
			//asStatement() needs to be called on the last element in result.
			//except, asStatement() should NOT be called on the last element
			//in result when result is completely full and contains an element
			//at every index. hence the length check below.
			if (writeIndex != flattenedLength) {
				result[writeIndex - 1] = result[writeIndex - 1].asStatement();
			}
		}
		return result;
	}

	/** separate method to we can avoid calling it when asserts are disabled. */
	public static boolean isArrayValid(InsnTree[] array) {
		int limit = array.length - 1;
		if (limit <= 0) {
			return false;
		}
		for (int index = 0; index <= limit; index++) {
			InsnTree tree = array[index];
			if (tree == null) {
				return false;
			}
			if (index < limit && tree.getTypeInfo().isValue()) {
				return false;
			}
		}
		return true;
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
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree oldLast = this.statements[this.statements.length - 1];
		InsnTree newLast = oldLast.cast(parser, type, mode);
		if (newLast == null) return null;
		if (newLast == oldLast) return this;
		InsnTree[] newStatements = this.statements.clone();
		newStatements[newStatements.length - 1] = newLast;
		return new SequenceInsnTree(newStatements, true);
	}

	@Override
	public boolean canBeStatement() {
		return this.statements[this.statements.length - 1].canBeStatement();
	}

	@Override
	public InsnTree asStatement() {
		InsnTree oldLast = this.statements[this.statements.length - 1];
		InsnTree newLast = oldLast.asStatement();
		if (newLast == oldLast) return this;
		InsnTree[] newStatements = this.statements.clone();
		newStatements[newStatements.length - 1] = newLast;
		return new SequenceInsnTree(newStatements, true);
	}
}