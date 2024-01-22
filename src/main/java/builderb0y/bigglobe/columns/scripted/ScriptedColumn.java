package builderb0y.bigglobe.columns.scripted;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;

/** subclassed at runtime to add necessary fields. */
public abstract class ScriptedColumn {

	public final long seed;
	public int x, z;
	/** the upper and lower bounds of the area that can be cached. */
	public int minY, maxY;

	public ScriptedColumn(long seed, int x, int z, int minY, int maxY) {
		this.seed = seed;
		this.x = x;
		this.z = z;
		this.minY = minY;
		this.maxY = maxY;
	}

	public long columnSeed() {
		return Permuter.permute(this.seed, this.x, this.z);
	}

	public long columnSeed(long salt) {
		return Permuter.permute(this.seed ^ salt, this.x, this.z);
	}

	public abstract void clear();

	public void setPosUnchecked(int x, int z, int minY, int maxY) {
		this.x = x;
		this.z = z;
		this.minY = minY;
		this.maxY = maxY;
		this.clear();
	}

	public void setPos(int x, int z, int minY, int maxY) {
		if (this.x != x || this.z != z || this.minY != minY || this.maxY != maxY) {
			this.setPosUnchecked(x, z, minY, maxY);
		}
	}

	public static interface Factory {

		public abstract ScriptedColumn create(long seed, int x, int z, int minY, int maxY);
	}

	/** also subclassed at runtime. */
	public static abstract class VoronoiDataBase {

		public static final int BUILTIN_FLAG_COUNT = 3;

		/* public final synthetic ScriptedColumn$Generated_XXX column; */
		public final VoronoiDiagram2D.Cell cell;
		public final long seed;
		public int flags_0;
		public double softDistanceSquared, softDistance, hardDistance;

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

		public static interface Factory {

			public abstract VoronoiDataBase create(VoronoiDiagram2D.Cell cell);
		}
	}
}