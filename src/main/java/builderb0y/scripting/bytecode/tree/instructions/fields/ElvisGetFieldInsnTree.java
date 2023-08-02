package builderb0y.scripting.bytecode.tree.instructions.fields;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ElvisInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ElvisGetFieldInsnTree implements InsnTree {

	public InsnTree object;
	public FieldInfo field;
	public InsnTree runtimeFieldCaster;
	public InsnTree compileAlternative, runtimeAlternative;

	public ElvisGetFieldInsnTree(
		InsnTree object,
		FieldInfo field,
		InsnTree runtimeFieldCaster,
		InsnTree compileAlternative,
		InsnTree runtimeAlternative
	) {
		GetFieldInsnTree.check(object, field);
		if (!runtimeFieldCaster.getTypeInfo().equals(runtimeAlternative.getTypeInfo())) {
			throw new IllegalArgumentException("Type mismatch between runtimeFieldCaster (" + runtimeFieldCaster.getTypeInfo() + ") and runtimeAlternative (" + runtimeAlternative.getTypeInfo() + ')');
		}
		this.object = object;
		this.field = field;
		this.runtimeFieldCaster = runtimeFieldCaster;
		this.compileAlternative = compileAlternative;
		this.runtimeAlternative = runtimeAlternative;
	}

	public static ElvisGetFieldInsnTree create(ExpressionParser parser, InsnTree object, FieldInfo field, InsnTree alternative) {
		IfElseInsnTree.Operands operands = IfElseInsnTree.Operands.of(parser, getFromStack(field.type), alternative);
		return new ElvisGetFieldInsnTree(object, field, operands.runtimeTrue(), operands.compileFalse(), operands.runtimeFalse());
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label get = label(), alternative = label(), end = label();

		this.object.emitBytecode(method);
		method.node.visitInsn(DUP);
		method.node.visitJumpInsn(IFNONNULL, get);
		method.node.visitInsn(POP);
		method.node.visitJumpInsn(GOTO, alternative);

		method.node.visitLabel(get);
		this.field.emitGet(method);
		this.runtimeFieldCaster.emitBytecode(method);
		method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2 : DUP);
		ElvisInsnTree.jumpIfNonNull(method, this.field.type, end);
		method.node.visitInsn(this.field.type.isDoubleWidth() ? POP2 : POP);

		method.node.visitLabel(alternative);
		this.runtimeAlternative.emitBytecode(method);

		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.runtimeAlternative.getTypeInfo();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree fieldCaster = getFromStack(this.field.type).cast(parser, type, mode);
		if (fieldCaster == null) return null;
		InsnTree alternative = this.compileAlternative.cast(parser, type, mode);
		if (alternative == null) return null;
		return new ElvisGetFieldInsnTree(this.object, this.field, fieldCaster, alternative, alternative);
	}
}