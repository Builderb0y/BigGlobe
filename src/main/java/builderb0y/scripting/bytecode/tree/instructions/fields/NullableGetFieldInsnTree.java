package builderb0y.scripting.bytecode.tree.instructions.fields;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.NullableInstanceFieldUpdateInsnTree.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

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
		//method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2 : DUP);
		//ElvisInsnTree.jumpIfNonNull(method, this.field.type, end);
		//method.node.visitInsn(this.field.type.isDoubleWidth() ? POP2 : POP);
		//constantAbsent(this.getTypeInfo()).emitBytecode(method);

		method.node.visitLabel(end);
	}

	@Override
	public InsnTree elvis(ExpressionParser parser, InsnTree alternative) throws ScriptParsingException {
		return ElvisGetFieldInsnTree.create(parser, this.object, this.field, alternative);
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (this.field.isFinal()) {
			throw new ScriptParsingException("Can't write to final field: " + this.field, parser.input);
		}
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.field.type, CastMode.IMPLICIT_THROW);
			return switch (order) {
				case VOID -> new NullableInstanceFieldAssignVoidUpdateInsnTree(this.object, this.field, cast);
				case PRE  -> new  NullableInstanceFieldAssignPreUpdateInsnTree(this.object, this.field, cast);
				case POST -> new NullableInstanceFieldAssignPostUpdateInsnTree(this.object, this.field, cast);
			};
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
			return switch (order) {
				case VOID -> new NullableInstanceFieldVoidUpdateInsnTree(this.object, this.field, updater);
				case PRE  -> new  NullableInstanceFieldPreUpdateInsnTree(this.object, this.field, updater);
				case POST -> new NullableInstanceFieldPostUpdateInsnTree(this.object, this.field, updater);
			};
		}
	}
}