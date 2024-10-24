package builderb0y.scripting.bytecode;

import java.lang.StackWalker.Option;
import java.lang.reflect.Field;

import builderb0y.scripting.util.ReflectionData;

import static org.objectweb.asm.Opcodes.*;

public class FieldInfo implements Typeable {

	public static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

	public final int access;
	public final TypeInfo owner;
	public final String name;
	public final TypeInfo type;

	public FieldInfo(int access, TypeInfo owner, String name, TypeInfo type) {
		this.access = access;
		this.owner  = owner;
		this.name   = name;
		this.type   = type;
		if (owner.isPrimitive()) {
			throw new IllegalArgumentException("Primitive class cannot have fields: " + this);
		}
		if (owner.isArray()) {
			throw new IllegalArgumentException("Array class cannot have fields: " + this);
		}
		if (type.isVoid()) {
			throw new IllegalArgumentException("Cannot have a field of type void: " + this);
		}
	}

	public static FieldInfo inCaller(String name) {
		return getField(STACK_WALKER.getCallerClass(), name);
	}

	public static FieldInfo getField(Class<?> in, String name) {
		return forField(ReflectionData.forClass(in).getDeclaredField(name));
	}

	public static FieldInfo findField(Class<?> in, String name, Class<?> type) {
		return forField(ReflectionData.forClass(in).findDeclaredField(name, type));
	}

	public static FieldInfo forField(Field field) {
		return new FieldInfo(
			field.getModifiers(),
			TypeInfo.of(field.getDeclaringClass()),
			field.getName(),
			TypeInfo.of(field.getGenericType())
		);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	public boolean isStatic() {
		return (this.access & ACC_STATIC) != 0;
	}

	public boolean isFinal() {
		return (this.access & ACC_FINAL) != 0;
	}

	public FieldInfo makeFinal() {
		return this.isFinal() ? this : new FieldInfo(this.access | ACC_FINAL, this.owner, this.name, this.type);
	}

	public void emitGet(MethodCompileContext method) {
		method.node.visitFieldInsn(this.isStatic() ? GETSTATIC : GETFIELD, this.owner.getInternalName(), this.name, this.type.getDescriptor());
	}

	public void emitPut(MethodCompileContext method) {
		method.node.visitFieldInsn(this.isStatic() ? PUTSTATIC : PUTFIELD, this.owner.getInternalName(), this.name, this.type.getDescriptor());
	}

	@Override
	public String toString() {
		return this.owner.getInternalName() + '.' + this.name + " : " + this.type.getDescriptor();
	}

	@Override
	public int hashCode() {
		int hash = this.owner.hashCode();
		hash = hash * 31 + this.name.hashCode();
		hash = hash * 31 + this.type.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		return this == object || (
			object instanceof FieldInfo that &&
			this.access == that.access &&
			this.owner.equals(that.owner) &&
			this.name.equals(that.name) &&
			this.type.equals(that.type)
		);
	}
}