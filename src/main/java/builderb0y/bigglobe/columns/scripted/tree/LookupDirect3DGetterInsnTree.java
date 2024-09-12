package builderb0y.bigglobe.columns.scripted.tree;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree.CombinedMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

//`example_mod:example_value`(x, y, z)
public class LookupDirect3DGetterInsnTree implements InsnTree {

	public final InsnTree lookup, x, y, z;
	public final MethodInfo getter;
	public final @Nullable MethodInfo setter;

	public LookupDirect3DGetterInsnTree(
		InsnTree lookup,
		InsnTree x,
		InsnTree y,
		InsnTree z,
		MethodInfo getter,
		@Nullable MethodInfo setter
	) {
		this.lookup = lookup;
		this.x = x;
		this.y = y;
		this.z = z;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.lookup.emitBytecode(method); //lookup
		this.x.emitBytecode(method); //lookup x
		this.y.emitBytecode(method); //lookup x y
		method.node.visitInsn(DUP_X2); //y lookup x y
		method.node.visitInsn(POP); //y lookup x
		this.z.emitBytecode(method); //y lookup x z
		ScriptedColumnLookup.LOOKUP_COLUMN.emitBytecode(method); //y column
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName()); //y column
		method.node.visitInsn(SWAP); //column y
		this.getter.emitBytecode(method); //value
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.getter.returnType;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (this.setter != null) {
			if (op == UpdateOp.ASSIGN) {
				InsnTree cast = rightValue.cast(parser, this.getter.returnType, CastMode.IMPLICIT_THROW);
				return new LookupDirect3DSetterInsnTree(CombinedMode.of(order, true), this.lookup, this.x, this.y, this.z, cast, this.getter, this.setter);
			}
			else {
				InsnTree updater = op.createUpdater(parser, this.getter.returnType, rightValue);
				return new LookupDirect3DSetterInsnTree(CombinedMode.of(order, false), this.lookup, this.x, this.y, this.z, updater, this.getter, this.setter);
			}
		}
		return InsnTree.super.update(parser, op, order, rightValue);
	}
}