package builderb0y.scripting.bytecode.tree.instructions.nullability;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullMapperInsnTree implements InsnTree {

	public InsnTree value;
	public InsnTree mapper; //should get from stack.

	public NullMapperInsnTree(InsnTree value, InsnTree mapper) {
		this.value = value;
		this.mapper = mapper;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label get = label(), end = label();

		this.value.emitBytecode(method);
		method.node.visitInsn(this.value.getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
		ElvisInsnTree.jumpIfNonNull(method, this.value.getTypeInfo(), get);
		method.node.visitInsn(this.value.getTypeInfo().isDoubleWidth() ? POP2 : POP);
		if (this.getTypeInfo().isValue()) {
			constantAbsent(this.getTypeInfo()).emitBytecode(method);
		}
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(get);
		this.mapper.emitBytecode(method);

		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.mapper.getTypeInfo();
	}
}