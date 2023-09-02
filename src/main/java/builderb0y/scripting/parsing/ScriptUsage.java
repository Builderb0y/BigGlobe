package builderb0y.scripting.parsing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.scripting.parsing.ScriptTemplate.ScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage.ScriptUsageCoder;

@UseCoder(name = "new", in = ScriptUsageCoder.class, usage = MemberUsage.METHOD_IS_FACTORY)
public class ScriptUsage<T extends ScriptTemplateUsage> {

	public final @VerifyNullable @MultiLine String script;
	public final @VerifyNullable T template;

	public ScriptUsage(@NotNull String script) {
		this.script = script;
		this.template = null;
	}

	public ScriptUsage(@NotNull T template) {
		this.script = null;
		this.template = template;
	}

	public String findSource() {
		return this.script != null ? this.script : this.template.getEntry().value().getSource();
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
		public final AutoCoder<T> templateCoder;

		@SuppressWarnings("unchecked")
		public ScriptUsageCoder(FactoryContext<ScriptUsage<T>> context) {
			super(context.type);
			this.scriptCoder = context.type(new ReifiedType<@MultiLine String>() {}).forceCreateCoder();
			this.templateCoder = context.type((ReifiedType<T>)(context.type.getParameters()[0])).forceCreateCoder();
		}

		@Override
		public <T_Encoded> @Nullable ScriptUsage<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;
			else if (context.isString() || context.isList()) return new ScriptUsage<>(context.decodeWith(this.scriptCoder));
			else return new ScriptUsage<>(context.decodeWith(this.templateCoder));
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, ScriptUsage<T>> context) throws EncodeException {
			ScriptUsage<T> object = context.input;
			if (object == null) return context.empty();
			if (object.script != null) return context.input(object.script).encodeWith(this.scriptCoder);
			else return context.input(object.template).encodeWith(this.templateCoder);
		}
	}
}