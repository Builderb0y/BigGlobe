package builderb0y.scripting.parsing;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.util.registry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.util.UnregisteredObjectException;
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

	@Override
	public int hashCode() {
		return this.template.hashCode() * 31 + this.providedInputs.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof ScriptInputs that &&
			this.template.equals(that.template) &&
			this.providedInputs.equals(that.providedInputs)
		);
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
					throw new VerifyException(() -> context.pathToStringBuilder().append(" cannot specify both script and template at the same time.").toString());
				}
				else if (inputs.inputs != null) {
					throw new VerifyException(() -> context.pathToStringBuilder().append(" can only specify inputs with template, not script.").toString());
				}
			}
			else if (inputs.template == null) {
				throw new VerifyException(() -> context.pathToStringBuilder().append(" must specify either script or template.").toString());
			}
			else if (inputs.inputs == null) {
				throw new VerifyException(() -> context.pathToStringBuilder().append(" must specify inputs when template is specified.").toString());
			}
		}

		public boolean isScript() {
			return this.script != null;
		}

		public boolean isTemplate() {
			return this.script == null;
		}

		public ScriptInputs buildScriptInputs() {
			return (
				this.isScript()
				? new ScriptInputs(this.script)
				: new ScriptInputs(this.template.value(), this.inputs)
			);
		}

		@Override
		public int hashCode() {
			return (
				this.isScript()
				? this.script.hashCode()
				: UnregisteredObjectException.getKey(this.template).getValue().hashCode() * 31 + this.inputs.hashCode()
			);
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || (
				obj instanceof SerializableScriptInputs that
				&& this.isScript() == that.isScript()
				&& (
					this.isScript()
					? this.script.equals(that.script)
					: (
						UnregisteredObjectException.getID(this.template).equals(UnregisteredObjectException.getID(that.template)) &&
						this.inputs.equals(that.inputs)
					)
				)
			);
		}
	}
}