package builderb0y.bigglobe.noise;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

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
		public final @DefaultEmpty Map<String, String> inputs;

		public ScriptedGridTemplateUsage(
			RegistryEntry<ScriptTemplate> template,
			Map<String, String> inputs
		) {
			if (template.value() instanceof ScriptedGridTemplate actualTemplate) {
				this.actualTemplate = actualTemplate;
			}
			else {
				throw new IllegalStateException("Referenced template must be of type bigglobe:grid");
			}
			this.template = template;
			this.inputs = inputs;
		}

		@Override
		public RegistryEntry<ScriptTemplate> getEntry() {
			return this.template;
		}

		@Override
		public Map<String, String> getProvidedInputs() {
			return this.inputs;
		}

		public <X extends Throwable> void validateInputs(Map<String, G> providedGridInputs, Function<Supplier<String>, X> exceptionFactory) throws X {
			ScriptTemplateUsage.super.validateInputs(exceptionFactory);
			List<GridInput> gridInputs = this.actualTemplate.grid_inputs;
			Set<String> expected = new HashSet<>(gridInputs.size());
			for (GridInput gridInput : gridInputs) {
				if (!expected.add(gridInput.name())) {
					throw exceptionFactory.apply(() -> "Duplicate input: " + gridInput.name());
				}
			}
			Set<String> actual = providedGridInputs.keySet();
			if (!expected.equals(actual)) {
				throw exceptionFactory.apply(() -> "Input mismatch: Expected " + expected + ", got " + actual);
			}
		}
	}
}