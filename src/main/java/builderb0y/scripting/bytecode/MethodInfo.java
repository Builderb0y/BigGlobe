package builderb0y.scripting.bytecode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.Handle;

import builderb0y.scripting.util.ReflectionData;
import builderb0y.scripting.util.TypeInfos;

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

	public MethodInfo pure() {
		return this.isPure() ? this : new MethodInfo(this.access | PURE, this.owner, this.name, this.returnType, this.paramTypes);
	}

	public MethodInfo notPure() {
		return !this.isPure() ? this : new MethodInfo(this.access & ~PURE, this.owner, this.name, this.returnType, this.paramTypes);
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

	public static MethodInfo getMethod(Class<?> in, String name) {
		return forMethod(ReflectionData.forClass(in).getDeclaredMethod(name));
	}

	public static MethodInfo findMethod(Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return forMethod(ReflectionData.forClass(in).findDeclaredMethod(name, returnType, paramTypes));
	}

	public static MethodInfo forMethod(Method method) {
		int access = method.getModifiers();
		if (method.getDeclaringClass().isInterface()) access |= INTERFACE;
		return new MethodInfo(
			access,
			TypeInfo.of(method.getDeclaringClass()),
			method.getName(),
			TypeInfo.of(method.getGenericReturnType()),
			TypeInfo.allOf(method.getGenericParameterTypes())
		);
	}

	public static MethodInfo getConstructor(Class<?> in) {
		return forConstructor(ReflectionData.forClass(in).getConstructor());
	}

	public static MethodInfo findConstructor(Class<?> in, Class<?>... paramTypes) {
		return forConstructor(ReflectionData.forClass(in).findConstructor(paramTypes));
	}

	public static MethodInfo forConstructor(Constructor<?> constructor) {
		return new MethodInfo(
			constructor.getModifiers(),
			TypeInfo.of(constructor.getDeclaringClass()),
			"<init>",
			TypeInfos.VOID,
			TypeInfo.allOf(constructor.getGenericParameterTypes())
		);
	}

	@Override
	public String toString() {
		return Modifier.toString(this.access()) + ' ' + this.owner.getInternalName() + '.' + this.name + this.getDescriptor() + (this.isInterface() ? " (interface)" : "") + (this.isPure() ? " (pure)" : "");
	}
}