package builderb0y.scripting.bytecode.tree.instructions.invokers;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class InvokeBaseInsnTree implements InsnTree {

	public MethodInfo method;
	public InsnTree[] args;

	public InvokeBaseInsnTree(MethodInfo method, InsnTree... args) {
		this.method = method;
		this.args = args;
	}

	public static void checkArguments(TypeInfo[] requirements, InsnTree[] arguments) {
		int length = arguments.length;
		if (requirements.length != length) {
			throw new IllegalArgumentException("Wrong number of arguments: expected " + requirements.length + ", got " + arguments.length);
		}
		for (int index = 0; index < length; index++) {
			if (!arguments[index].getTypeInfo().extendsOrImplements(requirements[index])) {
				throw new IllegalArgumentException("Argument " + index + " is of the wrong type: expected " + requirements[index] + ", got " + arguments[index].getTypeInfo());
			}
		}
	}

	public int opcode() {
		return this.method.getInvokeOpcode();
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		for (InsnTree arg : this.args) {
			arg.emitBytecode(method);
		}
		this.method.emit(method, this.opcode());
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