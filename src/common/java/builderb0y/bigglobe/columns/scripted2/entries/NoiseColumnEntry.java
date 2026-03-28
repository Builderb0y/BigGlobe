package builderb0y.bigglobe.columns.scripted2.entries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.data.BooleanData;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.data.MapData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.columns.scripted.MappedRangeArray;
import builderb0y.bigglobe.columns.scripted.MappedRangeNumberArray;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.classes.ElementSpec;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ColumnValueException;
import builderb0y.bigglobe.columns.scripted2.Valid;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@UseCoder(name = "new", in = NoiseColumnEntry.Coder.class, usage = MemberUsage.METHOD_IS_FACTORY)
public class NoiseColumnEntry extends NonConstantColumnEntry {

	public final @Nullable Seed seed;
	public final Grid2D grid2D;
	public final Grid3D grid3D;

	public NoiseColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		boolean cache,
		Grid2D grid2D,
		Grid3D grid3D,
		@Nullable Seed seed
	) {
		super(params, valid, cache);
		this.seed = seed;
		this.grid2D = grid2D;
		this.grid3D = grid3D;
	}

	@Override
	public void verify(ColumnEntryRegistry registry) throws ColumnValueException {
		super.verify(registry);
		if (!ElementSpec.asType(this.params.type()).getTypeInfo().isFloat()) {
			throw new ColumnValueException("Noise-based column values must be of type float or double.");
		}
	}

	@Override
	public InsnTree makeComputer(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ScriptParsingException {
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		if (this.params.is_3d()) {
			/**
			this.grid3D.getValue(
			this.seed(registry),
			column.x,
			y,
			column.z
			)
			*/
			return invokeInstance(
				ldc(this.grid3D, type(Grid3D.class)),
				Grid3D.INFO.getValue,
				ldc(this.seed(registry)),
				invokeInstance(loadColumn, ScriptedColumn.INFO.x),
				load("y", TypeInfos.INT),
				invokeInstance(loadColumn, ScriptedColumn.INFO.z)
			);
		}
		else {
			/**
			this.grid2D.getValue(
			this.seed(registry),
			column.x,
			column.z
			)
			*/
			return invokeInstance(
				ldc(this.grid2D, type(Grid2D.class)),
				Grid2D.INFO.getValue,
				ldc(this.seed(registry)),
				invokeInstance(loadColumn, ScriptedColumn.INFO.x),
				invokeInstance(loadColumn, ScriptedColumn.INFO.z)
			);
		}
	}

	@Override
	public InsnTree makeBulkComputer(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ScriptParsingException {
		assert this.params.is_3d() : "Requesting bulk computer for 2D noise";
		LoadInsnTree loadColumn = load("this", registry.columnCompileContext.columnTypeInfo());
		InsnTree loadMappedArray = getField(loadColumn, context.valueField.info);
		/**
		this.grid3D.getBulkY(
		this.seed(registry),
		column.x,
		column.backingArray.minCached,
		column.z,
		column.backingArray.cachedPrefix()
		*/
		return invokeInstance(
			ldc(this.grid3D, type(Grid3D.class)),
			Grid3D.INFO.getBulkY,
			ldc(this.seed(registry)),
			invokeInstance(loadColumn, ScriptedColumn.INFO.x),
			getField(loadMappedArray, MappedRangeArray.INFO.minCached),
			invokeInstance(loadColumn, ScriptedColumn.INFO.z),
			invokeInstance(loadMappedArray, MappedRangeNumberArray.CACHED_PREFIX)
		);
	}

	public long seed(ColumnEntryRegistry registry) {
		return this.seed != null ? this.seed.value : Permuter.permute(0L, UnregisteredObjectException.getID(registry.entryOf(this)));
	}

	/**
	hacks to make 2 java fields share the same JSON field.
	*/
	public static class Coder extends NamedCoder<NoiseColumnEntry> {

		public final AutoCoder<AccessSchema> params;
		public final AutoCoder<Valid> valid;
		public final AutoCoder<Boolean> cache;
		public final AutoCoder<Grid2D> grid2D;
		public final AutoCoder<Grid3D> grid3D;
		public final AutoCoder<Seed> seed;

		public Coder(FactoryContext<NoiseColumnEntry> context) {
			super(context.type);
			this.params = context.type(ReifiedType.from(AccessSchema.class)).forceCreateCoder();
			this.valid = context.type(ReifiedType.from(Valid.class).addAnnotation(VerifyNullable.INSTANCE)).forceCreateCoder();
			this.cache = context.type(new ReifiedType<@DefaultBoolean(true) Boolean>() {

			}).forceCreateCoder();
			this.grid2D = context.type(ReifiedType.from(Grid2D.class)).forceCreateCoder();
			this.grid3D = context.type(ReifiedType.from(Grid3D.class)).forceCreateCoder();
			this.seed = context.type(new ReifiedType<Seed>() {

			}).forceCreateCoder();
		}

		@Override
		public <T_Encoded> @Nullable NoiseColumnEntry decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;
			AccessSchema params = context.forceGetMember("params").decodeWith(this.params);
			Valid valid = context.forceGetMember("valid").decodeWith(this.valid);
			boolean cache = context.forceGetMember("cache").decodeWith(this.cache);
			DecodeContext<T_Encoded> encodedSeed = context.forceGetMember("seed");
			Seed seed = encodedSeed.isEmpty() ? null : encodedSeed.decodeWith(this.seed);
			if (params.is_3d()) {
				Grid3D grid = context.forceGetMember("grid").decodeWith(this.grid3D);
				return new NoiseColumnEntry(params, valid, cache, null, grid, seed);
			}
			else {
				Grid2D grid = context.forceGetMember("grid").decodeWith(this.grid2D);
				return new NoiseColumnEntry(params, valid, cache, grid, null, seed);
			}
		}

		@Override
		public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, NoiseColumnEntry> context) throws EncodeException {
			NoiseColumnEntry entry = context.object;
			if (entry == null) return EmptyData.INSTANCE;
			MapData map = new MapData(8);
			map.put("params", context.object(entry.params).encodeWith(this.params));
			if (entry.valid != null) map.put("valid", context.object(entry.valid).encodeWith(this.valid));
			if (!entry.cache) map.put("cache", new BooleanData(false));
			map.put(
				"grid",
				entry.params.is_3d()
					? context.object(entry.grid3D).encodeWith(this.grid3D)
					: context.object(entry.grid2D).encodeWith(this.grid2D)
			);
			if (entry.seed != null) map.put("seed", context.object(entry.seed).encodeWith(this.seed));
			return map;
		}
	}
}