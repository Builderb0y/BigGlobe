package builderb0y.bigglobe.columns.scripted.entries;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.columns.scripted.MappedRangeNumberArray;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.Valid;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.types.DoubleColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.FloatColumnValueType;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.BitwiseXorInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.OpcodeCastInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@UseCoder(name = "new", in = NoiseColumnEntry.Coder.class, usage = MemberUsage.METHOD_IS_FACTORY)
public class NoiseColumnEntry extends AbstractColumnEntry {

	public static final ColumnEntryMemory.Key<ConstantValue>
		CONSTANT_GRID = new ColumnEntryMemory.Key<>("constantGrid");

	public final Grid2D grid2D;
	public final Grid3D grid3D;

	public NoiseColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		@DefaultBoolean(true) boolean cache,
		Grid2D grid2D,
		Grid3D grid3D,
		DecodeContext<?> decodeContext
	) {
		super(params, valid, cache, decodeContext);
		this.grid2D = grid2D;
		this.grid3D = grid3D;
		if (!(params.type() instanceof FloatColumnValueType || params.type() instanceof DoubleColumnValueType)) {
			throw new IllegalArgumentException("params for noise should be of type float or double.");
		}
	}

	@Override
	public void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		memory.putTyped(CONSTANT_GRID, ConstantValue.ofManual(this.is3D() ? this.grid3D : this.grid2D, type(this.is3D() ? Grid3D.class : Grid2D.class)));
		super.emitFieldGetterAndSetter(memory, context);
	}

	@Override
	public void populateComputeAll(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeAllMethod) {
		ConstantValue constantGrid = memory.getTyped(CONSTANT_GRID);
		computeAllMethod.setCode(
			"""
			grid.getBulkY(
				seed(salt),
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
			.addFunction("seed", (ExpressionParser parser, String name, InsnTree... arguments) -> {
				return new CastResult(context.loadSeed(arguments[0]), false);
			})
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
	public void populateCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		InsnTree x = getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "x"));
		InsnTree z = getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "z"));
		long salt = Permuter.permute(0L, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));
		InsnTree originalSeed = getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "seed"));
		InsnTree saltedSeed = new BitwiseXorInsnTree(originalSeed, ldc(salt), LXOR);
		InsnTree getValueInvoker = invokeInstance(ldc(memory.getTyped(CONSTANT_GRID)), MethodInfo.getMethod(this.is3D() ? Grid3D.class : Grid2D.class, "getValue"), saltedSeed, x, z);
		if (this.params.type() instanceof FloatColumnValueType) getValueInvoker = new OpcodeCastInsnTree(getValueInvoker, D2F, TypeInfos.FLOAT);
		return_(getValueInvoker).emitBytecode(computeMethod);
		computeMethod.endCode();
	}

	@Override
	public void populateCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		computeMethod.setCode(
			this.params.type() instanceof FloatColumnValueType
			? "return(float(grid.getValue(column.seed # salt, column.x, y, column.z)))"
			: "return(grid.getValue(column.seed # salt, column.x, y, column.z))",
			new MutableScriptEnvironment()
			.addVariableConstant("grid", memory.getTyped(CONSTANT_GRID))
			.addMethodInvoke(this.is3D() ? Grid3D.class : Grid2D.class, "getValue")
			.addVariable("column", context.loadColumn())
			.addFieldGet(ScriptedColumn.class, "seed")
			.addVariableConstant("salt", Permuter.permute(0L, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID)))
			.addFieldGet(ScriptedColumn.class, "x")
			.addVariableLoad("y", TypeInfos.INT)
			.addFieldGet(ScriptedColumn.class, "z")
		);
	}

	/** hacks to make 2 java fields share the same JSON field. */
	public static class Coder extends NamedCoder<NoiseColumnEntry> {

		public final AutoCoder<AccessSchema> params;
		public final AutoCoder<Valid> valid;
		public final AutoCoder<Boolean> cache;
		public final AutoCoder<Grid2D> grid2D;
		public final AutoCoder<Grid3D> grid3D;

		public Coder(FactoryContext<NoiseColumnEntry> context) {
			super(context.type);
			this.params = context.type(ReifiedType.from(AccessSchema.class)).forceCreateCoder();
			this.valid  = context.type(ReifiedType.from(Valid.class).addAnnotation(VerifyNullable.INSTANCE)).forceCreateCoder();
			this.cache  = context.type(new ReifiedType<@DefaultBoolean(true) Boolean>() {}).forceCreateCoder();
			this.grid2D = context.type(ReifiedType.from(Grid2D.class)).forceCreateCoder();
			this.grid3D = context.type(ReifiedType.from(Grid3D.class)).forceCreateCoder();
		}

		@Override
		public <T_Encoded> @Nullable NoiseColumnEntry decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;
			AccessSchema params = context.getMember("params").decodeWith(this.params);
			Valid valid = context.getMember("valid").decodeWith(this.valid);
			boolean cache = context.getMember("cache").decodeWith(this.cache);
			if (params.is_3d()) {
				Grid3D grid = context.getMember("grid").decodeWith(this.grid3D);
				return new NoiseColumnEntry(params, valid, cache, null, grid, context);
			}
			else {
				Grid2D grid = context.getMember("grid").decodeWith(this.grid2D);
				return new NoiseColumnEntry(params, valid, cache, grid, null, context);
			}
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, NoiseColumnEntry> context) throws EncodeException {
			NoiseColumnEntry entry = context.input;
			if (entry == null) return context.empty();
			else return context.createStringMap(
				Map.of(
					"params", context.input(entry.params).encodeWith(this.params),
					"valid", context.input(entry.valid).encodeWith(this.valid),
					"cache", context.input(entry.cache).encodeWith(this.cache),
					"grid", (
						entry.params.is_3d()
						? context.input(entry.grid3D).encodeWith(this.grid3D)
						: context.input(entry.grid2D).encodeWith(this.grid2D)
					)
				)
			);
		}
	}
}