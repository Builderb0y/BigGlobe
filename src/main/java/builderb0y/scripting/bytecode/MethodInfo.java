package builderb0y.scripting.bytecode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import org.objectweb.asm.Handle;

import builderb0y.scripting.util.ReflectionData;
import builderb0y.scripting.util.TypeInfos;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("deprecation")
public class MethodInfo {

	public static final int PURE = Integer.MIN_VALUE;

	@Deprecated //use getter method instead.
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
		return this.access & ~PURE;
	}

	public boolean isStatic() {
		return (this.access & ACC_STATIC) != 0;
	}

	public boolean isInterface() {
		return this.owner.type.isInterface;
	}

	public int getInvokeOpcode() {
		if (this.isStatic()) return INVOKESTATIC;
		if (this.isPrivate() || this.name.equals("<init>")) return INVOKESPECIAL;
		if (this.isInterface()) return INVOKEINTERFACE;
		return INVOKEVIRTUAL;
	}

	public boolean isPrivate() {
		return (this.access() & ACC_PRIVATE) != 0;
	}

	public boolean isPure() {
		return (this.access & PURE) != 0;
	}

	public static MethodInfo getMethod(Class<?> in, String name) {
		return forMethod(ReflectionData.forClass(in).getDeclaredMethod(name));
	}

	public static MethodInfo getMethod(Class<?> in, String name, Predicate<Method> predicate) {
		return forMethod(ReflectionData.forClass(in).findDeclaredMethod(name, predicate));
	}

	public static MethodInfo findMethod(Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return forMethod(ReflectionData.forClass(in).findDeclaredMethod(name, returnType, paramTypes));
	}

	public static MethodInfo forMethod(Method method) {
		return new MethodInfo(
			method.getModifiers(),
			TypeInfo.of(method.getDeclaringClass()),
			method.getName(),
			TypeInfo.of(method.getGenericReturnType()),
			TypeInfo.allOf(method.getGenericParameterTypes())
		);
	}

	public static MethodInfo getConstructor(Class<?> in) {
		return forConstructor(ReflectionData.forClass(in).getConstructor());
	}

	public static MethodInfo getConstructor(Class<?> in, Predicate<Constructor<?>> predicate) {
		return forConstructor(ReflectionData.forClass(in).findConstructor(predicate));
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
		return Modifier.toString(this.access()) + (this.isPure() ? " pure" : "") + ' ' + this.owner.getInternalName() + '.' + this.name + this.getDescriptor() + (this.isInterface() ? " (interface)" : "");
	}
}