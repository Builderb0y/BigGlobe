package builderb0y.scripting.bytecode;

import builderb0y.autocodec.util.ObjectArrayFactory;

import static org.objectweb.asm.Opcodes.*;

public record LazyVarInfo(String name, TypeInfo type) implements Typeable {

	public static final ObjectArrayFactory<LazyVarInfo> ARRAY_FACTORY = new ObjectArrayFactory<>(LazyVarInfo.class);

	public LazyVarInfo(String name, TypeInfo type) {
		this.name = name;
		this.type = type;
		if (type.isVoid()) {
			throw new IllegalArgumentException("Void-typed variable: " + this);
		}
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
}