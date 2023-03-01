package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class StaticGetterSetterUpdateInsnTree extends UpdateInsnTree {

	public MethodInfo getter, setter;

	public StaticGetterSetterUpdateInsnTree(
		MethodInfo getter,
		MethodInfo setter,
		InsnTree updater
	) {
		super(updater);
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.getter.emit(method, INVOKESTATIC);
		this.updater.emitBytecode(method);
		this.setter.emit(method, INVOKESTATIC);
	}
}