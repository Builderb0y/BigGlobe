package builderb0y.scripting.bytecode.tree.instructions.invokers;

import com.google.common.collect.ObjectArrays;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class BaseInvokeInsnTree implements InsnTree {

	public MethodInfo method;
	public InsnTree[] args;

	public BaseInvokeInsnTree(MethodInfo method, InsnTree... args) {
		this.method = method;
		this.args = args;
	}

	public BaseInvokeInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		this.method = method;
		this.args = ObjectArrays.concat(receiver, args);
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

	public static void checkArguments(TypeInfo[] requirements, TypeInfo[] arguments) {
		int length = arguments.length;
		if (requirements.length != length) {
			throw new IllegalArgumentException("Wrong number of arguments: expected " + requirements.length + ", got " + arguments.length);
		}
		for (int index = 0; index < length; index++) {
			if (!arguments[index].extendsOrImplements(requirements[index])) {
				throw new IllegalArgumentException("Argument " + index + " is of the wrong type: expected " + requirements[index] + ", got " + arguments[index]);
			}
		}
	}

	public static void checkGetterSetter(MethodInfo getter, MethodInfo setter) {
		if (getter.getInvokeTypes().length != 1) {
			throw new IllegalArgumentException("Getter should take exactly 1 argument: " + getter);
		}
		if (getter.returnType.isVoid()) {
			throw new IllegalArgumentException("Getter should not return void: " + getter);
		}
		if (setter.getInvokeTypes().length != 2) {
			throw new IllegalArgumentException("Setter should take exactly 2 arguments: " + setter);
		}
		if (setter.returnType.isValue()) {
			throw new IllegalArgumentException("Setter should return void: " + setter);
		}
		if (!getter.getInvokeTypes()[0].equals(setter.getInvokeTypes()[0])) {
			throw new IllegalArgumentException("Getter and setter operate on different types: " + getter + "; " + setter);
		}
		if (!getter.returnType.equals(setter.getInvokeTypes()[1])) {
			throw new IllegalArgumentException("Getter return type does not match setter value type: " + getter + "; " + setter);
		}
	}

	public static void checkGetterSetter(InsnTree receiver, MethodInfo getter, MethodInfo setter) {
		checkGetterSetter(getter, setter);
		if (!receiver.getTypeInfo().extendsOrImplements(getter.getInvokeTypes()[0])) {
			throw new IllegalArgumentException("Receiver is of the wrong type: expected " + getter + ", got " + receiver.describe());
		}
	}

	public int opcode() {
		return this.method.getInvokeOpcode();
	}

	public void emitAllArgs(MethodCompileContext method) {
		for (InsnTree arg : this.args) {
			arg.emitBytecode(method);
		}
	}

	public void emitFirstArg(MethodCompileContext method) {
		this.args[0].emitBytecode(method);
	}

	public void emitAllArgsExceptFirst(MethodCompileContext method) {
		InsnTree[] args = this.args;
		int length = args.length;
		for (int index = 1; index < length; index++) {
			args[index].emitBytecode(method);
		}
	}

	public void emitMethod(MethodCompileContext method) {
		this.method.emit(method, this.opcode());
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.emitAllArgs(method);
		this.emitMethod(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.method.returnType;
	}

	@Override
	public boolean canBeStatement() {
		return !this.method.isPure();
	}
}