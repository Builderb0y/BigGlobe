package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Float2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.Valids.Float2DValid;
import builderb0y.bigglobe.columns.scripted.Valids._2DValid;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.BitwiseXorInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class FloatNoise2DColumnEntry extends Basic2DColumnEntry {

	public final Grid2D value;
	public final Float2DValid valid;
	public final boolean cache;

	public FloatNoise2DColumnEntry(Grid2D value, Float2DValid valid, boolean cache) {
		this.value = value;
		this.valid = valid;
		this.cache = cache;
	}

	@Override
	public _2DValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Float2DAccessSchema.INSTANCE;
	}

	@Override
	public boolean hasField() {
		return this.cache;
	}

	@Override
	public void populateCompute(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		ConstantValue gridConstant = context.mainClass.newConstant(this.value, type(Grid2D.class));
		computeMethod.scopes.withScope((MethodCompileContext computer) -> {
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