package builderb0y.scripting.parsing.input;

import builderb0y.autocodec.annotations.Hidden;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.VerifyNullable;

public class SourceScriptUsage extends ScriptUsage {

	public final @MultiLine String source;

	public SourceScriptUsage(@VerifyNullable String debug_name, @MultiLine String source) {
		super(debug_name);
		this.source = source;
	}

	@Hidden
	public SourceScriptUsage(@MultiLine String source) {
		super(null);
		this.source = source;
	}

	@Override
	public String getSource() {
		return this.source;
	}
}