package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.bytecode.tree.instructions.update.ArrayUpdateInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ArrayLoadInsnTree implements InsnTree {

	public InsnTree array;
	public InsnTree index;
	public TypeInfo type;

	public ArrayLoadInsnTree(InsnTree array, InsnTree index) {
		this.array = array;
		this.index = index;
		this.type  = array.getTypeInfo().componentType;
		if (this.type == null) throw new IllegalArgumentException("Not an array: " + array.getTypeInfo());
	}

	public static ArrayLoadInsnTree create(InsnTree array, InsnTree index) {
		if (!array.getTypeInfo().isArray()) {
			throw new InvalidOperandException("Array must be array-typed.");
		}
		if (!index.getTypeInfo().isSingleWidthInt()) {
			throw new InvalidOperandException("Index must be single-width int.");
		}
		return new ArrayLoadInsnTree(array, index);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.array.emitBytecode(method);
		this.index.emitBytecode(method);
		method.node.visitInsn(this.type.getOpcode(IALOAD));
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	@Override
	public InsnTree then(ExpressionParser parser, InsnTree nextStatement) {
		return this.array.then(parser, this.index).then(parser, nextStatement);
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			return arrayStore(this.array, this.index, rightValue);
		}
		return new ArrayUpdateInsnTree(this.array, this.index, op.createUpdater(parser, this.getTypeInfo(), rightValue));
	}
}