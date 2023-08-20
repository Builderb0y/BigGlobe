package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.BaseInvokeInsnTree;

public abstract class ListMapUpdaterInsnTree extends AbstractUpdaterInsnTree {

	public InsnTree receiver, key, value;
	public MethodInfo replacer;

	public ListMapUpdaterInsnTree(CombinedMode mode, InsnTree receiver, InsnTree key, InsnTree value, MethodInfo replacer) {
		super(mode);
		this.receiver = receiver;
		this.key = key;
		this.value = value;
		this.replacer = replacer;
		BaseInvokeInsnTree.checkArguments(this.replacer.getInvokeTypes(), new InsnTree[] { receiver, key, value });
	}

	public ListMapUpdaterInsnTree(UpdateOrder order, boolean isAssignment, InsnTree receiver, InsnTree key, InsnTree value, MethodInfo replacer) {
		super(order, isAssignment);
		this.receiver = receiver;
		this.key = key;
		this.value = value;
		this.replacer = replacer;
		BaseInvokeInsnTree.checkArguments(this.replacer.getInvokeTypes(), new InsnTree[] { receiver, key, value });
	}

	@Override
	public TypeInfo getPreType() {
		return this.replacer.returnType;
	}

	@Override
	public TypeInfo getPostType() {
		return this.value.getTypeInfo();
	}
}