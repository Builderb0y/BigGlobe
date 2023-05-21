package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.bytecode.tree.instructions.update.ArrayUpdateInsnTree.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

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
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.type, CastMode.IMPLICIT_THROW);
			return switch (order) {
				case VOID -> new ArrayAssignVoidUpdateInsnTree(this.array, this.index, cast);
				case PRE  -> new  ArrayAssignPreUpdateInsnTree(this.array, this.index, cast);
				case POST -> new ArrayAssignPostUpdateInsnTree(this.array, this.index, cast);
			};
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.type, rightValue);
			return switch (order) {
				case VOID -> new ArrayVoidUpdateInsnTree(this.array, this.index, updater);
				case PRE  -> new  ArrayPreUpdateInsnTree(this.array, this.index, updater);
				case POST -> new ArrayPostUpdateInsnTree(this.array, this.index, updater);
			};
		}
	}
}