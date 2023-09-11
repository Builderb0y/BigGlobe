package builderb0y.scripting.parsing;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.scripting.parsing.ScriptTemplate.ScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage.ScriptUsageCoder;

@UseCoder(name = "new", in = ScriptUsageCoder.class, usage = MemberUsage.METHOD_IS_FACTORY)
public class ScriptUsage<T extends ScriptTemplateUsage> {

	public final @VerifyNullable @MultiLine String script;
	public final @VerifyNullable T template;
	public final @VerifyNullable String debug_name;

	public ScriptUsage(@NotNull String script, @VerifyNullable String debug_name) {
		this.script = script;
		this.template = null;
		this.debug_name = debug_name;
	}

	public ScriptUsage(@NotNull T template, @VerifyNullable String debug_name) {
		this.script = null;
		this.template = template;
		this.debug_name = debug_name;
	}

	public static <T_Encoded> void verifyDebugName(VerifyContext<T_Encoded, String> context) throws VerifyException {
		String name = context.object;
		if (name != null) {
			for (int index = 0, length = name.length(); index < length; index++) {
				char c = name.charAt(index);
				if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_')) continue;
				else throw new VerifyException(() -> "Illegal character in debug name: " + c);
			}
		}
	}

	public String findSource() {
		return this.isScript() ? this.getScript() : this.getTemplate().getEntry().value().getSource();
	}

	public boolean isScript() {
		return this.script != null;
	}

	public boolean isTemplate() {
		return this.template != null;
	}

	public String getScript() {
		if (this.script != null) return this.script;
		else throw new IllegalStateException("Not a script");
	}

	public T getTemplate() {
		if (this.template != null) return this.template;
		else throw new IllegalStateException("Not a template");
	}

	public static class ScriptUsageCoder<T extends ScriptTemplateUsage> extends NamedCoder<ScriptUsage<T>> {

		public final AutoCoder<@MultiLine String> scriptCoder;
		public final AutoCoder<@MultiLine @VerifyNullable String> sourceCoder;
		public final AutoCoder<String> debugNameCoder;
		public final AutoCoder<T> templateCoder;

		public ScriptUsageCoder(FactoryContext<ScriptUsage<T>> context) {
			super(context.type);
			this.scriptCoder = context.type(new ReifiedType<@MultiLine String>() {}).forceCreateCoder();
			this.sourceCoder = context.type(new ReifiedType<@MultiLine @VerifyNullable String>() {}).forceCreateCoder();
			this.templateCoder = context.type(context.type.getParameters()[0].<T>uncheckedCast()).forceCreateCoder();
			this.debugNameCoder = context.type(new ReifiedType<@VerifyNullable @UseVerifier(name = "verifyDebugName", in = ScriptUsage.class, usage = MemberUsage.METHOD_IS_HANDLER) String>() {}).forceCreateCoder();
		}

		@Override
		public <T_Encoded> @Nullable ScriptUsage<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) {
				return null;
			}
			else if (context.isString() || context.isList()) {
				return new ScriptUsage<>(context.decodeWith(this.scriptCoder), null);
			}
			else if (context.isMap()) {
				String debugName = context.getMember("debug_name").decodeWith(this.debugNameCoder);
				String source = context.getMember("source").decodeWith(this.sourceCoder);
				if (source != null) {
					return new ScriptUsage<>(source, debugName);
				}
				else {
					return new ScriptUsage<>(context.decodeWith(this.templateCoder), debugName);
				}
			}
			else {
				throw context.notA("string, list, or map");
			}
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, ScriptUsage<T>> context) throws EncodeException {
			ScriptUsage<T> object = context.input;
			if (object == null) return context.empty();
			if (object.isTemplate()) {
				if (object.debug_name != null) {
					return context.addToStringMap(
						context.input(object.template).encodeWith(this.templateCoder),
						"debug_name",
						context.input(object.debug_name).encodeWith(this.debugNameCoder)
					);
				}
				else {
					return context.input(object.template).encodeWith(this.templateCoder);
				}
			}
			else {
				if (object.debug_name != null) {
					return context.createStringMap(
						Map.of(
							"source", context.input(object.script).encodeWith(this.sourceCoder),
							"debug_name", context.input(object.debug_name).encodeWith(this.debugNameCoder)
						)
					);
				}
				else {
					return context.input(object.script).encodeWith(this.scriptCoder);
				}
			}
		}
	}
}