package builderb0y.scripting.parsing;

import java.util.List;
import java.util.Map;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

public class GenericScriptTemplate implements ScriptTemplate {

	public final @MultiLine String script;
	public final @DefaultEmpty List<RequiredInput> inputs;

	public GenericScriptTemplate(@MultiLine String script, @DefaultEmpty List<RequiredInput> inputs) {
		this.script = script;
		this.inputs = inputs;
	}

	@Override
	public String getSource() {
		return this.script;
	}

	@Override
	public List<RequiredInput> getRequiredInputs() {
		return this.inputs;
	}

	public static class GenericScriptTemplateUsage implements ScriptTemplateUsage {

		public final RegistryEntry<ScriptTemplate> template;
		public final transient GenericScriptTemplate actualTemplate;
		public final @DefaultEmpty Map<@IdentifierName String, @MultiLine String> inputs;

		public GenericScriptTemplateUsage(RegistryEntry<ScriptTemplate> template, Map<String, String> inputs) {
			if (template.value() instanceof GenericScriptTemplate actualTemplate) {
				this.actualTemplate = actualTemplate;
			}
			else {
				throw new IllegalStateException("Referenced template must be of type bigglobe:generic");
			}
			this.template = template;
			this.inputs = inputs;
		}

		@Override
		public RegistryEntry<ScriptTemplate> getEntry() {
			return this.template;
		}

		@Override
		public ScriptTemplate getActualTemplate() {
			return this.actualTemplate;
		}

		@Override
		public Map<String, @MultiLine String> getProvidedInputs() {
			return this.inputs;
		}
	}
}