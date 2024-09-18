package builderb0y.scripting.parsing;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.coders.RecordCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptUsage.ScriptTemplate.RequiredInput;
import builderb0y.scripting.parsing.ScriptUsage.ScriptUsageCoder;

@UseCoder(name = "new", in = ScriptUsageCoder.class, usage = MemberUsage.METHOD_IS_FACTORY)
@UseVerifier(name = "verify", in = ScriptUsage.class, usage = MemberUsage.METHOD_IS_HANDLER)
public class ScriptUsage {

	public final @VerifyNullable @MultiLine String source;
	public final @VerifyNullable RegistryEntry<ScriptTemplate> template;
	public final @VerifyNullable Map<@IdentifierName String, @MultiLine String> inputs;
	public final @VerifyNullable @IdentifierName String debug_name;

	@Hidden
	public ScriptUsage(@NotNull @MultiLine String source) {
		this.source     = source;
		this.template   = null;
		this.inputs     = null;
		this.debug_name = null;
	}

	public ScriptUsage(
		@VerifyNullable String source,
		@VerifyNullable RegistryEntry<ScriptTemplate> template,
		@VerifyNullable Map<@IdentifierName String, @MultiLine String> inputs,
		@VerifyNullable String debug_name
	) {
		this.source     = source;
		this.template   = template;
		this.inputs     = inputs;
		this.debug_name = debug_name;
	}

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, ScriptUsage> context) throws VerifyException {
		ScriptUsage usage = context.object;
		if (usage == null) return;
		if (usage.source != null) {
			if (usage.template != null || usage.inputs != null) {
				throw new VerifyException(() -> "Must specify EITHER source OR template and inputs, but not both.");
			}
		}
		else {
			if (usage.template == null || usage.inputs == null) {
				throw new VerifyException(() -> "Must specify EITHER source OR template and inputs, but not both.");
			}
			List<RequiredInput> requiredInputs = usage.template.value().inputs();
			Set<String> expected = new HashSet<>(requiredInputs.size());
			for (RequiredInput requiredInput : requiredInputs) {
				if (!expected.add(requiredInput.name())) {
					throw new VerifyException(() -> "Duplicate input: " + requiredInput.name());
				}
			}
			Set<String> actual = usage.inputs.keySet();
			if (!expected.equals(actual)) {
				throw new VerifyException(() -> "Input mismatch: Expected " + expected + ", got " + actual);
			}
		}
	}

	public String findSource() {
		return this.isScript() ? this.getScript() : this.getTemplate().value().source();
	}

	public boolean isScript() {
		return this.source != null;
	}

	public boolean isTemplate() {
		return this.template != null;
	}

	public String getScript() {
		if (this.source != null) return this.source;
		else throw new IllegalStateException("Not a script");
	}

	public RegistryEntry<ScriptTemplate> getTemplate() {
		if (this.template != null) return this.template;
		else throw new IllegalStateException("Not a template");
	}

	public Map<String, String> getInputs() {
		if (this.inputs != null) return this.inputs;
		else throw new IllegalStateException("Not a template");
	}

	@UseVerifier(name = "verify", in = ScriptTemplate.class, usage = MemberUsage.METHOD_IS_HANDLER)
	public static record ScriptTemplate(@MultiLine @UseName("script") String source, List<RequiredInput> inputs) implements SimpleDependencyView {

		public static final AutoCoder<ScriptTemplate> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(ScriptTemplate.class);

		public record RequiredInput(@IdentifierName String name, String type) {}

		public static <T_Encoded> void verify(VerifyContext<T_Encoded, ScriptTemplate> context) throws VerifyException {
			ScriptTemplate template = context.object;
			if (template == null) return;
			Set<String> seen = new HashSet<>(template.inputs.size() << 1);
			for (RequiredInput input : template.inputs) {
				if (!seen.add(input.name)) {
					throw new VerifyException(() -> "Duplicate input name: " + input.name);
				}
			}
		}

		@Override
		public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
			return Stream.empty();
		}
	}

	public static class ScriptUsageCoder extends NamedCoder<ScriptUsage> {

		public final AutoCoder<@MultiLine String> stringArrayCoder;
		public final AutoCoder<ScriptUsage> objectCoder;

		public ScriptUsageCoder(FactoryContext<ScriptUsage> context) {
			super(context.type);
			this.stringArrayCoder = context.type(new ReifiedType<@MultiLine String>() {}).forceCreateCoder();
			this.objectCoder = context.forceCreateCoder(RecordCoder.Factory.INSTANCE);
		}

		@Override
		public <T_Encoded> @Nullable ScriptUsage decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) {
				return null;
			}
			else if (context.isString() || context.isList()) {
				return new ScriptUsage(context.decodeWith(this.stringArrayCoder));
			}
			else if (context.isMap()) {
				return context.decodeWith(this.objectCoder);
			}
			else {
				throw context.notA("string, list, or map");
			}
		}

		@OverrideOnly
		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, ScriptUsage> context) throws EncodeException {
			ScriptUsage usage = context.object;
			if (usage == null) return context.empty();
			if (usage.source != null && usage.debug_name == null) {
				return context.object(usage.source).encodeWith(this.stringArrayCoder);
			}
			else {
				return context.encodeWith(this.objectCoder);
			}
		}
	}
}