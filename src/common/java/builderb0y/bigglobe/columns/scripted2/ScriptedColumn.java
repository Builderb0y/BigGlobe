package builderb0y.bigglobe.columns.scripted2;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelHeightAccessor;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.classes.VoronoiSampler;
import builderb0y.bigglobe.columns.scripted2.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted2.entries.ColumnEntry.ColumnEntryContext;
import builderb0y.bigglobe.columns.scripted2.traits.WorldTraits;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.scripting.bytecode.FieldConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.CollectionTransformer;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/**
subclassed at runtime to add data-driven fields.
*/
public abstract class ScriptedColumn {

	public static final Info INFO = new Info();

	public static class Info extends InfoHolder {

		public MethodInfo
			x,
			z,
			minY,
			maxY,
			hints,
			worldTraits,
			baseSeed,
			saltedBaseSeed,
			positionedSeed,
			saltedPositionedSeed,
			positionedSeed3D,
			saltedPositionedSeed3D;

		public InsnTree x(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.x);
		}

		public InsnTree z(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.z);
		}

		public InsnTree minY(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.minY);
		}

		public InsnTree maxY(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.maxY);
		}

		public InsnTree hints(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.hints);
		}

		public InsnTree worldTraits(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.worldTraits);
		}

		public InsnTree baseSeed(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.baseSeed);
		}

		public InsnTree saltedSeed(InsnTree loadColumn, InsnTree salt) {
			return invokeInstance(loadColumn, this.saltedBaseSeed, salt);
		}

		public InsnTree positionedSeed(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.positionedSeed);
		}

		public InsnTree saltedPositionedSeed(InsnTree loadColumn, InsnTree salt) {
			return invokeInstance(loadColumn, this.saltedPositionedSeed, salt);
		}

		public InsnTree positionedSeed3D(InsnTree loadColumn, InsnTree y) {
			return invokeInstance(loadColumn, this.positionedSeed3D, y);
		}

		public InsnTree saltedPositionedSeed3D(InsnTree loadColumn, InsnTree salt, InsnTree y) {
			return invokeInstance(loadColumn, this.saltedPositionedSeed3D, salt, y);
		}
	}

	public static Consumer<MutableScriptEnvironment> baseEnvironment(@Nullable InsnTree loadColumn, @Nullable InsnTree columnLookup, TypeInfo columnType) {
		return (MutableScriptEnvironment environment) -> {
			environment.addType("ColumnStorage", columnType);
			if (loadColumn != null) {
				environment
				.addVariable("column", loadColumn)
				.addVariable("x", INFO.x(loadColumn))
				.addVariable("z", INFO.z(loadColumn))
				.addVariable("minCachedYLevel", INFO.minY(loadColumn))
				.addVariable("maxCachedYLevel", INFO.maxY(loadColumn))
				.addVariable("hints", INFO.hints(loadColumn))
				.addVariable("worldSeed", INFO.baseSeed(loadColumn))
				.addFunctionInvoke("worldSeed", loadColumn, INFO.saltedBaseSeed)
				.addVariable("columnSeed", INFO.positionedSeed(loadColumn))
				.addFunctionInvoke("columnSeed", loadColumn, INFO.saltedPositionedSeed)
				;
			}
			if (columnLookup != null) {
				environment.addFunction("columnAt", Handlers.builder(ScriptedColumnLookup.LOOKUP_COLUMN).addArguments(columnLookup, "II").explicitCast(columnType).buildFunction());
			}

			environment
			.addFieldInvoke("x", INFO.x)
			.addFieldInvoke("z", INFO.z)
			.addFieldInvoke("minCachedYLevel", INFO.minY)
			.addFieldInvoke("maxCachedYLevel", INFO.maxY)
			.addFieldInvoke("hints", INFO.hints)
			.addFieldInvoke("worldSeed", INFO.baseSeed)
			.addMethodInvoke("worldSeed", INFO.saltedBaseSeed)
			.addFieldInvoke("columnSeed", INFO.positionedSeed)
			.addMethodInvoke("columnSeed", INFO.saltedPositionedSeed)

			.addType("Hints", Hints.class)
			.addFieldInvokes(Hints.class, "fill", "carve", "isLod", "distanceBetweenColumns", "lod", "usage", "decorate")
			.addCastConstant(ColumnUsage.CONSTANT_FACTORY, true)

			;

			VoronoiSampler.INFO.addAllTo(environment, columnType);
		};
	}

	public static final ConstructorInfo CONSTRUCTOR_INFO = new ConstructorInfo(ScriptedColumn.class);

	public static record Params(
		int x,
		int z,
		WorldInfo worldInfo,
		Hints hints
	) {

		public Params(long seed, int x, int z, int minY, int maxY, Hints hints, WorldTraits traits) {
			this(x, z, new WorldInfo(seed, minY, maxY, traits), hints);
		}

		public Params(long seed, int x, int z, LevelHeightAccessor world, Hints hints, WorldTraits traits) {
			this(x, z, new WorldInfo(seed, HeightLimitViewVersions.getMinY(world), HeightLimitViewVersions.getMaxY(world), traits), hints);
		}

		public Params(BigGlobeScriptedChunkGenerator generator, int x, int z, Hints hints) {
			this(x, z, new WorldInfo(generator), hints);
		}

		public long seed() {
			return this.worldInfo.seed;
		}

		public int minY() {
			return this.worldInfo.minY;
		}

		public int maxY() {
			return this.worldInfo.maxY;
		}

		public WorldTraits worldTraits() {
			return this.worldInfo.worldTraits;
		}

		public Params at(int x, int z) {
			return this.x == x && this.z == z ? this : new Params(x, z, this.worldInfo, this.hints);
		}

		public Params hints(Hints hints) {
			return this.hints.equals(hints) ? this : new Params(this.x, this.z, this.worldInfo, hints);
		}
	}

	public static record WorldInfo(
		long seed,
		int minY,
		int maxY,
		WorldTraits worldTraits
	) {

		public WorldInfo(BigGlobeScriptedChunkGenerator generator) {
			this(generator.columnSeed, generator.height.min_y(), generator.height.max_y(), generator.compiledWorldTraits);
		}
	}

	public static record Hints(
		boolean isLod,
		UndergroundMode underground,
		byte lod,
		ColumnUsage usage
	) {

		public Hints(boolean isLod, UndergroundMode underground, int lod, ColumnUsage usage) {
			this(isLod, underground, (byte)(lod), usage);
		}

		public boolean fill() {
			return this.underground.shouldFill();
		}

		public boolean carve() {
			return this.underground.shouldCarve();
		}

		public boolean decorate() {
			return this.underground.shouldDecorate();
		}

		public int distanceBetweenColumns() {
			return 1 << this.lod;
		}
	}

	public static enum UndergroundMode {
		NONE,
		FILL,
		CARVE,
		DECORATE;

		public static final UndergroundMode[] VALUES = values();

		public boolean shouldFill() {
			return this.ordinal() >= FILL.ordinal();
		}

		public boolean shouldCarve() {
			return this.ordinal() >= CARVE.ordinal();
		}

		public boolean shouldDecorate() {
			return this.ordinal() >= DECORATE.ordinal();
		}

		public static UndergroundMode min(UndergroundMode a, UndergroundMode b) {
			return VALUES[Math.min(a.ordinal(), b.ordinal())];
		}

		public static UndergroundMode max(UndergroundMode a, UndergroundMode b) {
			return VALUES[Math.max(a.ordinal(), b.ordinal())];
		}
	}

	public static enum ColumnUsage {
		GENERIC,
		HEIGHTMAP,
		RAW_GENERATION,
		FEATURES;

		public static final FieldConstantFactory CONSTANT_FACTORY = FieldConstantFactory.forEnum(ColumnUsage.class, ColumnUsage::lowerCaseName);

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);

		public String lowerCaseName() {
			return this.lowerCaseName;
		}

		public UndergroundMode defaultUndergroundMode() {
			return switch (this) {
				case GENERIC -> UndergroundMode.DECORATE;
				case HEIGHTMAP -> UndergroundMode.FILL;
				case RAW_GENERATION -> UndergroundMode.DECORATE;
				case FEATURES -> UndergroundMode.DECORATE;
			};
		}

		public Hints normalHints() {
			return new Hints(false, this.defaultUndergroundMode(), 0, this);
		}

		public Hints builtinLodHints(int lod) {
			return new Hints(true, UndergroundMode.min(this.defaultUndergroundMode(), BigGlobeConfig.INSTANCE.get().lodRendering.undergroundMode), lod, this);
		}

		public Hints dhHints(int lod) {
			return new Hints(true, UndergroundMode.min(this.defaultUndergroundMode(), BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.undergroundMode), lod, this);
		}

		public Hints maybeDhHints() {
			return this.maybeDhHints(DistantHorizonsCompat.isOnDistantHorizonThread());
		}

		public Hints maybeDhHints(boolean dh) {
			return dh ? this.dhHints(0) : this.normalHints();
		}
	}

	public Params params;

	public ScriptedColumn(Params params) {
		this.params = params;
	}

	@FunctionalInterface
	public static interface Factory {

		public abstract ScriptedColumn create(Params params);
	}

	public static record ConfiguredColumnFactory(
		Factory factory,
		WorldInfo worldInfo,
		Hints hints
	) {

		public Params params(int x, int z) {
			return new Params(x, z, this.worldInfo, this.hints);
		}

		public ScriptedColumn createAt(int x, int z) {
			return this.factory.create(this.params(x, z));
		}

		public ScriptedColumnLookup lookup() {
			return new ScriptedColumnLookup.Impl(this);
		}
	}

	public int x() {
		return this.params.x;
	}

	public int z() {
		return this.params.z;
	}

	public int minY() {
		return this.params.worldInfo.minY;
	}

	public int maxY() {
		return this.params.worldInfo.maxY;
	}

	public Hints hints() {
		return this.params.hints;
	}

	public WorldTraits worldTraits() {
		return this.params.worldInfo.worldTraits;
	}

	public long baseSeed() {
		return this.params.worldInfo.seed;
	}

	public long saltedBaseSeed(long salt) {
		return this.params.worldInfo.seed ^ salt;
	}

	public long positionedSeed() {
		return Permuter.permute(this.params.worldInfo.seed, this.x(), this.z());
	}

	public long saltedPositionedSeed(long salt) {
		return Permuter.permute(this.params.worldInfo.seed ^ salt, this.x(), this.z());
	}

	public long positionedSeed3D(int y) {
		return Permuter.permute(this.params.worldInfo.seed, this.x(), y, this.z());
	}

	public long saltedPositionedSeed3D(long salt, int y) {
		return Permuter.permute(this.params.worldInfo.seed ^ salt, this.x(), y, this.z());
	}

	public abstract ScriptedColumn blankCopy();

	public abstract void clear();

	public void setParams(Params params) {
		if (!this.params.equals(params)) {
			this.params = params;
			this.clear();
		}
	}

	public void setParamsUnchecked(Params params) {
		this.params = params;
		this.clear();
	}

	public static final WeakHashMap<Class<? extends ScriptedColumn>, Map<Holder<ColumnEntry>, ColumnValueInfo>> COLUMN_VALUE_INFOS = new WeakHashMap<>();

	public static Map<Holder<ColumnEntry>, ColumnValueInfo> getColumnValues(ColumnEntryRegistry registry) {
		synchronized (COLUMN_VALUE_INFOS) {
			return COLUMN_VALUE_INFOS.computeIfAbsent(registry.columnClass, (Class<? extends ScriptedColumn> clazz) -> {
				return (
					registry
					.columnEntryLookup
					.values()
					.stream()
					.map((Holder<ColumnEntry> holder) -> {
						try {
							ColumnEntryContext context = registry.columnCompileContext.getCompileContext(holder.value());
							MethodHandle getter = findHandle(registry.columnLookup, context.mainGetter.info);
							if (getter == null) return null;
							boolean is3D = holder.value().params.is_3d();
							if (!is3D) getter = MethodHandles.dropArguments(getter, 1, int.class);
							getter = getter.asType(MethodType.methodType(Object.class, ScriptedColumn.class, int.class));

							MethodHandle setter;
							if (context.mainSetter != null) {
								setter = findHandle(registry.columnLookup, context.mainSetter.info);
								if (setter != null) {
									if (!is3D) setter = MethodHandles.dropArguments(setter, 1, int.class);
									setter = setter.asType(MethodType.methodType(void.class, ScriptedColumn.class, int.class, Object.class));
								}
							}
							else {
								setter = null;
							}

							MethodHandle preComputer;
							if (context.preComputer != null) {
								preComputer = findHandle(registry.columnLookup, context.preComputer.info);
								if (preComputer != null) {
									preComputer = preComputer.asType(MethodType.methodType(void.class, ScriptedColumn.class));
								}
							}
							else {
								preComputer = null;
							}

							return new ColumnValueInfo(holder, getter, preComputer, setter);
						}
						catch (Exception exception) {
							BigGlobeMod.LOGGER.error("Exception creating column value info for " + UnregisteredObjectException.getID(holder), exception);
							return null;
						}
					})
					.collect(Collectors.toMap(ColumnValueInfo::holder, Function.identity()))
				);
			});
		}
	}

	public static MethodHandle findHandle(MethodHandles.Lookup lookup, MethodInfo info) {
		try {
			return lookup.findVirtual(
				lookup.lookupClass(),
				info.name,
				MethodType.methodType(
					info.returnType.toClass(lookup.lookupClass().getClassLoader()),
					CollectionTransformer.convertArray(
						info.paramTypes,
						Class<?>[]::new,
						(TypeInfo paramType) -> paramType.toClass(lookup.lookupClass().getClassLoader())
					)
				)
			);
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Could not find method " + info + " in " + lookup.lookupClass(), exception);
			return null;
		}
	}

	public static record ColumnValueInfo(
		Holder<ColumnEntry> holder,
		MethodHandle getter, //(ScriptedColumn, int) -> Object
		@Nullable MethodHandle preComputer, //(ScriptedColumn) -> void
		@Nullable MethodHandle setter //(ScriptedColumn, int, Object) -> void
	) {

		public Identifier id() {
			return UnregisteredObjectException.getID(this.holder);
		}

		@Override
		public String toString() {
			return this.id().toString();
		}
	}
}