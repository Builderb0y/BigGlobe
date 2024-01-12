package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.DefaultLong;
import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

public class LongScript2DColumnEntry extends Script2DColumnEntry {

	public final @VerifyNullable Valid valid;
	public static record Valid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultLong(0L) long fallback) implements Script2DColumnEntry.Valid {

		@Override
		public ConstantValue getFallback() {
			return ConstantValue.of(this.fallback);
		}
	}

	public LongScript2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Valid valid, boolean cache) {
		super(value, cache);
		this.valid = valid;
	}

	@Override
	public Script2DColumnEntry.Valid valid() {
		return this.valid;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return new Long2DAccessSchema();
	}

	@RecordLike({})
	public static class Long2DAccessSchema extends Basic2DAccessSchema {

		@Override
		public TypeInfo type() {
			return TypeInfos.LONG;
		}
	}
}