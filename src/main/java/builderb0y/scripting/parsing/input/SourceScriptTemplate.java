package builderb0y.scripting.parsing.input;

import java.util.List;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseName;

public class SourceScriptTemplate extends ScriptTemplate {

	public final @MultiLine @UseName("script") String source;

	public SourceScriptTemplate(@MultiLine @UseName("script") String source, List<RequiredInput> inputs) {
		super(inputs);
		this.source = source;
	}

	@Override
	public String getSource() {
		return this.source;
	}
}