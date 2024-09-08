package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ColumnGet3DValueWithTraitsInsnTree implements InsnTree {

	public InsnTree loadColumn, y;
	public MethodInfo getter;

	public ColumnGet3DValueWithTraitsInsnTree(
		InsnTree loadColumn,
		InsnTree y,
		MethodInfo getter
	) {
		this.loadColumn = loadColumn;
		this.y = y;
		this.getter = getter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.loadColumn.emitBytecode(method); //column
		if (!this.loadColumn.getTypeInfo().equals(this.getter.paramTypes[0])) {
			method.node.visitTypeInsn(CHECKCAST, this.getter.paramTypes[0].getInternalName());
		}
		method.node.visitInsn(DUP); //column column
		ScriptedColumn.INFO.worldTraits.emitBytecode(method); //column traits
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName());
		method.node.visitInsn(SWAP); //traits column
		this.y.emitBytecode(method); //traits column y
		this.getter.emitBytecode(method); //value
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.getter.returnType;
	}
}