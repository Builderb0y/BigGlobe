package builderb0y.bigglobe.columns.scripted;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.world.HeightLimitView;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints.*;
import static builderb0y.scripting.bytecode.InsnTrees.*;

/** subclassed at runtime to add necessary fields. */
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

	//I keep changing the parameters for the constructor,
	//which then means changing the generated subclasses,
	//and having this info be fetched with reflection
	//means I don't have to hard-code all that logic
	//in the ColumnCompileContext code.
	//though now that I've switched to a Params object,
	//I probably don't need this anymore.
	//oh well, it can't hurt if I ever change it back.
	public static final Parameter[] CONSTRUCTOR_PARAMETERS = ScriptedColumn.class.getDeclaredConstructors()[0].getParameters();
	public static final Class<?>[] PARAMETER_CLASSES = Arrays.stream(CONSTRUCTOR_PARAMETERS).map(Parameter::getType).toArray(Class<?>[]::new);
	public static final TypeInfo[] PARAMETER_TYPE_INFOS = Arrays.stream(PARAMETER_CLASSES).map(InsnTrees::type).toArray(TypeInfo[]::new);
	public static final LazyVarInfo[] PARAMETER_VAR_INFOS = Arrays.stream(CONSTRUCTOR_PARAMETERS).map((Parameter parameter) -> new LazyVarInfo(parameter.getName(), type(parameter.getType()))).toArray(LazyVarInfo[]::new);
	public static final LoadInsnTree[] LOADERS = Arrays.stream(PARAMETER_VAR_INFOS).map(InsnTrees::load).toArray(LoadInsnTree[]::new);

	public static record Params(
		long seed,
		int x,
		int z,
		int minY,
		int maxY,
		Hints hints,
		WorldTraits worldTraits
	) {

		public Params(long seed, int x, int z, HeightLimitView world, Hints hints, WorldTraits traits) {
			this(seed, x, z, world.getBottomY(), world.getTopY(), hints, traits);
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

		public Params heightRange(HeightLimitView world) {
			return this.heightRange(world.getBottomY(), world.getTopY());
		}

		public Params hints(Hints hints) {
			return this.hints.equals(hints) ? this : new Params(this.seed, this.x, this.z, this.minY, this.maxY, hints, this.worldTraits);
		}
	}

	public static record Hints(
		boolean isLod,
		byte underground,
		byte lod,
		ColumnUsage usage
	) {

		public static final int
			NO_UNDERGROUND = 0,
			FILL           = 1,
			CARVE          = 2,
			DECORATE       = 3;

		public static boolean isValidUndergroundMode(int mode) {
			return mode >= NO_UNDERGROUND && mode <= DECORATE;
		}

		public Hints(boolean isLod, int flags, int lod, ColumnUsage usage) {
			this(isLod, (byte)(flags), (byte)(lod), usage);
		}

		public boolean fill() {
			return this.underground >= FILL;
		}

		public boolean carve() {
			return this.underground >= CARVE;
		}

		public boolean decorate() {
			return this.underground >= DECORATE;
		}

		public int distanceBetweenColumns() {
			return 1 << this.lod;
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

		public int defaultUndergroundMode() {
			return switch (this) {
				case GENERIC        -> CARVE;
				case HEIGHTMAP      -> FILL;
				case RAW_GENERATION -> DECORATE;
				case FEATURES       -> DECORATE;
			};
		}

		public Hints normalHints() {
			return new Hints(false, this.defaultUndergroundMode(), 0, this);
		}

		public Hints dhHints(int lod) {
			return new Hints(true, Math.min(this.defaultUndergroundMode(), BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.undergroundMode), lod, this);
		}

		public Hints voxyHints(int lod) {
			return new Hints(true, Math.min(this.defaultUndergroundMode(), BigGlobeConfig.INSTANCE.get().voxyIntegration.undergroundMode), lod, this);
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

	public int         x              () { return  this.params.x; }
	public int         z              () { return  this.params.z; }
	public int         minY           () { return  this.params.minY; }
	public int         maxY           () { return  this.params.maxY; }
	public String      purpose        () { return  this.params.hints.usage.lowerCaseName; }
	public boolean     distantHorizons() { return  this.params.hints.isLod(); }
	public boolean     surfaceOnly    () { return !this.params.hints.fill(); }
	public Hints       hints          () { return  this.params.hints; }
	public WorldTraits worldTraits    () { return  this.params.worldTraits; }

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