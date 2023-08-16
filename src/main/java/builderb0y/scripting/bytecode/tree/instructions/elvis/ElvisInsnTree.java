package builderb0y.scripting.bytecode.tree.instructions.elvis;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ElvisInsnTree implements InsnTree {

	public InsnTree value, alternative;
	public TypeInfo type;

	public ElvisInsnTree(InsnTree value, InsnTree alternative, TypeInfo type) {
		this.value       = value;
		this.alternative = alternative;
		this.type        = type;
	}

	public static InsnTree create(ExpressionParser parser, InsnTree value, InsnTree alternative) {
		IfElseInsnTree.Operands operands = IfElseInsnTree.Operands.of(parser, value, alternative);
		return new ElvisInsnTree(operands.trueBody(), operands.falseBody(), operands.type());
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label end = label();
		this.value.emitBytecode(method);
		dupAndJumpIfNonNull(this.value.getTypeInfo(), end, method);
		method.node.visitInsn(this.value.getTypeInfo().isDoubleWidth() ? POP2 : POP); //value is still on stack, and guaranteed to be null. pop it.
		this.alternative.emitBytecode(method);
		method.node.visitLabel(end);
	}

	public static void dupAndJumpIfNonNull(TypeInfo type, Label ifNonNull, MethodCompileContext method) {
		switch (type.getSort()) {
			case BOOLEAN, BYTE, CHAR, SHORT, INT, LONG -> {
				method.node.visitJumpInsn(GOTO, ifNonNull);
			}
			case FLOAT -> {
				method.node.visitInsn(DUP);
				method.node.visitInsn(DUP);
				method.node.visitInsn(FCMPL);
				method.node.visitJumpInsn(IFEQ, ifNonNull);
			}
			case DOUBLE -> {
				method.node.visitInsn(DUP2);
				method.node.visitInsn(DUP2);
				method.node.visitInsn(DCMPL);
				method.node.visitJumpInsn(IFEQ, ifNonNull);
			}
			case OBJECT, ARRAY -> {
				method.node.visitInsn(DUP);
				method.node.visitJumpInsn(IFNONNULL, ifNonNull);
			}
			case VOID -> {
				throw new InvalidOperandException("Attempt to check if void is non-null");
			}
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree value = this.value.cast(parser, type, mode);
		if (value == null) return null;
		InsnTree alternative = this.alternative.cast(parser, type, mode);
		if (alternative == null) return null;
		return new ElvisInsnTree(value, alternative, type);
	}
}