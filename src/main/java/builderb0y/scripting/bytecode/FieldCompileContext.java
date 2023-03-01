package builderb0y.scripting.bytecode;

import org.objectweb.asm.tree.FieldNode;

import builderb0y.scripting.bytecode.tree.ConstantValue;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class FieldCompileContext {

	public ClassCompileContext clazz;
	public FieldInfo info;
	public FieldNode node;
	public ConstantValue initializer;

	public FieldCompileContext(ClassCompileContext clazz, int access, String name, TypeInfo type) {
		this.clazz = clazz;
		this.info = field(access, clazz.info, name, type);
		this.node = new FieldNode(access, name, type.getDescriptor(), null, null);
	}

	public String name() {
		return this.node.name;
	}
}