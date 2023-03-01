package builderb0y.scripting.bytecode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.Handle;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.util.UncheckedReflection;

import static org.objectweb.asm.Opcodes.*;

public class MethodInfo {

	public static final int
		INTERFACE = ACC_INTERFACE,
		PURE      = Integer.MIN_VALUE;

	public int access;
	public TypeInfo owner;
	public String name;
	public TypeInfo returnType;
	public TypeInfo[] paramTypes;

	public MethodInfo(int access, TypeInfo owner, String name, TypeInfo returnType, TypeInfo... paramTypes) {
		this.access = access;
		this.owner = owner;
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		for (TypeInfo paramType : paramTypes) {
			if (paramType.isVoid()) {
				throw new IllegalArgumentException("Void-type parameter: " + this);
			}
		}
	}

	public String getDescriptor() {
		StringBuilder builder = new StringBuilder(128).append('(');
		for (TypeInfo paramType : this.paramTypes) {
			builder.append(paramType.getDescriptor());
		}
		return builder.append(')').append(this.returnType.getDescriptor()).toString();
	}

	public Handle toHandle(int handleType) {
		return new Handle(handleType, this.owner.getInternalName(), this.name, this.getDescriptor(), this.isInterface());
	}

	public void emit(MethodCompileContext method, int opcode) {
		method.node.visitMethodInsn(opcode, this.owner.getInternalName(), this.name, this.getDescriptor(), this.isInterface());
	}

	public int access() {
		return this.access & ~(INTERFACE | PURE);
	}

	public boolean isStatic() {
		return (this.access & ACC_STATIC) != 0;
	}

	public boolean isInterface() {
		return (this.access & INTERFACE) != 0;
	}

	public boolean isPure() {
		return (this.access & PURE) != 0;
	}

	public static MethodInfo findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
		return forMethod(UncheckedReflection.getDeclaredMethod(owner, name, parameterTypes));
	}

	public static MethodInfo findFirstMethod(Class<?> owner, String name) {
		name = name.intern();
		for (Method method : owner.getDeclaredMethods()) {
			if (method.getName() == name) return forMethod(method);
		}
		throw AutoCodecUtil.rethrow(new NoSuchMethodException(name + " in " + owner));
	}

	public static MethodInfo forMethod(Method method) {
		int access = method.getModifiers();
		if (method.getDeclaringClass().isInterface()) access |= INTERFACE;
		return new MethodInfo(
			access,
			TypeInfo.of(method.getDeclaringClass()),
			method.getName(),
			TypeInfo.of(method.getReturnType()),
			TypeInfo.allOf(method.getParameterTypes())
		);
	}

	public static MethodInfo findConstructor(Class<?> owner, Class<?>... parameterTypes) {
		return forConstructor(UncheckedReflection.getDeclaredConstructor(owner, parameterTypes));
	}

	public static MethodInfo forConstructor(Constructor<?> constructor) {
		return new MethodInfo(
			constructor.getModifiers(),
			TypeInfo.of(constructor.getDeclaringClass()),
			"<init>",
			TypeInfos.VOID,
			TypeInfo.allOf(constructor.getParameterTypes())
		);
	}

	@Override
	public String toString() {
		return Modifier.toString(this.access()) + ' ' + this.owner.getInternalName() + '.' + this.name + this.getDescriptor() + (this.isInterface() ? " (interface)" : "") + (this.isPure() ? " (pure)" : "");
	}
}