package builderb0y.scripting.parsing;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.noise.ScriptedGridTemplate;

@UseCoder(name = "REGISTRY", in = ScriptTemplate.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface ScriptTemplate extends CoderRegistryTyped<ScriptTemplate> {

	public static final CoderRegistry<ScriptTemplate> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("script_templates"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("generic"), GenericScriptTemplate.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("grid"), ScriptedGridTemplate.class);
	}};

	public abstract String getSource();

	public abstract List<RequiredInput> getRequiredInputs();

	public record RequiredInput(String name, String type) {}

	public static interface ScriptTemplateUsage {

		public abstract RegistryEntry<ScriptTemplate> getEntry();

		public abstract Map<String, String> getProvidedInputs();

		public default <X extends Throwable> void validateInputs(Function<Supplier<String>, X> exceptionFactory) throws X {
			List<RequiredInput> requiredInputs = this.getEntry().value().getRequiredInputs();
			Set<String> expected = new HashSet<>(requiredInputs.size());
			for (RequiredInput requiredInput : requiredInputs) {
				if (!expected.add(requiredInput.name())) {
					throw exceptionFactory.apply(() -> "Duplicate input: " + requiredInput.name());
				}
			}
			Set<String> actual = this.getProvidedInputs().keySet();
			if (!expected.equals(actual)) {
				throw exceptionFactory.apply(() -> "Input mismatch: Expected " + expected + ", got " + actual);
			}
		}
	}
}