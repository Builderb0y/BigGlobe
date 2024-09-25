package builderb0y.scripting.parsing.input;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.VerifyNullable;

public class FileScriptUsage extends ScriptUsage {

	public final Identifier file;
	public final transient String source;

	public FileScriptUsage(@VerifyNullable String debug_name, Identifier @VerifyNullable [] includes, Identifier file) {
		super(debug_name, includes);
		this.file = file;
		this.source = ScriptFileResolver.resolve(file);
	}

	@Override
	public String getRawSource() {
		return this.source;
	}

	@Override
	public @Nullable Identifier getFile() {
		return this.file;
	}
}