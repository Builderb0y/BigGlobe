package builderb0y.scripting.bytecode.tree.instructions.between;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class LongBetweenInsnTree extends BetweenInsnTree {

	public LongBetweenInsnTree(
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
		return new LongBetweenInsnTree(
			value.cast(parser, TypeInfos.LONG, CastMode.IMPLICIT_THROW),
			min.cast(parser, TypeInfos.LONG, CastMode.IMPLICIT_THROW),
			minInclusive,
			max.cast(parser, TypeInfos.LONG, CastMode.IMPLICIT_THROW),
			maxInclusive
		);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label outOfBounds1 = label(), outOfBounds0 = label(), end = label();
		this.value.emitBytecode(method);
		method.node.visitInsn(DUP2);
		method.scopes.pushScope();
		this.min.emitBytecode(method);
		method.node.visitInsn(LCMP);
		method.node.visitJumpInsn(this.minInclusive ? IFLT : IFLE, outOfBounds1);
		this.max.emitBytecode(method);
		method.node.visitInsn(LCMP);
		method.node.visitJumpInsn(this.maxInclusive ? IFGT : IFGE, outOfBounds0);
		method.scopes.popScope();
		method.node.visitInsn(ICONST_1);
		method.node.visitJumpInsn(GOTO, end);
		method.node.visitLabel(outOfBounds1);
		method.node.visitInsn(POP2);
		method.node.visitLabel(outOfBounds0);
		method.node.visitInsn(ICONST_0);
		method.node.visitLabel(end);
	}
}