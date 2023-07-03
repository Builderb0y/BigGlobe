package builderb0y.scripting.parsing;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.versions.AutoCodecVersions;
import builderb0y.scripting.parsing.ScriptTemplate.RequiredInput;

public class ScriptInputs {

	public final ScriptTemplate template;
	public final Map<String, String> providedInputs;

	public ScriptInputs(ScriptTemplate template, Map<String, String> providedInputs) {
		this.template = template;
		this.providedInputs = providedInputs;
	}

	public ScriptInputs(String script) {
		this.template = new ScriptTemplate(script, Collections.emptyList());
		this.providedInputs = Collections.emptyMap();
	}

	public <X extends Throwable> void validateInputs(Function<String, X> exceptionFactory) throws X {
		Set<String> expected = new HashSet<>(this.template.inputs.size());
		for (RequiredInput input : this.template.inputs) {
			if (!expected.add(input.name)) {
				throw exceptionFactory.apply("Duplicate input: " + input.name);
			}
		}
		Set<String> actual = this.providedInputs.keySet();
		if (expected.size() != actual.size() || !actual.containsAll(expected)) {
			throw exceptionFactory.apply("Input mismatch: expected " + expected + ", got " + actual);
		}
	}

	@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER)
	public static class SerializableScriptInputs {

		public final @VerifyNullable @MultiLine String script;
		public final @VerifyNullable RegistryEntry<ScriptTemplate> template;
		public final @VerifyNullable Map<String, @MultiLine String> inputs;

		public SerializableScriptInputs(
			@VerifyNullable @MultiLine String script,
			@VerifyNullable RegistryEntry<ScriptTemplate> template,
			@VerifyNullable Map<String, @MultiLine String> inputs
		) {
			this.script   = script;
			this.template = template;
			this.inputs   = inputs;
		}

		public static <T_Encoded> void verify(VerifyContext<T_Encoded, SerializableScriptInputs> context) throws VerifyException {
			SerializableScriptInputs inputs = context.object;
			if (inputs == null) return;

			if (inputs.script != null) {
				if (inputs.template != null) {
					throw AutoCodecVersions.newVerifyException(() -> context.pathToStringBuilder().append(" cannot specify both script and template at the same time.").toString());
				}
				else if (inputs.inputs != null) {
					throw AutoCodecVersions.newVerifyException(() -> context.pathToStringBuilder().append(" can only specify inputs with template, not script.").toString());
				}
			}
			else if (inputs.template == null) {
				throw AutoCodecVersions.newVerifyException(() -> context.pathToStringBuilder().append(" must specify either script or template.").toString());
			}
			else if (inputs.inputs == null) {
				throw AutoCodecVersions.newVerifyException(() -> context.pathToStringBuilder().append(" must specify inputs when template is specified.").toString());
			}
		}

		public ScriptInputs buildScriptInputs() {
			if (this.script != null) return new ScriptInputs(this.script);
			else return new ScriptInputs(this.template.value(), this.inputs);
		}
	}
}