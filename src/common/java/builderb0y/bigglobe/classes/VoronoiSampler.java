package builderb0y.bigglobe.classes;

import builderb0y.bigglobe.columns.scripted.ConstructorInfo;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.Cell;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.SeedPoint;
import builderb0y.bigglobe.util.Derivative2D;
import builderb0y.scripting.bytecode.TypeInfo;

public class VoronoiSampler {

	public static final TypeInfo TYPE = TypeInfo.of(VoronoiSampler.class);

	public static final int
		FLAG_CENTER                = 1 << 0,
		FLAG_CELL                  = 1 << 1,
		FLAG_SOFT_DISTANCE_SQUARED = 1 << 2,
		FLAG_SOFT_DISTANCE         = 1 << 3,
		FLAG_HARD_DISTANCE         = 1 << 4,
		FLAG_EUCLIDEAN_DISTANCE    = 1 << 5;

	public static final ConstructorInfo CONSTRUCTOR = new ConstructorInfo(VoronoiSampler.class);

	public final VoronoiDiagram2D diagram;
	public final ScriptedColumn column;
	public ScriptedColumn centerColumn;
	public int flags;
	public final Derivative2D
		softDistanceSquared = new Derivative2D(),
		softDistance        = new Derivative2D();
	public double hardDistance, euclideanDistance;
	public SeedPoint center;
	public Cell cell;

	public VoronoiSampler(VoronoiDiagram2D diagram, ScriptedColumn column) {
		this.diagram = diagram;
		this.column  = column;
	}

	public void clear() {
		this.flags = 0;
	}

	public boolean setFlag(int flag) {
		int oldFlags = this.flags;
		int newFlags = oldFlags | flag;
		if (oldFlags != newFlags) {
			this.flags = newFlags;
			return true;
		}
		else {
			return false;
		}
	}

	public SeedPoint center() {
		if (this.setFlag(FLAG_CENTER)) {
			this.center = this.diagram.getNearestSeedPoint(this.column.x(), this.column.z(), this.center);
		}
		return this.center;
	}

	public Cell cell() {
		if (this.setFlag(FLAG_CELL)) {
			this.cell = this.diagram.getCellCached(this.center());
		}
		return this.cell;
	}

	public int cellX() {
		return this.center().cellX;
	}

	public int cellZ() {
		return this.center().cellZ;
	}

	public int centerX() {
		return this.center().centerX;
	}

	public int centerZ() {
		return this.center().centerZ;
	}

	public long nextSeed(long seed) {
		SeedPoint center = this.center();
		return Permuter.permute(seed, center.cellX, center.cellZ);
	}

	public void preComputeSoftDistanceSquared() {
		if (this.setFlag(FLAG_SOFT_DISTANCE_SQUARED)) {
			this.cell().derivativeProgressToEdgeSquaredD(this.softDistanceSquared, this.column.x(), this.column.z());
		}
	}

	public double softDistanceSquared() {
		this.preComputeSoftDistanceSquared();
		return this.softDistanceSquared.value;
	}

	public double dxSoftDistanceSquared() {
		this.preComputeSoftDistanceSquared();
		return this.softDistanceSquared.dx;
	}

	public double dzSoftDistanceSquared() {
		this.preComputeSoftDistanceSquared();
		return this.softDistanceSquared.dy;
	}

	public void preComputeSoftDistance() {
		if (this.setFlag(FLAG_SOFT_DISTANCE)) {
			this.softDistance.set(this.softDistanceSquared()).sqrt();
		}
	}

	public double softDistance() {
		this.preComputeSoftDistance();
		return this.softDistance.value;
	}

	public double dxSoftDistance() {
		this.preComputeSoftDistance();
		return this.softDistance.dx;
	}

	public double dzSoftDistance() {
		this.preComputeSoftDistance();
		return this.softDistance.dy;
	}

	public double hardDistance() {
		if (this.setFlag(FLAG_HARD_DISTANCE)) {
			return this.hardDistance = this.cell().hardProgressToEdgeD(this.column.x(), this.column.z());
		}
		return this.hardDistance;
	}

	public double hardDistanceSquared() {
		return BigGlobeMath.squareD(this.hardDistance());
	}

	public double euclideanDistanceSquared() {
		return BigGlobeMath.squareD(this.column.x() - this.centerX(), this.column.z() - this.centerZ());
	}

	public double euclideanDistance() {
		if (this.setFlag(FLAG_EUCLIDEAN_DISTANCE)) {
			return this.euclideanDistance = Math.sqrt(this.euclideanDistanceSquared());
		}
		return this.euclideanDistance;
	}

	public ScriptedColumn centerColumn() {
		if (this.centerColumn == null) {
			this.centerColumn = this.column.blankCopy();
		}
		this.centerColumn.setParams(this.centerColumn.params.at(this.centerX(), this.centerZ()));
		return this.centerColumn;
	}
}