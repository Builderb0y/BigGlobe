package builderb0y.bigglobe.columns.scripted;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.world.level.LevelHeightAccessor;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.bigglobe.columns.scripted2.ConstructorInfo;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.scripting.bytecode.FieldConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/**
subclassed at runtime to add necessary fields.
*/
public abstract class ScriptedColumn implements ColumnValueHolder {

	public static final Info INFO = new Info();

	public static class Info extends InfoHolder {

		public MethodInfo
			x,
			z,
			minY,
			maxY,
			hints,
			purpose,
			distantHorizons,
			surfaceOnly,
			worldTraits,
			baseSeed,
			saltedBaseSeed,
			positionedSeed,
			saltedPositionedSeed;

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

		public InsnTree purpose(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.purpose);
		}

		public InsnTree distantHorizons(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.distantHorizons);
		}

		public InsnTree surfaceOnly(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.surfaceOnly);
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
	}

	public static Consumer<MutableScriptEnvironment> baseEnvironment(InsnTree loadColumn) {
		return (MutableScriptEnvironment environment) -> {
			environment

				.addType("ScriptedColumn", loadColumn.getTypeInfo())
				.addVariable("x", INFO.x(loadColumn))
				.addVariable("z", INFO.z(loadColumn))
				.addVariable("minCachedYLevel", INFO.minY(loadColumn))
				.addVariable("maxCachedYLevel", INFO.maxY(loadColumn))
				.addVariable("hints", INFO.hints(loadColumn))
				.addVariable("purpose", INFO.purpose(loadColumn))
				.addVariable("distantHorizons", INFO.distantHorizons(loadColumn))
				.addVariable("surfaceOnly", INFO.surfaceOnly(loadColumn))
				.addVariable("worldSeed", INFO.baseSeed(loadColumn))
				.addFunctionInvoke("worldSeed", loadColumn, INFO.saltedBaseSeed)
				.addVariable("columnSeed", INFO.positionedSeed(loadColumn))
				.addFunctionInvoke("columnSeed", loadColumn, INFO.saltedPositionedSeed)

				.addFieldInvoke("x", INFO.x)
				.addFieldInvoke("z", INFO.z)
				.addFieldInvoke("minCachedYLevel", INFO.minY)
				.addFieldInvoke("maxCachedYLevel", INFO.maxY)
				.addFieldInvoke("hints", INFO.hints)
				.addFieldInvoke("purpose", INFO.purpose)
				.addFieldInvoke("distantHorizons", INFO.distantHorizons)
				.addFieldInvoke("surfaceOnly", INFO.surfaceOnly)

				.configure(hintsEnvironment())
			;
		};
	}

	public static Consumer<MutableScriptEnvironment> hintsEnvironment() {
		return (MutableScriptEnvironment environment) -> {
			environment
				.addType("Hints", Hints.class)
				.addFieldInvokes(Hints.class, "fill", "carve", "isLod", "distanceBetweenColumns", "lod", "usage", "decorate")
				.addCastConstant(ColumnUsage.CONSTANT_FACTORY, true)
			;
		};
	}

	public static final ConstructorInfo CONSTRUCTOR_INFO = new ConstructorInfo(ScriptedColumn.class);

	public static record Params(
		long seed,
		int x,
		int z,
		int minY,
		int maxY,
		Hints hints,
		WorldTraits worldTraits
	) {

		public Params(long seed, int x, int z, LevelHeightAccessor world, Hints hints, WorldTraits traits) {
			this(seed, x, z, HeightLimitViewVersions.getMinY(world), HeightLimitViewVersions.getMaxY(world), hints, traits);
		}

		public Params(BigGlobeScriptedChunkGenerator generator, int x, int z, Hints hints) {
			this(generator.columnSeed, x, z, generator.height.min_y(), generator.height.max_y(), hints, generator.compiledWorldTraits);
		}

		public Params withSeed(long seed) {
			return this.seed == seed ? this : new Params(seed, this.x, this.z, this.minY, this.maxY, this.hints, this.worldTraits);
		}

		public Params at(int x, int z) {
			return this.x == x && this.z == z ? this : new Params(this.seed, x, z, this.minY, this.maxY, this.hints, this.worldTraits);
		}

		public Params heightRange(int minY, int maxY) {
			return this.minY == minY && this.maxY == maxY ? this : new Params(this.seed, this.x, this.z, minY, maxY, this.hints, this.worldTraits);
		}

		public Params heightRange(LevelHeightAccessor world) {
			return this.heightRange(HeightLimitViewVersions.getMinY(world), HeightLimitViewVersions.getMaxY(world));
		}

		public Params hints(Hints hints) {
			return this.hints.equals(hints) ? this : new Params(this.seed, this.x, this.z, this.minY, this.maxY, hints, this.worldTraits);
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

		public Hints voxyHints(int lod) {
			return new Hints(true, UndergroundMode.min(this.defaultUndergroundMode(), BigGlobeConfig.INSTANCE.get().voxyIntegration.undergroundMode), lod, this);
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

	public int x() {
		return this.params.x;
	}

	public int z() {
		return this.params.z;
	}

	public int minY() {
		return this.params.minY;
	}

	public int maxY() {
		return this.params.maxY;
	}

	public Hints hints() {
		return this.params.hints;
	}

	public WorldTraits worldTraits() {
		return this.params.worldTraits;
	}

	@Deprecated
	public String purpose() {
		return this.params.hints.usage.lowerCaseName;
	}

	@Deprecated
	public boolean distantHorizons() {
		return this.params.hints.isLod();
	}

	@Deprecated
	public boolean surfaceOnly() {
		return !this.params.hints.fill();
	}

	public long baseSeed() {
		return this.params.seed;
	}

	public long saltedBaseSeed(long salt) {
		return this.params.seed ^ salt;
	}

	public long positionedSeed() {
		return Permuter.permute(this.params.seed, this.x(), this.z());
	}

	public long saltedPositionedSeed(long salt) {
		return Permuter.permute(this.params.seed ^ salt, this.x(), this.z());
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
}