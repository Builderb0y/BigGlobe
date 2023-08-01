package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ReduceInsnTree implements InsnTree {

	public MethodInfo method;
	public InsnTree[] args;

	public ReduceInsnTree(MethodInfo method, InsnTree... args) {
		if (
			!method.isStatic() ||
			method.paramTypes.length != 2 ||
			!method.returnType.equals(method.paramTypes[0]) ||
			!method.returnType.equals(method.paramTypes[1])
		) {
			throw new IllegalArgumentException(method.toString());
		}
		if (args.length < 2) {
			throw new IllegalArgumentException("Reduction requires at least 2 arguments");
		}
		for (int index = 0, length = args.length; index < length; index++) {
			if (!args[index].getTypeInfo().extendsOrImplements(method.returnType)) {
				throw new IllegalArgumentException("Argument " + index + " is of the wrong type! Expected " + method.returnType + ", got " + args[index].describe());
			}
		}
		this.method = method;
		this.args = args;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		InsnTree[] args = this.args;
		args[0].emitBytecode(method);
		for (int index = 1, length = args.length; index < length; index++) {
			args[index].emitBytecode(method);
			this.method.emit(method);
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.method.returnType;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}