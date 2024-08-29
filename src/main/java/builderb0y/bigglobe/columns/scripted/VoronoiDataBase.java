package builderb0y.bigglobe.columns.scripted;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.SeedPoint;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/** subclassed at runtime to add necessary fields. */
public abstract class VoronoiDataBase implements ColumnValueHolder {

	public static final int
		SOFT_DISTANCE_SQUARED_FLAG = 0,
		SOFT_DISTANCE_FLAG = 1,
		HARD_DISTANCE_FLAG = 2,
		EUCLIDEAN_DISTANCE_FLAG = 3,
		BUILTIN_FLAG_COUNT = 4;

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
			pre_compute_soft_distance,
			pre_compute_soft_distance_squared,
			pre_compute_hard_distance,
			pre_compute_euclidean_distance,
			unsalted_seed,
			salted_seed;
			//note: when adding things here, be sure to update
			//AbstractVoronoiDataCompileContext accordingly.

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

		public InsnTree pre_compute_soft_distance(InsnTree receiver) {
			return invokeInstance(receiver, this.pre_compute_soft_distance);
		}

		public InsnTree get_soft_distance_squared(InsnTree receiver) {
			return invokeInstance(receiver, this.get_soft_distance_squared);
		}

		public InsnTree pre_compute_soft_distance_squared(InsnTree receiver) {
			return invokeInstance(receiver, this.pre_compute_soft_distance_squared);
		}

		public InsnTree get_hard_distance(InsnTree receiver) {
			return invokeInstance(receiver, this.get_hard_distance);
		}

		public InsnTree pre_compute_hard_distance(InsnTree receiver) {
			return invokeInstance(receiver, this.pre_compute_hard_distance);
		}

		public InsnTree get_hard_distance_squared(InsnTree receiver) {
			return invokeInstance(receiver, this.get_hard_distance_squared);
		}

		public InsnTree get_euclidean_distance(InsnTree receiver) {
			return invokeInstance(receiver, this.get_euclidean_distance);
		}

		public InsnTree pre_compute_euclidean_distance(InsnTree receiver) {
			return invokeInstance(receiver, this.pre_compute_euclidean_distance);
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

		public void addAll(MutableScriptEnvironment environment, @Nullable InsnTree loadVoronoiCell) {
			if (loadVoronoiCell != null) {
				environment
				.addVariable("id",                         this.id                            (loadVoronoiCell))
				.addVariable("cell_x",                     this.get_cell_x                    (loadVoronoiCell))
				.addVariable("cell_z",                     this.get_cell_z                    (loadVoronoiCell))
				.addVariable("center_x",                   this.get_center_x                  (loadVoronoiCell))
				.addVariable("center_z",                   this.get_center_z                  (loadVoronoiCell))
				.addVariable("soft_distance_squared",      this.get_soft_distance_squared     (loadVoronoiCell))
				.addVariable("soft_distance",              this.get_soft_distance             (loadVoronoiCell))
				.addVariable("hard_distance_squared",      this.get_hard_distance_squared     (loadVoronoiCell))
				.addVariable("hard_distance",              this.get_hard_distance             (loadVoronoiCell))
				.addVariable("euclidean_distance_squared", this.get_euclidean_distance_squared(loadVoronoiCell))
				.addVariable("euclidean_distance",         this.get_euclidean_distance        (loadVoronoiCell));
			}
			else {
				environment
				.addFieldInvoke("id",                         this.id                            )
				.addFieldInvoke("cell_x",                     this.get_cell_x                    )
				.addFieldInvoke("cell_z",                     this.get_cell_z                    )
				.addFieldInvoke("center_x",                   this.get_center_x                  )
				.addFieldInvoke("center_z",                   this.get_center_z                  )
				.addFieldInvoke("soft_distance_squared",      this.get_soft_distance_squared     )
				.addFieldInvoke("soft_distance",              this.get_soft_distance             )
				.addFieldInvoke("hard_distance_squared",      this.get_hard_distance_squared     )
				.addFieldInvoke("hard_distance",              this.get_hard_distance             )
				.addFieldInvoke("euclidean_distance_squared", this.get_euclidean_distance_squared)
				.addFieldInvoke("euclidean_distance",         this.get_euclidean_distance        );
			}
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
		return Permuter.permute(this.column().baseSeed(), seedPoint.cellX, seedPoint.cellZ);
	}

	public long salted_seed(long salt) {
		SeedPoint seedPoint = this.cell.center;
		return Permuter.permute(this.column().baseSeed() ^ salt, seedPoint.cellX, seedPoint.cellZ);
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

	public void pre_compute_soft_distance_squared() {
		int oldFlags = this.flags_0;
		int newFlags = oldFlags | 1;
		if (oldFlags != newFlags) {
			this.flags_0 = newFlags;
			this.softDistanceSquared = this.cell.progressToEdgeSquaredD(this.column().x(), this.column().z());
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

	public void pre_compute_soft_distance() {
		int oldFlags = this.flags_0;
		int newFlags = oldFlags | 2;
		if (oldFlags != newFlags) {
			this.flags_0 = newFlags;
			this.softDistance = Math.sqrt(this.get_soft_distance_squared());
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

	public void pre_compute_hard_distance() {
		int oldFlags = this.flags_0;
		int newFlags = oldFlags | 4;
		if (oldFlags != newFlags) {
			this.flags_0 = newFlags;
			this.hardDistance = this.cell.hardProgressToEdgeD(this.column().x(), this.column().z());
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

	public void pre_compute_euclidean_distance() {
		int oldFlags = this.flags_0;
		int newFlags = oldFlags | 8;
		if (oldFlags != newFlags) {
			this.flags_0 = newFlags;
			this.euclideanDistance = Math.sqrt(this.get_euclidean_distance_squared());
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(128);
		if (!this.id().isEmpty()) {
			builder.append(this.id()).append(": ");
		}
		return (
			builder
			.append(this.id())
			.append("{ center: ")
			.append(this.get_center_x())
			.append(", ")
			.append(this.get_center_z())
			.append(", cell: ")
			.append(this.get_cell_x())
			.append(", ")
			.append(this.get_cell_z())
			.append(" }")
			.toString()
		);
	}

	@FunctionalInterface
	public static interface Factory {

		public abstract VoronoiDataBase create(ScriptedColumn column, VoronoiDiagram2D.Cell cell);
	}
}