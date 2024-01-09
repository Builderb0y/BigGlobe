package builderb0y.bigglobe.columns.scripted.entries;

import java.util.function.BiConsumer;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.DefaultInt;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptProviders.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ScriptProviders.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

public class IntScript2DColumnEntry extends ColumnEntry {

	public static final int VALUE = 1 << 0;

	public int value;

	public static class Registrable extends ColumnEntryRegistrable {

		public final @DefaultBoolean(true) boolean cache;
		public final ScriptUsage<GenericScriptTemplateUsage> value;
		public final @VerifyNullable Valid valid;

		public Registrable(ScriptUsage<GenericScriptTemplateUsage> value, @VerifyNullable Valid valid, @DefaultBoolean(true) boolean cache) {
			this.cache = cache;
			this.value = value;
			this.valid = valid;
		}

		@Override
		public boolean hasEntry() {
			return this.cache;
		}

		@Override
		public ColumnEntry createEntry() {
			return new IntScript2DColumnEntry();
		}

		@Override
		public void createAccessors(String selfName, int slot, BiConsumer<String, ColumnEntryAccessor> accessors) {
			accessors.accept(selfName, new Accessor(this.cache ? slot : -1, this));
		}
	}

	public static class Accessor extends ColumnEntryAccessor {

		public Registrable source;
		public ColumnToIntScript.Holder provider;
		public ColumnToBooleanScript.@VerifyNullable Holder where;
		public int fallback;

		public Accessor(int slot, Registrable source) {
			super(slot);
			this.source = source;
		}

		@Override
		public AccessType getAccessType() {
			return AccessType.INT_2D;
		}

		@Override
		public void setupAndCompile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.provider = new ColumnToIntScript.Holder(this.source.value);
			Valid valid = this.source.valid;
			if (valid != null) {
				if (valid.where != null) {
					this.where = new ColumnToBooleanScript.Holder(valid.where);
				}
				this.fallback = valid.fallback;
			}
			this.source = null;
		}

		public final IntScript2DColumnEntry entry(ScriptedColumn column) {
			return (IntScript2DColumnEntry)(column.values[this.slot]);
		}

		@Override
		public void doPopulate(ScriptedColumn column) {
			IntScript2DColumnEntry entry = this.entry(column);
			if (entry.setFlags(VALUE)) {
				if (this.where != null && !this.where.compute(column)) {
					entry.value = this.fallback;
				}
				else {
					entry.value = this.provider.compute(column);
				}
			}
		}

		public int get(ScriptedColumn column) {
			if (this.slot >= 0) {
				IntScript2DColumnEntry entry = this.entry(column);
				if (entry.setFlags(VALUE)) {
					if (this.where != null && !this.where.compute(column)) {
						return entry.value = this.fallback;
					}
					else {
						return entry.value = this.provider.compute(column);
					}
				}
				else {
					return entry.value;
				}
			}
			else {
				if (this.where != null && !this.where.compute(column)) {
					return this.fallback;
				}
				else {
					return this.provider.compute(column);
				}
			}
		}

		public void set(ScriptedColumn column, int value) {
			this.entry(column).value = value;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Integer.toString(this.get(column));
		}
	}

	public static record Valid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultInt(0) int fallback) {}
}