package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.util.TypeInfos;

public class ArrayStoreInsnTree implements InsnTree {

	public InsnTree array, index, value;

	public ArrayStoreInsnTree(InsnTree array, InsnTree index, InsnTree value) {
		this.array = array;
		this.index = index;
		this.value = value;
	}

	public static ArrayStoreInsnTree create(InsnTree array, InsnTree index, InsnTree value) {
		if (!array.getTypeInfo().isArray()) {
			throw new InvalidOperandException("Array must be array-typed.");
		}
		if (!index.getTypeInfo().isSingleWidthInt()) {
			throw new InvalidOperandException("Index must be single-width int.");
		}
		if (!value.getTypeInfo().extendsOrImplements(array.getTypeInfo().componentType)) {
			throw new InvalidOperandException("Can't store " + value.getTypeInfo() + " in " + array.getTypeInfo());
		}
		return new ArrayStoreInsnTree(array, index, value);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.array.emitBytecode(method);
		this.index.emitBytecode(method);
		this.value.emitBytecode(method);
		method.node.visitInsn(this.value.getTypeInfo().getOpcode(IASTORE));
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}