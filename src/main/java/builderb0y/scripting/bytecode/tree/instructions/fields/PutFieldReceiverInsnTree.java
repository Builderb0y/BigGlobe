package builderb0y.scripting.bytecode.tree.instructions.fields;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class PutFieldReceiverInsnTree implements InsnTree {

	public InsnTree object;
	public FieldInfo field;
	public InsnTree value;

	public PutFieldReceiverInsnTree(InsnTree object, FieldInfo field, InsnTree value) {
		PutFieldInsnTree.check(object, field, value);
		this.object = object;
		this.field = field;
		this.value = value;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.object.emitBytecode(method);
		method.node.visitInsn(DUP);
		this.value.emitBytecode(method);
		this.field.emitPut(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.object.getTypeInfo();
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}

	@Override
	public InsnTree asStatement() {
		return new PutFieldInsnTree(this.object, this.field, this.value);
	}
}