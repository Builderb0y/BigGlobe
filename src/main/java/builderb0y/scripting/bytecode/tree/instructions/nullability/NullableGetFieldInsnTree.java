package builderb0y.scripting.bytecode.tree.instructions.nullability;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.GetFieldInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableGetFieldInsnTree extends GetFieldInsnTree {

	public NullableGetFieldInsnTree(InsnTree object, FieldInfo field) {
		super(object, field);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label get = label(), end = label();

		this.object.emitBytecode(method);
		method.node.visitInsn(DUP);
		method.node.visitJumpInsn(IFNONNULL, get);
		method.node.visitInsn(POP);
		constantAbsent(this.getTypeInfo()).emitBytecode(method);
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(get);
		this.field.emitGet(method);
		method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2 : DUP);
		ElvisInsnTree.jumpIfNonNull(method, this.field.type, end);
		method.node.visitInsn(this.field.type.isDoubleWidth() ? POP2 : POP);
		constantAbsent(this.getTypeInfo()).emitBytecode(method);

		method.node.visitLabel(end);
	}
}