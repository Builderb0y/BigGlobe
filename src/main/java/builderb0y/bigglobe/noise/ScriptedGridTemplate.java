package builderb0y.bigglobe.noise;

import java.util.List;
import java.util.Map;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.scripting.parsing.ScriptTemplate;

public class ScriptedGridTemplate implements ScriptTemplate {

	public final @MultiLine String script;
	public final @DefaultEmpty List<RequiredInput> script_inputs;
	public final @DefaultEmpty List<GridInput> grid_inputs;

	public ScriptedGridTemplate(
		@MultiLine String script,
		@DefaultEmpty List<RequiredInput> script_inputs,
		@DefaultEmpty List<GridInput> grid_inputs
	) {
		this.script = script;
		this.script_inputs = script_inputs;
		this.grid_inputs = grid_inputs;
	}

	@Override
	public String getSource() {
		return this.script;
	}

	@Override
	public List<RequiredInput> getRequiredInputs() {
		return this.script_inputs;
	}

	public static record GridInput(String name) {}

	public static class ScriptedGridTemplateUsage<G extends Grid> implements ScriptTemplateUsage {

		public final RegistryEntry<ScriptTemplate> template;
		public final transient ScriptedGridTemplate actualTemplate;
		public final Map<String, String> script_inputs;
		public final Map<String, G> grid_inputs;

		public ScriptedGridTemplateUsage(
			RegistryEntry<ScriptTemplate> template,
			Map<String, String> script_inputs,
			Map<String, G> grid_inputs
		) {
			if (template.value() instanceof ScriptedGridTemplate actualTemplate) {
				this.actualTemplate = actualTemplate;
			}
			else {
				throw new IllegalStateException("Referenced template must be of type bigglobe:grid");
			}
			this.template = template;
			this.script_inputs = script_inputs;
			this.grid_inputs = grid_inputs;
		}

		@Override
		public RegistryEntry<ScriptTemplate> getEntry() {
			return this.template;
		}

		@Override
		public Map<String, String> getProvidedInputs() {
			return this.script_inputs;
		}
	}
}