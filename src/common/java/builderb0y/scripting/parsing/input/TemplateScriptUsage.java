package builderb0y.scripting.parsing.input;

import java.util.*;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.input.ScriptTemplate.RequiredInput;

@UseVerifier(name = "verify", in = TemplateScriptUsage.class, usage = MemberUsage.METHOD_IS_HANDLER)
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

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, TemplateScriptUsage> context) throws VerifyException {
		TemplateScriptUsage usage = context.object;
		if (usage == null) return;
		List<RequiredInput> requiredInputs = usage.template.value().inputs;
		Map<String, String> providedInputs = usage.inputs;
		if (
			(requiredInputs != null && !requiredInputs.isEmpty()) ||
			(providedInputs != null && !providedInputs.isEmpty())
		) {
			if (requiredInputs == null) requiredInputs = Collections.emptyList();
			if (providedInputs == null) providedInputs = Collections.emptyMap();
			providedInputs = new HashMap<>(providedInputs);
			Set<String> missingInputs = null;
			for (RequiredInput requiredInput : requiredInputs) {
				if (providedInputs.remove(requiredInput.name()) == null && requiredInput.fallback() == null) {
					if (missingInputs == null) missingInputs = new HashSet<>(4);
					missingInputs.add(requiredInput.name());
				}
			}
			if (missingInputs != null || !providedInputs.isEmpty()) {
				//did I put too much effort into this error message?
				//it's longer than the verification algorithm, so... probably.
				Set<String> missingInputs_ = missingInputs;
				Set<String> unknownInputs = providedInputs.keySet();
				throw new VerifyException(() -> {
					StringBuilder message = new StringBuilder(64);
					if (missingInputs_ != null) {
						if (missingInputs_.size() == 1) {
							message.append("Missing input: ").append(missingInputs_.iterator().next());
						}
						else {
							message.append("Missing inputs: ").append(missingInputs_);
						}
					}
					if (!unknownInputs.isEmpty()) {
						if (!message.isEmpty()) message.append("; ");
						if (unknownInputs.size() == 1) {
							message.append("Unknown input: ").append(unknownInputs.iterator().next());
						}
						else {
							message.append("Unknown inputs: ").append(unknownInputs);
						}
					}
					return message.toString();
				});
			}
		}
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