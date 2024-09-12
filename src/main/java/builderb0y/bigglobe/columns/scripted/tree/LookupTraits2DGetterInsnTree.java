package builderb0y.bigglobe.columns.scripted.tree;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree.CombinedMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

//world_traits.`example_mod:example_value`(x, z)
public class LookupTraits2DGetterInsnTree implements InsnTree {

	public final InsnTree lookup, x, z;
	public final MethodInfo getter;
	public final @Nullable MethodInfo setter;

	public LookupTraits2DGetterInsnTree(
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
		this.lookup.emitBytecode(method); //lookup
		this.x.emitBytecode(method); //lookup x
		this.z.emitBytecode(method); //lookup x z
		ScriptedColumnLookup.LOOKUP_COLUMN.emitBytecode(method); //column
		method.node.visitTypeInsn(CHECKCAST, this.getter.paramTypes[0].getInternalName()); //column
		method.node.visitInsn(DUP); //column column
		ScriptedColumn.INFO.worldTraits.emitBytecode(method); //column traits
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName()); //column traits
		method.node.visitInsn(SWAP);
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
				return new LookupTraits2DSetterInsnTree(CombinedMode.of(order, true), this.lookup, this.x, this.z, cast, this.getter, this.setter);
			}
			else {
				InsnTree updater = op.createUpdater(parser, this.getter.returnType, rightValue);
				return new LookupTraits2DSetterInsnTree(CombinedMode.of(order, false), this.lookup, this.x, this.z, updater, this.getter, this.setter);
			}
		}
		return InsnTree.super.update(parser, op, order, rightValue);
	}
}