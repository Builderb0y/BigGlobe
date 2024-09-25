package builderb0y.scripting.parsing.input;

import java.util.List;

import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.VerifyNullable;

public class FileScriptTemplate extends ScriptTemplate {

	public final Identifier file;
	public final transient String source;

	public FileScriptTemplate(Identifier file, List<RequiredInput> inputs, Identifier @VerifyNullable [] includes) {
		super(inputs, includes);
		this.file = file;
		this.source = ScriptFileResolver.resolve(file);
	}

	@Override
	public String getRawSource() {
		return this.source;
	}
}