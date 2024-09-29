package builderb0y.scripting.parsing.input;

import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.TypelessCoderRegistry;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.input.ScriptFileResolver.ResolvedIncludes;

@UseCoder(name = "CODER", in = ScriptTemplate.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public abstract class ScriptTemplate implements SimpleDependencyView {

	public static final TypelessCoderRegistry<ScriptTemplate> CODER = new TypelessCoderRegistry<>(ReifiedType.from(ScriptTemplate.class), BigGlobeAutoCodec.AUTO_CODEC);
	static {
		CODER.register(SourceScriptTemplate.class);
		CODER.register(FileScriptTemplate.class);
	}

	public final @VerifyNullable List<RequiredInput> inputs;
	public final @VerifyNullable ResolvedIncludes includes;

	public ScriptTemplate(@Nullable List<RequiredInput> inputs, @VerifyNullable ResolvedIncludes includes) {
		this.inputs = inputs;
		this.includes = includes;
	}

	public abstract String getRawSource();

	public String getSource() {
		return this.includes != null ? this.includes.assemble(this.getRawSource()) : this.getRawSource();
	}

	public @Nullable List<RequiredInput> getInputs() {
		return this.inputs;
	}

	public static record RequiredInput(@IdentifierName String name, String type) {}

	@Override
	public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
		return this.includes != null ? this.includes.streamDirectDependencies() : Stream.empty();
	}
}