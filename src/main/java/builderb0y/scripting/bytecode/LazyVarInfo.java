package builderb0y.scripting.bytecode;

import builderb0y.autocodec.util.ObjectArrayFactory;

import static org.objectweb.asm.Opcodes.*;

public class LazyVarInfo implements Typeable {

	public static final ObjectArrayFactory<LazyVarInfo> ARRAY_FACTORY = new ObjectArrayFactory<>(LazyVarInfo.class);

	public final String name;
	public final TypeInfo type;

	public LazyVarInfo(String name, TypeInfo type) {
		this.name = name;
		this.type = type;
		if (type.isVoid()) {
			throw new IllegalArgumentException("Void-typed variable: " + this);
		}
	}

	public String name() {
		return this.name;
	}

	public TypeInfo type() {
		return this.type;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	public void emitLoad(MethodCompileContext context) {
		int index = context.scopes.getVariableIndex(this);
		context.node.visitVarInsn(this.type.getOpcode(ILOAD), index);
	}

	public void emitStore(MethodCompileContext context) {
		int index = context.scopes.getVariableIndex(this);
		context.node.visitVarInsn(this.type.getOpcode(ISTORE), index);
	}

	@Override
	public String toString() {
		return this.name + " : " + this.type;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + this.type.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return this == object || (
			object instanceof LazyVarInfo that &&
			this.name.equals(that.name) &&
			this.type.equals(that.type)
		);
	}
}