package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Double3DAccessSchema;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.MappedRangeNumberArray;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.Valids.Float3DValid;
import builderb0y.bigglobe.columns.scripted.Valids._3DValid;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class DoubleNoise3DColumnEntry extends Basic3DColumnEntry {

	public static final ColumnEntryMemory.Key<ConstantValue>
		CONSTANT_GRID = new ColumnEntryMemory.Key<>("constantGrid");

	public final Grid3D value;
	public final @VerifyNullable Float3DValid valid;
	public final @DefaultBoolean(true) boolean cache;

	public DoubleNoise3DColumnEntry(Grid3D value, @VerifyNullable Float3DValid valid, boolean cache) {
		this.value = value;
		this.valid = valid;
		this.cache = cache;
	}

	@Override
	public _3DValid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return Double3DAccessSchema.INSTANCE;
	}

	@Override
	public boolean hasField() {
		return this.cache;
	}

	@Override
	public void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		memory.putTyped(CONSTANT_GRID, context.mainClass.newConstant(this.value, type(Grid3D.class)));
		super.emitFieldGetterAndSetter(memory, context);
	}

	@Override
	public void populateComputeAll(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeAllMethod) {
		ConstantValue constantGrid = memory.getTyped(CONSTANT_GRID);
		computeAllMethod.setCode(
			"""
			grid.getBulkY(
				column.seed # salt,
				column.x,
				valueField.minCached,
				column.z,
				valueField.array.prefix(valueField.maxCached - valueField.minCached)
			)
			""",
			new MutableScriptEnvironment()
			.addVariableConstant("grid", constantGrid)
			.addMethodInvoke(Grid3D.class, "getBulkY")
			.addVariable("column", context.loadColumn())
			.addFieldGet(ScriptedColumn.class, "seed")
			.addVariableConstant("salt", Permuter.permute(0L, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID)))
			.addFieldGet(ScriptedColumn.class, "x")
			.addVariableRenamedGetField(context.loadSelf(), "valueField", memory.getTyped(ColumnEntryMemory.FIELD).info)
			.addFieldGet(MappedRangeNumberArray.MIN_CACHED)
			.addFieldGet(ScriptedColumn.class, "z")
			.addFieldGet(MappedRangeNumberArray.ARRAY)
			.addMethodInvoke(NumberArray.class, "prefix")
			.addFieldGet(MappedRangeNumberArray.MAX_CACHED)
		);
	}

	@Override
	public void populateComputeOne(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeOneMethod) throws ScriptParsingException {
		computeOneMethod.setCode(
			"return(grid.getValue(column.seed # salt, column.x, y, column.z))",
			new MutableScriptEnvironment()
			.addVariableConstant("grid", memory.getTyped(CONSTANT_GRID))
			.addMethodInvoke(Grid3D.class, "getValue")
			.addVariable("column", context.loadColumn())
			.addFieldGet(ScriptedColumn.class, "seed")
			.addVariableConstant("salt", Permuter.permute(0L, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID)))
			.addFieldGet(ScriptedColumn.class, "x")
			.addVariableLoad("y", TypeInfos.INT)
			.addFieldGet(ScriptedColumn.class, "z")
		);
	}
}