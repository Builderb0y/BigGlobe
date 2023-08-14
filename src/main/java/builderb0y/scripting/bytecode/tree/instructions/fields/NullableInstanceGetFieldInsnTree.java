package builderb0y.scripting.bytecode.tree.instructions.fields;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree.ElvisEmitters;
import builderb0y.scripting.bytecode.tree.instructions.update2.AbstractObjectUpdaterInsnTree.ObjectUpdaterEmitters;
import builderb0y.scripting.bytecode.tree.instructions.update2.NullableObjectUpdaterInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableInstanceGetFieldInsnTree extends AbstractInstanceGetFieldInsnTree {

	public NullableInstanceGetFieldInsnTree(InsnTree object, FieldInfo field) {
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

		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.field.type;
	}

	@Override
	public InsnTree constructUpdater(UpdateOrder order, boolean isAssignment, ObjectUpdaterEmitters emitters) {
		return new NullableObjectUpdaterInsnTree(order, isAssignment, emitters);
	}

	@Override
	public InsnTree elvis(ExpressionParser parser, InsnTree alternative) throws ScriptParsingException {
		return new ElvisGetInsnTree(ElvisEmitters.forField(this.object, this.field, alternative));
	}
}