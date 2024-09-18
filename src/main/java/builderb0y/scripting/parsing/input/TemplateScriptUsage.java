package builderb0y.scripting.parsing.input;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.input.ScriptTemplate.RequiredInput;

@UseVerifier(name = "verify", in = TemplateScriptUsage.class, usage = MemberUsage.METHOD_IS_HANDLER)
public class TemplateScriptUsage extends ScriptUsage {

	public static final AutoCoder<Map<String, String>> INPUTS_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(new ReifiedType<@VerifyNullable Map<@IdentifierName String, @MultiLine String>>() {});

	public final RegistryEntry<ScriptTemplate> template;
	public final @VerifyNullable Map<@IdentifierName String, @MultiLine String> inputs;

	public TemplateScriptUsage(
		@VerifyNullable String debug_name,
		RegistryEntry<ScriptTemplate> template,
		@VerifyNullable Map<@IdentifierName String, @MultiLine String> inputs
	) {
		super(debug_name);
		this.template = template;
		this.inputs = inputs;
	}

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, TemplateScriptUsage> context) throws VerifyException {
		TemplateScriptUsage usage = context.object;
		if (usage == null) return;
		List < RequiredInput > requiredInputs = usage.template.value().inputs;
		Set<String> expected = requiredInputs == null || requiredInputs.isEmpty() ? Collections.emptySet() : requiredInputs.stream().map(RequiredInput::name).collect(Collectors.toSet());
		Set<String> actual = usage.inputs == null || usage.inputs.isEmpty() ? Collections.emptySet() : usage.inputs.keySet();
		if (!expected.equals(actual)) {
			throw new VerifyException(() -> "Input mismatch: expected " + expected + ", got " + actual);
		}
	}

	@Override
	public String getSource() {
		return this.template.value().getSource();
	}

	@Override
	public @Nullable RegistryEntry<ScriptTemplate> getTemplate() {
		return this.template;
	}

	@Override
	public @Nullable Map<@IdentifierName String, @MultiLine String> getInputs() {
		return this.inputs;
	}
}