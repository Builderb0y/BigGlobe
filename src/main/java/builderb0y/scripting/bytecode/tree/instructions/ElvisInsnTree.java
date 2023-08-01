package builderb0y.scripting.bytecode.tree.instructions;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ElvisInsnTree implements InsnTree {

	public InsnTree compileValue, compileAlternative, runtimeValue, runtimeAlternative;
	public TypeInfo type;

	public ElvisInsnTree(
		InsnTree compileValue,
		InsnTree compileAlternative,
		InsnTree runtimeValue,
		InsnTree runtimeAlternative,
		TypeInfo type
	) {
		this.compileValue       = compileValue;
		this.compileAlternative = compileAlternative;
		this.runtimeValue       = runtimeValue;
		this.runtimeAlternative = runtimeAlternative;
		this.type               = type;
	}

	public static InsnTree create(ExpressionParser parser, InsnTree value, InsnTree alternative) {
		IfElseInsnTree.Operands operands = IfElseInsnTree.Operands.of(parser, value, alternative);
		return new ElvisInsnTree(operands.compileTrue(), operands.compileFalse(), operands.runtimeTrue(), operands.runtimeFalse(), operands.type());
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label end = label();
		this.runtimeValue.emitBytecode(method);
		method.node.visitInsn(this.runtimeValue.getTypeInfo().isDoubleWidth() ? DUP2 : DUP); //2 copies of the value.
		jumpIfNonNull(method, this.runtimeValue.getTypeInfo(), end);
		method.node.visitInsn(this.runtimeValue.getTypeInfo().isDoubleWidth() ? POP2 : POP); //value is still on stack, and guaranteed to be null. pop it.
		this.runtimeAlternative.emitBytecode(method);
		method.node.visitLabel(end);
	}

	public static void jumpIfNonNull(MethodCompileContext method, TypeInfo type, Label ifNonNull) {
		switch (type.getSort()) {
			case OBJECT, ARRAY -> {
				method.node.visitJumpInsn(IFNONNULL, ifNonNull);
			}
			case FLOAT -> {
				method.node.visitInsn(DUP);
				method.node.visitInsn(FCMPL);
				method.node.visitJumpInsn(IFEQ, ifNonNull);
			}
			case DOUBLE -> {
				method.node.visitInsn(DUP2);
				method.node.visitInsn(DCMPL);
				method.node.visitJumpInsn(IFEQ, ifNonNull);
			}
			default -> {
				method.node.visitInsn(type.isDoubleWidth() ? POP2 : POP);
				method.node.visitJumpInsn(GOTO, ifNonNull);
			}
		}
	}

	public static void jumpIfNull(MethodCompileContext method, TypeInfo type, Label ifNull) {
		switch (type.getSort()) {
			case OBJECT, ARRAY -> {
				method.node.visitJumpInsn(IFNULL, ifNull);
			}
			case FLOAT -> {
				method.node.visitInsn(DUP);
				method.node.visitInsn(FCMPL);
				method.node.visitJumpInsn(IFNE, ifNull);
			}
			case DOUBLE -> {
				method.node.visitInsn(DUP2);
				method.node.visitInsn(DCMPL);
				method.node.visitJumpInsn(IFNE, ifNull);
			}
			default -> {}
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree value = this.compileValue.cast(parser, type, mode);
		if (value == null) return null;
		InsnTree alternative = this.compileAlternative.cast(parser, type, mode);
		if (alternative == null) return null;
		return new ElvisInsnTree(value, alternative, value, alternative, type);
	}
}