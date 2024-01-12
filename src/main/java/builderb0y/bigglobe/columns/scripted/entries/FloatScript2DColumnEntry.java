package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

public class FloatScript2DColumnEntry extends Script2DColumnEntry {

	public FloatScript2DColumnEntry(ScriptUsage<GenericScriptTemplateUsage> value, Valid valid, boolean cache) {
		super(value, valid, cache);
	}

	@Override
	public AccessSchema getAccessSchema() {
		return new Float2DAccessSchema();
	}

	@RecordLike({})
	public static class Float2DAccessSchema extends Basic2DAccessSchema {

		@Override
		public TypeInfo type() {
			return TypeInfos.FLOAT;
		}
	}
}