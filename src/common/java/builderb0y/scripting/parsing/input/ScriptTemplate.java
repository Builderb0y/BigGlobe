package builderb0y.scripting.parsing.input;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.TypelessCoderRegistry;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.input.ScriptFileResolver.ResolvedIncludes;

@UseVerifier(name = "verify", in = ScriptTemplate.class, usage = MemberUsage.METHOD_IS_HANDLER)
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

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, ScriptTemplate> context) throws VerifyException {
		ScriptTemplate template = context.object;
		if (template == null || template.inputs == null || template.inputs.isEmpty()) return;
		Set<String> names = new ObjectOpenHashSet<>(template.inputs.size());
		for (RequiredInput input : template.inputs) {
			if (!names.add(input.name())) {
				throw new VerifyException(() -> "Duplicate input name: " + input.name());
			}
		}
	}

	public abstract String getRawSource();

	public String getSource() {
		return this.includes != null ? this.includes.assemble(this.getRawSource()) : this.getRawSource();
	}

	public @Nullable List<RequiredInput> getInputs() {
		return this.inputs;
	}

	public static record RequiredInput(
		@IdentifierName String name,
		String type,
		@UseName("default") @MultiLine @VerifyNullable String fallback
	) {}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return this.includes != null ? this.includes.streamDirectDependencies() : Stream.empty();
	}
}