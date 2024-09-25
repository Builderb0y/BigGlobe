package builderb0y.scripting.parsing.input;

import java.util.List;

import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;

public class SourceScriptTemplate extends ScriptTemplate {

	public final @MultiLine @UseName("script") String source;

	public SourceScriptTemplate(@MultiLine @UseName("script") String source, List<RequiredInput> inputs, Identifier @VerifyNullable [] includes) {
		super(inputs, includes);
		this.source = source;
	}

	@Override
	public String getRawSource() {
		return this.source;
	}
}