package builderb0y.scripting.bytecode;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import builderb0y.autocodec.util.ObjectArrayFactory;

public class VarInfo {

	public static final ObjectArrayFactory<VarInfo> ARRAY_FACTORY = new ObjectArrayFactory<>(VarInfo.class);

	//note: index in particular must be non-final
	//for VariableDeclarationInsnTree to assign it.
	public String name;
	public int index;
	public TypeInfo type;

	public VarInfo(String name, int index, TypeInfo type) {
		this.name  = name;
		this.index = index;
		this.type  = type;
		if (type.isVoid()) {
			throw new IllegalArgumentException("Void-typed variable: " + this);
		}
	}

	@Override
	public String toString() {
		return this.name + " : " + this.type + " @ " + this.index;
	}

	public void emitLoad(MethodVisitor visitor) {
		visitor.visitVarInsn(this.type.getOpcode(Opcodes.ILOAD), this.index);
	}

	public void emitStore(MethodVisitor visitor) {
		visitor.visitVarInsn(this.type.getOpcode(Opcodes.ISTORE), this.index);
	}

	@Override
	public int hashCode() {
		int hash = this.name.hashCode();
		hash = hash * 31 + this.index;
		hash = hash * 31 + this.type.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		return this == object || (
			object instanceof VarInfo that &&
			this.name.equals(that.name) &&
			this.index == that.index &&
			this.type.equals(that.type)
		);
	}
}