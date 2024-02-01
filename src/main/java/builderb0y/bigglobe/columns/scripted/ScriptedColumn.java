package builderb0y.bigglobe.columns.scripted;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.BoundInfoHolder;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/** subclassed at runtime to add necessary fields. */
public abstract class ScriptedColumn {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo seed, x, z, minY, maxY, distantHorizons;
		public MethodInfo unsaltedSeed, saltedSeed;

		public InsnTree seed(InsnTree loadColumn) {
			return getField(loadColumn, this.seed);
		}

		public InsnTree x(InsnTree loadColumn) {
			return getField(loadColumn, this.x);
		}

		public InsnTree z(InsnTree loadColumn) {
			return getField(loadColumn, this.z);
		}

		public InsnTree minY(InsnTree loadColumn) {
			return getField(loadColumn, this.minY);
		}

		public InsnTree maxY(InsnTree loadColumn) {
			return getField(loadColumn, this.maxY);
		}

		public InsnTree distantHorizons(InsnTree loadColumn) {
			return getField(loadColumn, this.distantHorizons);
		}

		public InsnTree unsaltedSeed(InsnTree loadColumn) {
			return invokeInstance(loadColumn, this.unsaltedSeed);
		}

		public InsnTree saltedSeed(InsnTree loadColumn, InsnTree salt) {
			return invokeInstance(loadColumn, this.saltedSeed, salt);
		}
	}

	public static class BoundInfo extends BoundInfoHolder {

		public InsnTree seed, x, z, minY, maxY;

		public BoundInfo(InfoHolder holder, InsnTree loadSelf) {
			super(holder, loadSelf);
		}
	}

	public final long seed;
	public int x, z;
	/** the upper and lower bounds of the area that can be cached. */
	public int minY, maxY;
	public boolean distantHorizons;

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

	public void setPos(int x, int z) {
		if (this.x != x || this.z != z) {
			this.x = x;
			this.z = z;
			this.clear();
		}
	}

	public void setParams(int x, int z, int minY, int maxY, boolean distantHorizons) {
		if (this.x != x || this.z != z || this.minY != minY || this.maxY != maxY || this.distantHorizons != distantHorizons) {
			this.x = x;
			this.z = z;
			this.minY = minY;
			this.maxY = maxY;
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

		public static interface Factory {

			public abstract VoronoiDataBase create(ScriptedColumn column, VoronoiDiagram2D.Cell cell);
		}
	}
}