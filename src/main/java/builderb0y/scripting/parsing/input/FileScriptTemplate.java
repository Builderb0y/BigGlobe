package builderb0y.scripting.parsing.input;

import java.util.List;

import net.minecraft.util.Identifier;

public class FileScriptTemplate extends ScriptTemplate {

	public final Identifier file;
	public final transient String source;

	public FileScriptTemplate(Identifier file, List<RequiredInput> inputs) {
		super(inputs);
		this.file = file;
		this.source = ScriptFileResolver.resolve(file);
	}

	@Override
	public String getSource() {
		return this.source;
	}
}