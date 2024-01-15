package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.BitwiseXorInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class FloatNoise2DColumnEntry extends Basic2DColumnEntry {

	public final Grid2D value;
	public final boolean cache;

	public FloatNoise2DColumnEntry(Grid2D value, boolean cache) {
		this.value = value;
		this.cache = cache;
	}

	@Override
	public boolean hasField() {
		return this.cache;
	}

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		MethodCompileContext computerMethod = memory.getTyped(ColumnEntryMemory.COMPUTER);
		ConstantValue gridConstant = context.mainClass.newConstant(this.value, type(Grid2D.class));
		computerMethod.scopes.withScope((MethodCompileContext computer) -> {
			computer.addThis();
			InsnTree x = getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "x"));
			InsnTree z = getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "z"));
			long salt = Permuter.permute(0L, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));
			InsnTree originalSeed = getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "seed"));
			InsnTree saltedSeed = new BitwiseXorInsnTree(originalSeed, ldc(salt), LXOR);
			return_(CastingSupport.primitiveCast(invokeInstance(ldc(gridConstant), MethodInfo.getMethod(Grid2D.class, "getValue"), saltedSeed, x, z), TypeInfos.FLOAT)).emitBytecode(computer);
		});
	}
}