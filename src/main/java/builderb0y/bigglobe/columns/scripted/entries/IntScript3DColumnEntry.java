package builderb0y.bigglobe.columns.scripted.entries;

import java.util.function.BiConsumer;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.DefaultInt;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptValueProvider.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ScriptValueProvider.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptValueProvider.ColumnYToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

public class IntScript3DColumnEntry extends ColumnEntry {

	public static final int ARRAY = 1 << 0;

	public NumberArray array;
	/**
	min and max y of our {@link Valid} bounds.
	of no relation to {@link ScriptedColumn#minY} and maxY.
	*/
	public int minY, maxY;

	public static class Registrable extends ColumnEntryRegistrable {

		public final ScriptUsage<GenericScriptTemplateUsage> value;
		public final @VerifyNullable Valid valid;
		public final @DefaultBoolean(true) boolean cache;

		public Registrable(ScriptUsage<GenericScriptTemplateUsage> value, @VerifyNullable Valid valid, @DefaultBoolean(true) boolean cache) {
			this.value = value;
			this.valid = valid;
			this.cache = cache;
		}

		@Override
		public boolean hasEntry() {
			return this.cache;
		}

		@Override
		public ColumnEntry createEntry() {
			return new IntScript3DColumnEntry();
		}

		@Override
		public void createAccessors(String selfName, int slot, BiConsumer<String, ColumnEntryAccessor> accessors) {
			accessors.accept(selfName, new Accessor(this.cache ? slot : -1, this));
		}
	}

	public static class Accessor extends ColumnEntryAccessor {

		public Registrable source;
		public ColumnYToIntScript.Holder value;
		public ColumnToBooleanScript.Holder where;
		public ColumnToIntScript.Holder min_y;
		public ColumnToIntScript.Holder max_y;
		public int fallback;

		public Accessor(int slot, Registrable source) {
			super(slot);
			this.source = source;
		}

		@Override
		public AccessType getAccessType() {
			return AccessType.INT_3D;
		}

		@Override
		public void setupAndCompile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.value = new ColumnYToIntScript.Holder(this.source.value);
			Valid valid = this.source.valid;
			if (valid != null) {
				if (valid.where != null) {
					this.where = new ColumnToBooleanScript.Holder(valid.where);
				}
				if (valid.min_y != null) {
					this.min_y = new ColumnToIntScript.Holder(valid.min_y);
				}
				if (valid.max_y != null) {
					this.max_y = new ColumnToIntScript.Holder(valid.max_y);
				}
				this.fallback = valid.fallback;
			}
			this.source = null;
		}

		public final IntScript3DColumnEntry entry(ScriptedColumn column) {
			return (IntScript3DColumnEntry)(column.values[this.slot]);
		}

		@Override
		public void doPopulate(ScriptedColumn column) {
			IntScript3DColumnEntry entry = this.entry(column);
			boolean needPopulation = this.populateBounds(column, entry);
			int actualMinY = Math.max(entry.minY, column.minY);
			int actualMaxY = Math.min(entry.maxY, column.maxY);
			if (needPopulation) {
				NumberArray array = entry.array;
				ColumnYToIntScript.Holder script = this.value;
				for (int y2 = actualMinY; y2 < actualMaxY; y2++) {
					array.setI(y2 - actualMinY, script.compute(column, y2));
				}
			}
		}

		public NumberArray allocateArray(int length) {
			return NumberArray.allocateIntsHeap(length);
		}

		public boolean populateBounds(ScriptedColumn column, IntScript3DColumnEntry entry) {
			if (entry.setFlags(ARRAY)) {
				entry.minY = Integer.MIN_VALUE;
				entry.maxY = Integer.MAX_VALUE;
				if (this.where != null && !this.where.compute(column)) {
					entry.minY = 0;
					entry.maxY = -1;
					return false;
				}
				if (this.min_y != null) {
					entry.minY = this.min_y.compute(column);
				}
				if (this.max_y != null) {
					entry.maxY = this.max_y.compute(column);
				}
				int actualMinY = Math.max(entry.minY, column.minY);
				int actualMaxY = Math.min(entry.maxY, column.maxY);
				int capacity = actualMaxY - actualMinY + 1;
				if (capacity <= 0) {
					return false;
				}
				NumberArray array = entry.array;
				if (array == null) {
					entry.array = this.allocateArray(capacity);
				}
				else if (array.length() < capacity) {
					entry.array = this.allocateArray(Math.max(array.length() << 1, capacity));
				}
				return true;
			}
			return false;
		}

		public boolean isValid(ScriptedColumn column, int y) {
			if (this.where != null && !this.where.compute(column)) {
				return false;
			}
			if (this.min_y != null && y < this.min_y.compute(column)) {
				return false;
			}
			if (this.max_y != null && y >= this.max_y.compute(column)) {
				return false;
			}
			return true;
		}

		public int get(ScriptedColumn column, int y) {
			if (this.slot >= 0) {
				IntScript3DColumnEntry entry = this.entry(column);
				boolean needPopulation = this.populateBounds(column, entry);
				int actualMinY = Math.max(entry.minY, column.minY);
				int actualMaxY = Math.min(entry.maxY, column.maxY);
				if (needPopulation) {
					NumberArray array = entry.array;
					ColumnYToIntScript.Holder script = this.value;
					for (int y2 = actualMinY; y2 < actualMaxY; y2++) {
						array.setI(y2 - actualMinY, script.compute(column, y2));
					}
				}
				if (y >= actualMinY && y <= actualMaxY) {
					return entry.array.getI(y - entry.minY);
				}
				else if (y >= entry.minY && y <= entry.maxY) {
					return this.value.compute(column, y);
				}
				else {
					return this.fallback;
				}
			}
			else {
				return this.isValid(column, y) ? this.value.compute(column, y) : this.fallback;
			}
		}

		public void set(ScriptedColumn column, int y, int value) {
			IntScript3DColumnEntry entry = this.entry(column);
			int actualMinY = Math.max(entry.minY, column.minY);
			int actualMaxY = Math.min(entry.maxY, column.maxY);
			if (y >= actualMinY && y <= actualMaxY) {
				entry.array.setI(y - actualMinY, value);
			}
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Integer.toString(this.get(column, y));
		}
	}

	public static record Valid(
		ScriptUsage<GenericScriptTemplateUsage> where,
		ScriptUsage<GenericScriptTemplateUsage> min_y,
		ScriptUsage<GenericScriptTemplateUsage> max_y,
		@DefaultInt(0) int fallback
	) {}
}