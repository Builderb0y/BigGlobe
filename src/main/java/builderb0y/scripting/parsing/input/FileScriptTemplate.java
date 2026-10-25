package builderb0y.scripting.parsing.input;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import builderb0y.scripting.parsing.input.ScriptFileResolver.ResolvedIncludes;

public class FileScriptTemplate extends ScriptTemplate {

	public final ResourceLocation file;
	public final transient String source;

	public FileScriptTemplate(ResourceLocation file, List<RequiredInput> inputs, ResolvedIncludes includes) {
		super(inputs, includes);
		this.file = file;
		this.source = ScriptFileResolver.resolve(file).source();
	}

	@Override
	public String getRawSource() {
		return this.source;
	}
}