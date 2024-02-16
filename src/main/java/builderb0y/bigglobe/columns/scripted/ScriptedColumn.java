package builderb0y.bigglobe.columns.scripted;

import java.lang.reflect.Parameter;
import java.util.Arrays;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.SeedPoint;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.util.BoundInfoHolder;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/** subclassed at runtime to add necessary fields. */
public abstract class ScriptedColumn {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo seed, x, z, minY, maxY, distantHorizons, heightmapOnly;
		public MethodInfo unsaltedSeed, saltedSeed;

		public InsnTree seed(InsnTree loadColumn) {
			return getField(loadColumn, this.seed.makeFinal());
		}

		public InsnTree x(InsnTree loadColumn) {
			return getField(loadColumn, this.x.makeFinal());
		}

		public InsnTree z(InsnTree loadColumn) {
			return getField(loadColumn, this.z.makeFinal());
		}

		public InsnTree minY(InsnTree loadColumn) {
			return getField(loadColumn, this.minY.makeFinal());
		}

		public InsnTree maxY(InsnTree loadColumn) {
			return getField(loadColumn, this.maxY.makeFinal());
		}

		public InsnTree distantHorizons(InsnTree loadColumn) {
			return getField(loadColumn, this.distantHorizons.makeFinal());
		}

		public InsnTree heightmapOnly(InsnTree loadColumn) {
			return getField(loadColumn, this.heightmapOnly.makeFinal());
		}

		public InsnTree unsaltedSeed(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.unsaltedSeed);
		}

		public InsnTree saltedSeed(InsnTree loadColumn, InsnTree salt) {
			return invokeInstance(loadColumn, this.saltedSeed, salt);
		}
	}

	public static class BoundInfo extends BoundInfoHolder {

		public InsnTree seed, x, z, minY, maxY, distantHorizons, heightmapOnly;

		public BoundInfo(InsnTree loadSelf) {
			super(INFO, loadSelf);
		}
	}

	public static final Parameter[] CONSTRUCTOR_PARAMETERS = ScriptedColumn.class.getDeclaredConstructors()[0].getParameters();
	public static final Class<?>[] PARAMETER_CLASSES = Arrays.stream(CONSTRUCTOR_PARAMETERS).map(Parameter::getType).toArray(Class<?>[]::new);
	public static final TypeInfo[] PARAMETER_TYPE_INFOS = Arrays.stream(PARAMETER_CLASSES).map(InsnTrees::type).toArray(TypeInfo[]::new);
	public static final LazyVarInfo[] PARAMETER_VAR_INFOS = Arrays.stream(CONSTRUCTOR_PARAMETERS).map((Parameter parameter) -> new LazyVarInfo(parameter.getName(), type(parameter.getType()))).toArray(LazyVarInfo[]::new);
	public static final LoadInsnTree[] LOADERS = Arrays.stream(PARAMETER_VAR_INFOS).map(InsnTrees::load).toArray(LoadInsnTree[]::new);

	public final long seed;
	public int x, z;
	/** the upper and lower bounds of the area that can be cached. */
	public int minY, maxY;
	public boolean distantHorizons;
	public boolean heightmapOnly;

	public ScriptedColumn(long seed, int x, int z, int minY, int maxY, boolean distantHorizons) {
		this.seed = seed;
		this.x = x;
		this.z = z;
		this.minY = minY;
		this.maxY = maxY;
		this.distantHorizons = distantHorizons;
	}

	public long unsaltedSeed() {
		return Permuter.permute(this.seed, this.x, this.z);
	}

	public long saltedSeed(long salt) {
		return Permuter.permute(this.seed ^ salt, this.x, this.z);
	}

	public abstract ScriptedColumn blankCopy();

	public abstract void clear();

	public void setPosUnchecked(int x, int z) {
		this.x = x;
		this.z = z;
		this.clear();
	}

	public void setPos(int x, int z) {
		if (this.x != x || this.z != z) {
			this.x = x;
			this.z = z;
			this.clear();
		}
	}

	public void setPosDH(int x, int z, boolean distantHorizons) {
		if (this.x != x || this.z != z || this.distantHorizons != distantHorizons) {
			this.x = x;
			this.z = z;
			this.distantHorizons = distantHorizons;
			this.clear();
		}
	}

	public static interface Factory {

		public abstract ScriptedColumn create(long seed, int x, int z, int minY, int maxY, boolean distantHorizons);
	}

	/** also subclassed at runtime. */
	public static abstract class VoronoiDataBase {

		public static final int BUILTIN_FLAG_COUNT = 4;

		public static final Info INFO = new Info();
		public static class Info extends InfoHolder {

			public MethodInfo
				column,
				id,
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
		public final long seed;
		public int flags_0;
		public double softDistanceSquared, softDistance, hardDistance, euclideanDistance;

		public VoronoiDataBase(VoronoiDiagram2D.Cell cell, long baseSeed) {
			this.cell = cell;
			this.seed = cell.center.getSeed(baseSeed);
		}

		public abstract ScriptedColumn column();

		public abstract String id();

		public long unsalted_seed() {
			SeedPoint seedPoint = this.cell.center;
			return Permuter.permute(this.column().seed, seedPoint.cellX, seedPoint.cellZ);
		}

		public long salted_seed(long salt) {
			SeedPoint seedPoint = this.cell.center;
			return Permuter.permute(this.column().seed ^ salt, seedPoint.cellX, seedPoint.cellZ);
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
				return this.softDistanceSquared = this.cell.progressToEdgeSquaredD(this.column().x, this.column().z);
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
				return this.hardDistance = this.cell.hardProgressToEdgeD(this.column().x, this.column().z);
			}
			else {
				return this.hardDistance;
			}
		}

		public double get_hard_distance_squared() {
			return BigGlobeMath.squareD(this.get_hard_distance());
		}

		public double get_euclidean_distance_squared() {
			return BigGlobeMath.squareD(this.column().x - this.get_center_x(), this.column().z - this.get_center_z());
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