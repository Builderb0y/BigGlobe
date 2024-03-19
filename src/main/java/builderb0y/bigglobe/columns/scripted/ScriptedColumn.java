package builderb0y.bigglobe.columns.scripted;

import java.lang.reflect.Parameter;
import java.util.Arrays;

import net.minecraft.world.HeightLimitView;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.SeedPoint;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/** subclassed at runtime to add necessary fields. */
public abstract class ScriptedColumn {

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
			seed,
			unsaltedSeed,
			saltedSeed;

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

		public InsnTree seed(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.seed);
		}

		public InsnTree unsaltedSeed(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.unsaltedSeed);
		}

		public InsnTree saltedSeed(InsnTree loadColumn, InsnTree salt) {
			return invokeInstance(loadColumn, this.saltedSeed, salt);
		}
	}

	public static MutableScriptEnvironment baseEnvironment(InsnTree loadColumn) {
		return (
			new MutableScriptEnvironment()
			.addVariable("x", INFO.x(loadColumn))
			.addVariable("z", INFO.z(loadColumn))
			.addVariable("minCachedYLevel", INFO.minY(loadColumn))
			.addVariable("maxCachedYLevel", INFO.maxY(loadColumn))
			.addVariable("purpose", INFO.purpose(loadColumn))
			.addVariable("distantHorizons", INFO.distantHorizons(loadColumn))
			.addVariable("surfaceOnly", INFO.surfaceOnly(loadColumn))
			.addVariable("worldSeed", INFO.seed(loadColumn))
			.addVariable("columnSeed", INFO.unsaltedSeed(loadColumn))
			.addFunctionInvoke("columnSeed", loadColumn, INFO.saltedSeed)
		);
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
		Purpose purpose
	) {

		public Params(long seed, int x, int z, HeightLimitView world, Purpose purpose) {
			this(seed, x, z, world.getBottomY(), world.getTopY(), purpose);
		}

		public Params(BigGlobeScriptedChunkGenerator generator, int x, int z, Purpose purpose) {
			this(generator.columnSeed, x, z, generator.height.min_y(), generator.height.max_y(), purpose);
		}

		public Params withSeed(long seed) {
			return this.seed == seed ? this : new Params(seed, this.x, this.z, this.minY, this.maxY, this.purpose);
		}

		public Params at(int x, int z) {
			return this.x == x && this.z == z ? this : new Params(this.seed, x, z, this.minY, this.maxY, this.purpose);
		}

		public Params heightRange(int minY, int maxY) {
			return this.minY == minY && this.maxY == maxY ? this : new Params(this.seed, this.x, this.z, minY, maxY, this.purpose);
		}

		public Params heightRange(HeightLimitView world) {
			return this.heightRange(world.getBottomY(), world.getTopY());
		}

		public Params purpose(Purpose purpose) {
			return this.purpose == purpose ? this : new Params(this.seed, this.x, this.z, this.minY, this.maxY, purpose);
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

	public long    seed           () { return this.params.seed                 ; }
	public int     x              () { return this.params.x                    ; }
	public int     z              () { return this.params.z                    ; }
	public int     minY           () { return this.params.minY                 ; }
	public int     maxY           () { return this.params.maxY                 ; }
	public String  purpose        () { return this.params.purpose.name       (); }
	public boolean distantHorizons() { return this.params.purpose.isForLODs  (); }
	public boolean surfaceOnly    () { return this.params.purpose.surfaceOnly(); }

	public long unsaltedSeed() {
		return Permuter.permute(this.seed(), this.x(), this.z());
	}

	public long saltedSeed(long salt) {
		return Permuter.permute(this.seed() ^ salt, this.x(), this.z());
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

	/** also subclassed at runtime. */
	public static abstract class VoronoiDataBase {

		public static final int BUILTIN_FLAG_COUNT = 4;

		public static final Info INFO = new Info();
		public static class Info extends InfoHolder {

			public MethodInfo
				column,
				id,
				get_cell_x,
				get_cell_z,
				get_center_x,
				get_center_z,
				get_soft_distance,
				get_soft_distance_squared,
				get_hard_distance,
				get_hard_distance_squared,
				get_euclidean_distance,
				get_euclidean_distance_squared,
				unsalted_seed,
				salted_seed;

			public InsnTree column(InsnTree receiver) {
				return invokeInstance(receiver, this.column);
			}

			public InsnTree id(InsnTree receiver) {
				return invokeInstance(receiver, this.id);
			}

			public InsnTree get_cell_x(InsnTree receiver) {
				return invokeInstance(receiver, this.get_cell_x);
			}

			public InsnTree get_cell_z(InsnTree receiver) {
				return invokeInstance(receiver, this.get_cell_z);
			}

			public InsnTree get_center_x(InsnTree receiver) {
				return invokeInstance(receiver, this.get_center_x);
			}

			public InsnTree get_center_z(InsnTree receiver) {
				return invokeInstance(receiver, this.get_center_z);
			}

			public InsnTree get_soft_distance(InsnTree receiver) {
				return invokeInstance(receiver, this.get_soft_distance);
			}

			public InsnTree get_soft_distance_squared(InsnTree receiver) {
				return invokeInstance(receiver, this.get_soft_distance_squared);
			}

			public InsnTree get_hard_distance(InsnTree receiver) {
				return invokeInstance(receiver, this.get_hard_distance);
			}

			public InsnTree get_hard_distance_squared(InsnTree receiver) {
				return invokeInstance(receiver, this.get_hard_distance_squared);
			}

			public InsnTree get_euclidean_distance(InsnTree receiver) {
				return invokeInstance(receiver, this.get_euclidean_distance);
			}

			public InsnTree get_euclidean_distance_squared(InsnTree receiver) {
				return invokeInstance(receiver, this.get_euclidean_distance_squared);
			}

			public InsnTree unsalted_seed(InsnTree receiver) {
				return invokeInstance(receiver, this.unsalted_seed);
			}

			public InsnTree salted_seed(InsnTree receiver, InsnTree salt) {
				return invokeInstance(receiver, this.salted_seed, salt);
			}
		}

		/* public final synthetic ScriptedColumn$Generated_XXX column; */
		public final VoronoiDiagram2D.Cell cell;
		public int flags_0;
		public double softDistanceSquared, softDistance, hardDistance, euclideanDistance;

		public VoronoiDataBase(VoronoiDiagram2D.Cell cell) {
			this.cell = cell;
		}

		public abstract ScriptedColumn column();

		public abstract String id();

		public long unsalted_seed() {
			SeedPoint seedPoint = this.cell.center;
			return Permuter.permute(this.column().seed(), seedPoint.cellX, seedPoint.cellZ);
		}

		public long salted_seed(long salt) {
			SeedPoint seedPoint = this.cell.center;
			return Permuter.permute(this.column().seed() ^ salt, seedPoint.cellX, seedPoint.cellZ);
		}

		public int get_cell_x() {
			return this.cell.center.cellX;
		}

		public int get_cell_z() {
			return this.cell.center.cellZ;
		}

		public int get_center_x() {
			return this.cell.center.centerX;
		}

		public int get_center_z() {
			return this.cell.center.centerZ;
		}

		public double get_soft_distance_squared() {
			int oldFlags = this.flags_0;
			int newFlags = oldFlags | 1;
			if (oldFlags != newFlags) {
				this.flags_0 = newFlags;
				return this.softDistanceSquared = this.cell.progressToEdgeSquaredD(this.column().x(), this.column().z());
			}
			else {
				return this.softDistanceSquared;
			}
		}

		public double get_soft_distance() {
			int oldFlags = this.flags_0;
			int newFlags = oldFlags | 2;
			if (oldFlags != newFlags) {
				this.flags_0 = newFlags;
				return this.softDistance = Math.sqrt(this.get_soft_distance_squared());
			}
			else {
				return this.softDistance;
			}
		}

		public double get_hard_distance() {
			int oldFlags = this.flags_0;
			int newFlags = oldFlags | 4;
			if (oldFlags != newFlags) {
				this.flags_0 = newFlags;
				return this.hardDistance = this.cell.hardProgressToEdgeD(this.column().x(), this.column().z());
			}
			else {
				return this.hardDistance;
			}
		}

		public double get_hard_distance_squared() {
			return BigGlobeMath.squareD(this.get_hard_distance());
		}

		public double get_euclidean_distance_squared() {
			return BigGlobeMath.squareD(this.column().x() - this.get_center_x(), this.column().z() - this.get_center_z());
		}

		public double get_euclidean_distance() {
			int oldFlags = this.flags_0;
			int newFlags = oldFlags | 8;
			if (oldFlags != newFlags) {
				this.flags_0 = newFlags;
				return this.euclideanDistance = Math.sqrt(this.get_euclidean_distance_squared());
			}
			else {
				return this.euclideanDistance;
			}
		}

		@Override
		public int hashCode() {
			return (this.cell.center.cellX * 31 + this.cell.center.cellZ) ^ this.getClass().hashCode();
		}

		@Override
		public boolean equals(Object object) {
			return this == object || (
				this.getClass() == object.getClass() &&
				this.cell.center.cellX == ((VoronoiDataBase)(object)).cell.center.cellX &&
				this.cell.center.cellZ == ((VoronoiDataBase)(object)).cell.center.cellZ
			);
		}

		@FunctionalInterface
		public static interface Factory {

			public abstract VoronoiDataBase create(ScriptedColumn column, VoronoiDiagram2D.Cell cell);
		}
	}
}