package builderb0y.bigglobe.columns.scripted.classes;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.SeedPoint;
import builderb0y.bigglobe.util.Derivative2D;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

public class VoronoiBase {

	public static final int
		SOFT_DISTANCE_SQUARED_INDEX = 0,
		SOFT_DISTANCE_INDEX         = 1,
		HARD_DISTANCE_INDEX         = 2,
		EUCLIDEAN_DISTANCE_INDEX    = 3,
		CENTER_COLUMN_INDEX         = 4;

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo
			column;
		public MethodInfo
			cell_x,
			cell_z,
			center_x,
			center_z,
			soft_distance_squared,
			dx_soft_distance_squared,
			dz_soft_distance_squared,
			soft_distance,
			dx_soft_distance,
			dz_soft_distance,
			hard_distance,
			hard_distance_squared,
			euclidean_distance,
			euclidean_distance_squared,
			unsalted_seed,
			salted_seed,
			center_column;
	}

	public final VoronoiDiagram2D.Cell $cell;
	public ScriptedColumn column, center_column;
	public int $flags;
	public final Derivative2D
		$softDistanceSquared = new Derivative2D(),
		$softDistance        = new Derivative2D();
	public double $hardDistance, $euclideanDistance;

	public VoronoiBase(ScriptedColumn column, VoronoiDiagram2D.Cell $cell) {
		this.column = column;
		this.$cell = $cell;
	}

	public long unsalted_seed() {
		SeedPoint seedPoint = this.$cell.center;
		return Permuter.permute(this.column.baseSeed(), seedPoint.cellX, seedPoint.cellZ);
	}

	public long salted_seed(long salt) {
		SeedPoint seedPoint = this.$cell.center;
		return Permuter.permute(this.column.baseSeed() ^ salt, seedPoint.cellX, seedPoint.cellZ);
	}

	public int cell_x() {
		return this.$cell.center.cellX;
	}

	public int cell_z() {
		return this.$cell.center.cellZ;
	}

	public int center_x() {
		return this.$cell.center.centerX;
	}

	public int center_z() {
		return this.$cell.center.centerZ;
	}

	public double soft_distance_squared() {
		this.$pre_compute_soft_distance_squared();
		return this.$softDistanceSquared.value;
	}

	public double dx_soft_distance_squared() {
		this.$pre_compute_soft_distance_squared();
		return this.$softDistanceSquared.dx;
	}

	public double dz_soft_distance_squared() {
		this.$pre_compute_soft_distance_squared();
		return this.$softDistanceSquared.dy;
	}

	public void $pre_compute_soft_distance_squared() {
		int oldFlags = this.$flags;
		int newFlags = oldFlags | (1 << SOFT_DISTANCE_SQUARED_INDEX);
		if (oldFlags != newFlags) {
			this.$flags = newFlags;
			this.$cell.derivativeProgressToEdgeSquaredD(this.$softDistanceSquared, this.column.x(), this.column.z());
		}
	}

	public double soft_distance() {
		this.$pre_compute_soft_distance();
		return this.$softDistance.value;
	}

	public double dx_soft_distance() {
		this.$pre_compute_soft_distance();
		return this.$softDistance.dx;
	}

	public double dz_soft_distance() {
		this.$pre_compute_soft_distance();
		return this.$softDistance.dy;
	}

	public void $pre_compute_soft_distance() {
		int oldFlags = this.$flags;
		int newFlags = oldFlags | (1 << SOFT_DISTANCE_INDEX);
		if (oldFlags != newFlags) {
			this.$flags = newFlags;
			this.$pre_compute_soft_distance_squared();
			this.$softDistance.set(this.$softDistanceSquared).sqrt();
		}
	}

	public double hard_distance() {
		this.$pre_compute_hard_distance();
		return this.$hardDistance;
	}

	public void $pre_compute_hard_distance() {
		int oldFlags = this.$flags;
		int newFlags = oldFlags | (1 << HARD_DISTANCE_INDEX);
		if (oldFlags != newFlags) {
			this.$flags = newFlags;
			this.$hardDistance = this.$cell.hardProgressToEdgeD(this.column.x(), this.column.z());
		}
	}

	public double hard_distance_squared() {
		return BigGlobeMath.squareD(this.hard_distance());
	}

	public double euclidean_distance_squared() {
		return BigGlobeMath.squareD(this.column.x() - this.center_x(), this.column.z() - this.center_z());
	}

	public double euclidean_distance() {
		this.$pre_compute_euclidean_distance();
		return this.$euclideanDistance;
	}

	public void $pre_compute_euclidean_distance() {
		int oldFlags = this.$flags;
		int newFlags = oldFlags | (1 << EUCLIDEAN_DISTANCE_INDEX);
		if (oldFlags != newFlags) {
			this.$flags = newFlags;
			this.$euclideanDistance = Math.sqrt(this.euclidean_distance_squared());
		}
	}

	public ScriptedColumn center_column() {
		this.$pre_compute_center_column();
		return this.center_column;
	}

	public void $pre_compute_center_column() {
		int oldFlags = this.$flags;
		int newFlags = oldFlags | (1 << CENTER_COLUMN_INDEX);
		if (oldFlags != newFlags) {
			this.$flags = newFlags;
			this.center_column = this.column.blankCopy();
			this.center_column.setParamsUnchecked(this.center_column.params.at(this.center_x(), this.center_z()));
		}
	}
}