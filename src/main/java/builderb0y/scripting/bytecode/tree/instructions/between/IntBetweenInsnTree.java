package builderb0y.scripting.bytecode.tree.instructions.between;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class IntBetweenInsnTree extends BetweenInsnTree {

	public IntBetweenInsnTree(
		InsnTree value,
		InsnTree min,
		boolean minInclusive,
		InsnTree max,
		boolean maxInclusive
	) {
		super(value, min, minInclusive, max, maxInclusive);
	}

	@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
	public static InsnTree create(
		ExpressionParser parser,
		InsnTree value,
		InsnTree min,
		boolean minInclusive,
		InsnTree max,
		boolean maxInclusive
	) {
		return new IntBetweenInsnTree(
			value.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW),
			min.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW),
			minInclusive,
			max.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW),
			maxInclusive
		);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label outOfBounds1 = label(), outOfBounds0 = label(), end = label();
		this.value.emitBytecode(method);
		method.node.visitInsn(DUP);
		method.scopes.pushScope();
		if (this.min.getConstantValue().isConstant() && this.min.getConstantValue().asInt() == 0) {
			method.node.visitJumpInsn(this.minInclusive ? IFLT : IFLE, outOfBounds1);
		}
		else {
			this.min.emitBytecode(method);
			method.node.visitJumpInsn(this.minInclusive ? IF_ICMPLT : IF_ICMPLE, outOfBounds1);
		}
		if (this.max.getConstantValue().isConstant() && this.max.getConstantValue().asInt() == 0) {
			method.node.visitJumpInsn(this.maxInclusive ? IFGT : IFGE, outOfBounds0);
		}
		else {
			this.max.emitBytecode(method);
			method.node.visitJumpInsn(this.maxInclusive ? IF_ICMPGT : IF_ICMPGE, outOfBounds0);
		}
		method.scopes.popScope();
		method.node.visitInsn(ICONST_1);
		method.node.visitJumpInsn(GOTO, end);
		method.node.visitLabel(outOfBounds1);
		method.node.visitInsn(POP);
		method.node.visitLabel(outOfBounds0);
		method.node.visitInsn(ICONST_0);
		method.node.visitLabel(end);
	}
}