package builderb0y.scripting.parsing.input;

import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.util.FakeRegistry;
import builderb0y.scripting.parsing.input.ScriptFileResolver.ResolvedIncludes;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

public class FileScriptUsage extends ScriptUsage {

	public static final FakeRegistry<FileScriptUsage> REGISTRY = new FakeRegistry<>(BigGlobeMod.modID("file_script_usage"));

	public final Identifier file;
	public final transient String source;

	public FileScriptUsage(@VerifyNullable String debug_name, ResolvedIncludes includes, Identifier file) {
		super(debug_name, includes);
		this.file = file;
		this.source = ScriptFileResolver.resolve(file).source();
	}

	@Override
	public String getRawSource() {
		return this.source;
	}

	@Override
	public @Nullable Identifier getFile() {
		return this.file;
	}

	public Holder<FileScriptUsage> toEntry() {
		return REGISTRY.getOrCreate(this.file, this);
	}
}