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

//`example_mod:example_value`(x, z)
public class LookupDirect2DGetterInsnTree implements InsnTree {

	public final InsnTree lookup, x, z;
	public final MethodInfo getter;
	public final @Nullable MethodInfo setter;

	public LookupDirect2DGetterInsnTree(
		InsnTree lookup,
		InsnTree x,
		InsnTree z,
		MethodInfo getter,
		@Nullable MethodInfo setter
	) {
		this.lookup = lookup;
		this.x = x;
		this.z = z;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.lookup.emitBytecode(method);
		this.x.emitBytecode(method);
		this.z.emitBytecode(method);
		ScriptedColumnLookup.LOOKUP_COLUMN.emitBytecode(method);
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName());
		this.getter.emitBytecode(method);
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
				return new LookupDirect2DSetterInsnTree(CombinedMode.of(order, true), this.lookup, this.x, this.z, cast, this.getter, this.setter);
			}
			else {
				InsnTree updater = op.createUpdater(parser, this.getter.returnType, rightValue);
				return new LookupDirect2DSetterInsnTree(CombinedMode.of(order, false), this.lookup, this.x, this.z, updater, this.getter, this.setter);
			}
		}
		return InsnTree.super.update(parser, op, order, rightValue);
	}
}