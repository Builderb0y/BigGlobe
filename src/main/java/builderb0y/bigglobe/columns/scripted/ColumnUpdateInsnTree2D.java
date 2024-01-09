package builderb0y.bigglobe.columns.scripted;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree;

public class ColumnUpdateInsnTree2D extends AbstractUpdaterInsnTree {

	public InsnTree accessor, column, updater;
	public TypeInfo type;
	public MethodInfo getter, setter;

	public ColumnUpdateInsnTree2D(
		boolean assignment,
		InsnTree accessor,
		InsnTree column,
		InsnTree updater,
		TypeInfo type,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(assignment ? CombinedMode.VOID_ASSIGN : CombinedMode.VOID);
		this.accessor = accessor;
		this.column   = column;
		this.updater  = updater;
		this.type     = type;
		this.getter   = getter;
		this.setter   = setter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		if (this.mode.isAssignment) {
			this.accessor.emitBytecode(method);
			this.column.emitBytecode(method);
			this.updater.emitBytecode(method);
		}
		else {
			this.accessor.emitBytecode(method);
			this.column.emitBytecode(method);
			method.node.visitInsn(DUP2);
			this.getter.emitBytecode(method);
			this.updater.emitBytecode(method);
			this.setter.emitBytecode(method);
		}
	}

	@Override
	public InsnTree asStatement() {
		return this;
	}

	@Override
	public TypeInfo getPreType() {
		return this.type;
	}

	@Override
	public TypeInfo getPostType() {
		return this.type;
	}
}