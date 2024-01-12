package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

public class DoubleScript2DColumnEntry extends Script2DColumnEntry {

	public DoubleScript2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Valid valid, boolean cache) {
		super(value, valid, cache);
	}

	@Override
	public AccessSchema getAccessSchema() {
		return new Double2DAccessSchema();
	}

	@RecordLike({})
	public static class Double2DAccessSchema extends Basic2DAccessSchema {

		@Override
		public TypeInfo type() {
			return TypeInfos.DOUBLE;
		}
	}
}