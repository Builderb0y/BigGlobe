package builderb0y.scripting.bytecode.tree.instructions.nullability;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.util.TypeMerger;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ElvisInsnTree implements InsnTree {

	public InsnTree value, alternative;
	public TypeInfo type;

	public ElvisInsnTree(InsnTree value, InsnTree alternative, TypeInfo type) {
		this.value = value;
		this.alternative = alternative;
		this.type = type;
	}

	public static InsnTree create(ExpressionParser parser, InsnTree value, InsnTree alternative) {
		TypeInfo commonType;
		if (value.jumpsUnconditionally()) {
			if (alternative.jumpsUnconditionally()) {
				commonType = TypeInfos.VOID;
			}
			else {
				commonType = alternative.getTypeInfo();
			}
		}
		else {
			if (alternative.jumpsUnconditionally()) {
				commonType = value.getTypeInfo();
			}
			else {
				commonType = TypeMerger.computeMostSpecificType(value.getTypeInfo(), alternative.getTypeInfo());
				value = value.cast(parser, commonType, CastMode.IMPLICIT_THROW);
				alternative = alternative.cast(parser, commonType, CastMode.IMPLICIT_THROW);
			}
		}
		return new ElvisInsnTree(value, alternative, commonType);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label end = label();
		this.value.emitBytecode(method);
		method.node.visitInsn(this.value.getTypeInfo().isDoubleWidth() ? DUP2 : DUP); //2 copies of the value.
		jumpIfNonNull(method, this.value.getTypeInfo(), end);
		method.node.visitInsn(this.value.getTypeInfo().isDoubleWidth() ? POP2 : POP); //value is still on stack, and guaranteed to be null. pop it.
		this.alternative.emitBytecode(method);
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
		return new ElvisInsnTree(this.value.cast(parser, type, mode), this.alternative.cast(parser, type, mode), type);
	}
}