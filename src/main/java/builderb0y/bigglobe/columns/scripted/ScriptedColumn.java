package builderb0y.bigglobe.columns.scripted;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Consumer;

import net.minecraft.world.HeightLimitView;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.InfoHolder;

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
			.addVariable("purpose", INFO.purpose(loadColumn))
			.addVariable("distantHorizons", INFO.distantHorizons(loadColumn))
			.addVariable("surfaceOnly", INFO.surfaceOnly(loadColumn))
			.addVariable("worldSeed", INFO.baseSeed(loadColumn))
			.addFunctionInvoke("worldSeed", loadColumn, INFO.saltedBaseSeed)
			.addVariable("columnSeed", INFO.positionedSeed(loadColumn))
			.addFunctionInvoke("columnSeed", loadColumn, INFO.saltedPositionedSeed)
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
		Purpose purpose,
		WorldTraits worldTraits
	) {

		public Params(long seed, int x, int z, HeightLimitView world, Purpose purpose, WorldTraits traits) {
			this(seed, x, z, world.getBottomY(), world.getTopY(), purpose, traits);
		}

		public Params(BigGlobeScriptedChunkGenerator generator, int x, int z, Purpose purpose) {
			this(generator.columnSeed, x, z, generator.height.min_y(), generator.height.max_y(), purpose, generator.compiledWorldTraits);
		}

		public Params withSeed(long seed) {
			return this.seed == seed ? this : new Params(seed, this.x, this.z, this.minY, this.maxY, this.purpose, this.worldTraits);
		}

		public Params at(int x, int z) {
			return this.x == x && this.z == z ? this : new Params(this.seed, x, z, this.minY, this.maxY, this.purpose, this.worldTraits);
		}

		public Params heightRange(int minY, int maxY) {
			return this.minY == minY && this.maxY == maxY ? this : new Params(this.seed, this.x, this.z, minY, maxY, this.purpose, this.worldTraits);
		}

		public Params heightRange(HeightLimitView world) {
			return this.heightRange(world.getBottomY(), world.getTopY());
		}

		public Params purpose(Purpose purpose) {
			return this.purpose == purpose ? this : new Params(this.seed, this.x, this.z, this.minY, this.maxY, purpose, this.worldTraits);
		}
	}

	public static interface Purpose {

		public static final Purpose
			GENERIC        = new Impl("generic", false, false),
			GENERIC_DH     = new DHLod("generic"),
			GENERIC_VOXY   = new VoxyLod("generic"),
			HEIGHTMAP      = new Impl("heightmap", false, true),
			HEIGHTMAP_DH   = new Impl("heightmap", true, true),
			HEIGHTMAP_VOXY = new Impl("heightmap", true, true),
			RAW_GENERATION = new Impl("raw_generation", false, false),
			RAW_DH         = new DHLod("raw_generation"),
			RAW_VOXY       = new VoxyLod("raw_generation"),
			FEATURES       = new Impl("features", false, false),
			FEATURES_DH    = new DHLod("features"),
			FEATURES_VOXY  = new VoxyLod("features");

		public static Purpose generic() {
			return generic(DistantHorizonsCompat.isOnDistantHorizonThread());
		}

		public static Purpose generic(boolean distantHorizons) {
			return distantHorizons ? GENERIC_DH : GENERIC;
		}

		public static Purpose rawGeneration() {
			return rawGeneration(DistantHorizonsCompat.isOnDistantHorizonThread());
		}

		public static Purpose rawGeneration(boolean distantHorizons) {
			return distantHorizons ? RAW_DH : RAW_GENERATION;
		}

		public static Purpose features() {
			return features(DistantHorizonsCompat.isOnDistantHorizonThread());
		}

		public static Purpose features(boolean distantHorizons) {
			return distantHorizons ? FEATURES_DH : FEATURES;
		}

		public static Purpose heightmap() {
			return heightmap(DistantHorizonsCompat.isOnDistantHorizonThread());
		}

		public static Purpose heightmap(boolean distantHorizons) {
			return distantHorizons ? HEIGHTMAP_DH : HEIGHTMAP;
		}

		public abstract String name();

		public abstract boolean isForLODs();

		public abstract boolean surfaceOnly();

		public static record Impl(String name, boolean isForLODs, boolean surfaceOnly) implements Purpose {

			public Impl {
				name = name.intern();
			}
		}

		public static abstract class Lod implements Purpose {

			public final String name;

			public Lod(String name) {
				this.name = name.intern();
			}

			@Override
			public String name() {
				return this.name;
			}

			@Override
			public boolean isForLODs() {
				return true;
			}
		}

		public static class DHLod extends Lod {

			public DHLod(String name) {
				super(name);
			}

			@Override
			public boolean surfaceOnly() {
				return BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipUnderground;
			}
		}

		public static class VoxyLod extends Lod {

			public VoxyLod(String name) {
				super(name);
			}

			@Override
			public boolean surfaceOnly() {
				return BigGlobeConfig.INSTANCE.get().voxyIntegration.skipUnderground;
			}
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

	public int         x              () { return this.params.x                    ; }
	public int         z              () { return this.params.z                    ; }
	public int         minY           () { return this.params.minY                 ; }
	public int         maxY           () { return this.params.maxY                 ; }
	public String      purpose        () { return this.params.purpose.name       (); }
	public boolean     distantHorizons() { return this.params.purpose.isForLODs  (); }
	public boolean     surfaceOnly    () { return this.params.purpose.surfaceOnly(); }
	public WorldTraits worldTraits    () { return this.params.worldTraits          ; }

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