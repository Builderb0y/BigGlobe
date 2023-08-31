package builderb0y.scripting.parsing;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntSupplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.decoders.AutoDecoder;
import builderb0y.autocodec.decoders.AutoDecoder.NamedDecoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.decoders.RecordDecoder;
import builderb0y.autocodec.encoders.AutoEncoder;
import builderb0y.autocodec.encoders.AutoEncoder.NamedEncoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
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

	@UseCoder(name = "new", in = SerializableScriptInputs.Coder.class, usage = MemberUsage.METHOD_IS_FACTORY)
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

		public static class Coder extends NamedCoder<SerializableScriptInputs> {

			public final AutoCoder<@MultiLine String> stringCoder;
			public final AutoCoder<SerializableScriptInputs> objectCoder;

			public Coder(
				@NotNull ReifiedType<SerializableScriptInputs> handledType,
				AutoCoder<@MultiLine String> stringCoder,
				AutoCoder<SerializableScriptInputs> objectCoder
			) {
				super(handledType);
				this.stringCoder = stringCoder;
				this.objectCoder = objectCoder;
			}

			public Coder(FactoryContext<SerializableScriptInputs> context) {
				super(context.type);
				this.stringCoder = context.type(new ReifiedType<@MultiLine String>() {}).forceCreateCoder();
				this.objectCoder = (AutoCoder<SerializableScriptInputs>)(context.forceCreateDecoder(RecordDecoder.Factory.INSTANCE));
			}

			@Override
			public <T_Encoded> @Nullable SerializableScriptInputs decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
				if (context.isEmpty()) return null;
				if (context.isString() || context.isList()) {
					return new SerializableScriptInputs(context.decodeWith(this.stringCoder), null, null);
				}
				else {
					return context.decodeWith(this.objectCoder);
				}
			}

			@Override
			public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, SerializableScriptInputs> context) throws EncodeException {
				SerializableScriptInputs input = context.input;
				if (input == null) return context.empty();
				if (input.isScript()) {
					return context.input(input.script).encodeWith(this.stringCoder);
				}
				else {
					return context.encodeWith(this.objectCoder);
				}
			}
		}
	}
}