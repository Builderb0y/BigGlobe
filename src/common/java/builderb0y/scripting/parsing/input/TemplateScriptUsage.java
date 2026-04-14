package builderb0y.scripting.parsing.input;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

public class TemplateScriptUsage extends ScriptUsage {

	public final Holder<ScriptTemplate> template;
	public final @VerifyNullable Map<@IdentifierName String, @MultiLine String> inputs;

	public TemplateScriptUsage(
		@VerifyNullable String debug_name,
		Holder<ScriptTemplate> template,
		@VerifyNullable Map<@IdentifierName String, @MultiLine String> inputs
	) {
		super(debug_name, null);
		this.template = template;
		this.inputs = inputs;
	}

	@Override
	public String getRawSource() {
		return this.template.value().getSource();
	}

	@Override
	public @Nullable Holder<ScriptTemplate> getTemplate() {
		return this.template;
	}

	@Override
	public @Nullable Map<@IdentifierName String, @MultiLine String> getInputs() {
		return this.inputs;
	}
}