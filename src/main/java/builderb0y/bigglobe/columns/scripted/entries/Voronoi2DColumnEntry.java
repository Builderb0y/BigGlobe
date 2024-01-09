package builderb0y.bigglobe.columns.scripted.entries;

import java.util.function.BiConsumer;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

public class Voronoi2DColumnEntry extends ColumnEntry {

	public static final int
		CELL                  = 1 << 0,
		SOFT_DISTANCE_SQUARED = 1 << 1,
		SOFT_DISTANCE         = 1 << 2,
		HARD_DISTANCE         = 1 << 3,
		ACCESSORS             = 1 << 4;

	public VoronoiDiagram2D.Cell cell;
	public double softDistanceSquared, softDistance, hardDistance;
	public ColumnEntryAccessor[] accessors;

	public static class Registrable extends ColumnEntryRegistrable {

		public final VoronoiDiagram2D value;
		public final Valid valid;

		public Registrable(VoronoiDiagram2D value, Valid valid) {
			this.value = value;
			this.valid = valid;
		}

		@Override
		public boolean hasEntry() {
			return true;
		}

		@Override
		public ColumnEntry createEntry() {
			return new Voronoi2DColumnEntry();
		}

		@Override
		public void createAccessors(String selfName, int slot, BiConsumer<String, ColumnEntryAccessor> accessors) {
			accessors.accept(selfName + ".cell_x",                new               CellXAccessor(slot, this.value));
			accessors.accept(selfName + ".cell_z",                new               CellZAccessor(slot, this.value));
			accessors.accept(selfName + ".center_x",              new             CenterXAccessor(slot, this.value));
			accessors.accept(selfName + ".center_z",              new             CenterZAccessor(slot, this.value));
			accessors.accept(selfName + ".soft_distance_squared", new SoftDistanceSquaredAccessor(slot, this.value));
			accessors.accept(selfName + ".soft_distance",         new        SoftDistanceAccessor(slot, this.value));
			accessors.accept(selfName + ".hard_distance",         new        HardDistanceAccessor(slot, this.value));
			accessors.accept(selfName + ".hard_distance_squared", new HardDistanceSquaredAccessor(slot, this.value));
		}
	}

	public static abstract class Accessor extends ColumnEntryAccessor {

		public final VoronoiDiagram2D diagram;

		public Accessor(int slot, VoronoiDiagram2D diagram) {
			super(slot);
			this.diagram = diagram;
		}

		@Override
		public void setupAndCompile(ColumnEntryRegistry registry) throws ScriptParsingException {}

		public final Voronoi2DColumnEntry entry(ScriptedColumn column) {
			return (Voronoi2DColumnEntry)(column.values[this.slot]);
		}

		public final VoronoiDiagram2D.Cell cell(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			return entry.setFlags(CELL) ? entry.cell = this.diagram.getNearestCell(column.x, column.z, entry.cell) : entry.cell;
		}

		public final VoronoiDiagram2D.Cell cell(ScriptedColumn column) {
			return this.cell(column, this.entry(column));
		}

		@Override
		public void doPopulate(ScriptedColumn column) {
			this.doPopulate(column, this.entry(column));
		}

		public void doPopulate(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			if (entry.setFlags(CELL)) {
				entry.cell = this.diagram.getNearestCell(column.x, column.z, entry.cell);
			}
		}
	}

	public static abstract class PositionAccessor extends Accessor {

		public PositionAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public AccessType getAccessType() {
			return AccessType.INT_2D;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		public abstract int get(ScriptedColumn column);

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Integer.toString(this.get(column));
		}
	}

	public static class CellXAccessor extends PositionAccessor {

		public CellXAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public int get(ScriptedColumn column) {
			return this.cell(column).center.cellX;
		}
	}

	public static class CellZAccessor extends PositionAccessor {

		public CellZAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public int get(ScriptedColumn column) {
			return this.cell(column).center.cellZ;
		}
	}

	public static class CenterXAccessor extends PositionAccessor {

		public CenterXAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public int get(ScriptedColumn column) {
			return this.cell(column).center.centerX;
		}
	}

	public static class CenterZAccessor extends PositionAccessor {

		public CenterZAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public int get(ScriptedColumn column) {
			return this.cell(column).center.centerZ;
		}
	}

	public static abstract class DistanceAccessor extends Accessor {

		public DistanceAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public AccessType getAccessType() {
			return AccessType.DOUBLE_2D;
		}

		public abstract double get(ScriptedColumn column);

		public final double softDistanceSquared(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			return entry.setFlags(SOFT_DISTANCE_SQUARED) ? entry.softDistanceSquared = this.cell(column, entry).progressToEdgeSquaredD(column.x, column.z) : entry.softDistanceSquared;
		}

		public final double softDistance(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			return entry.setFlags(SOFT_DISTANCE) ? entry.softDistance = Math.sqrt(this.softDistanceSquared(column, entry)) : entry.softDistance;
		}

		public final double hardDistance(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			return entry.setFlags(HARD_DISTANCE) ? entry.hardDistance = this.cell(column, entry).hardProgressToEdgeD(column.x, column.z) : entry.hardDistance;
		}

		public final double hardDistanceSquared(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			return BigGlobeMath.squareD(this.hardDistance(column, entry));
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Double.toString(this.get(column));
		}
	}

	public static class SoftDistanceSquaredAccessor extends DistanceAccessor {

		public SoftDistanceSquaredAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public double get(ScriptedColumn column) {
			return this.softDistanceSquared(column, this.entry(column));
		}

		@Override
		public void doPopulate(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			if (entry.setFlags(SOFT_DISTANCE_SQUARED)) {
				entry.softDistanceSquared = this.cell(column, entry).progressToEdgeSquaredD(column.x, column.z);
			}
		}
	}

	public static class SoftDistanceAccessor extends DistanceAccessor {

		public SoftDistanceAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public double get(ScriptedColumn column) {
			return this.softDistance(column, this.entry(column));
		}

		@Override
		public void doPopulate(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			if (entry.setFlags(SOFT_DISTANCE)) {
				entry.softDistance = Math.sqrt(this.softDistanceSquared(column, entry));
			}
		}
	}

	public static class HardDistanceAccessor extends DistanceAccessor {

		public HardDistanceAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public double get(ScriptedColumn column) {
			return this.hardDistance(column, this.entry(column));
		}

		@Override
		public void doPopulate(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			if (entry.setFlags(HARD_DISTANCE)) {
				entry.hardDistance = this.cell(column, entry).hardProgressToEdgeD(column.x, column.z);
			}
		}
	}

	public static class HardDistanceSquaredAccessor extends DistanceAccessor {

		public HardDistanceSquaredAccessor(int slot, VoronoiDiagram2D diagram) {
			super(slot, diagram);
		}

		@Override
		public double get(ScriptedColumn column) {
			return this.hardDistanceSquared(column, this.entry(column));
		}

		@Override
		public void doPopulate(ScriptedColumn column, Voronoi2DColumnEntry entry) {
			if (entry.setFlags(HARD_DISTANCE)) {
				entry.hardDistance = this.cell(column, entry).hardProgressToEdgeD(column.x, column.z);
			}
		}
	}

	public static record Valid(ScriptUsage<GenericScriptTemplateUsage> where) {}
}