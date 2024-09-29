package builderb0y.scripting.parsing.input;

import java.util.List;

import net.minecraft.util.Identifier;

import builderb0y.scripting.parsing.input.ScriptFileResolver.ResolvedIncludes;

public class FileScriptTemplate extends ScriptTemplate {

	public final Identifier file;
	public final transient String source;

	public FileScriptTemplate(Identifier file, List<RequiredInput> inputs, ResolvedIncludes includes) {
		super(inputs, includes);
		this.file = file;
		this.source = ScriptFileResolver.resolve(file).source();
	}

	@Override
	public String getRawSource() {
		return this.source;
	}
}