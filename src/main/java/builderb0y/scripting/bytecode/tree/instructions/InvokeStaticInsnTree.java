package builderb0y.scripting.bytecode.tree.instructions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.util.ArrayFactories;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.Typeable;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class InvokeStaticInsnTree implements InsnTree {

	public MethodInfo method;
	public InsnTree[] args;

	public InvokeStaticInsnTree(MethodInfo method, InsnTree... args) {
		this.args = args;
		this.method = method;
	}

	public static InsnTree create(MethodInfo method, InsnTree... args) {
		if (!method.isStatic()) {
			throw new IllegalArgumentException("Non-static method: " + method);
		}
		checkTypes(args, method.paramTypes);
		notPure:
		if (method.isPure() && isPrimitive(method.returnType)) {
			Object[] constantArgs = getConstantArgs(args);
			if (constantArgs == null) break notPure;
			try {
				MethodHandle handle = getMethodHandle(method);
				return ldc(
					handle.invokeWithArguments(constantArgs),
					method.returnType
				);
			}
			catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		}
		return new InvokeStaticInsnTree(method, args);
	}

	public static boolean isPrimitive(TypeInfo type) {
		return type.isPrimitiveValue() || type.equals(TypeInfos.STRING);
	}

	public static Object @Nullable [] getConstantArgs(InsnTree... args) {
		int argCount = args.length;
		Object[] constantArgs = new Object[argCount];
		for (int index = 0; index < argCount; index++) {
			ConstantValue constant = args[index].getConstantValue();
			if (constant.isConstant()) {
				constantArgs[index] = constant.asJavaObject();
			}
			else {
				return null;
			}
		}
		return constantArgs;
	}

	public static MethodHandle getMethodHandle(MethodInfo method) throws NoSuchMethodException, IllegalAccessException {
		Class<?> owner = method.owner.toClass();
		MethodType methodType = getMethodType(method);
		return MethodHandles.lookup().findStatic(owner, method.name, methodType);
	}

	public static MethodType getMethodType(MethodInfo method) {
		return MethodType.methodType(
			method.returnType.toClass(),
			Arrays.stream(method.paramTypes)
			.map(TypeInfo::toClass)
			.toArray(ArrayFactories.CLASS)
		);
	}

	public static void checkTypes(Typeable[] arguments, TypeInfo[] requirements) {
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