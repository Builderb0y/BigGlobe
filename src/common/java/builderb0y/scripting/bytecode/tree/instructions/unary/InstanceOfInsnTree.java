package builderb0y.scripting.bytecode.tree.instructions.unary;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class InstanceOfInsnTree extends UnaryInsnTree {

	public final TypeInfo type;

	public InstanceOfInsnTree(InsnTree value, TypeInfo type) {
		super(value);
		this.type = type;
	}

	public static InsnTree create(InsnTree value, TypeInfo type) {
		if (value.getTypeInfo().isObject()) {
			return new InstanceOfInsnTree(value, type);
		}
		else {
			throw new IllegalArgumentException(value.getTypeInfo() + " instanceof " + type);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.operand.emitBytecode(method);
		method.node.visitTypeInsn(INSTANCEOF, this.type.getInternalName());
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.BOOLEAN;
	}
}