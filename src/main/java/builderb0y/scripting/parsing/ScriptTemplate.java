package builderb0y.scripting.parsing;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseName;

public class ScriptTemplate {

	public final @UseName("script") @MultiLine String source;
	public final List<RequiredInput> inputs;

	public ScriptTemplate(String source, List<RequiredInput> inputs) {
		this.source = source;
		this.inputs = inputs;
	}

	@Override
	public String toString() {
		return (
			"ScriptTemplate:\n"
			+ "source:\n"
			+ this.source + '\n'
			+ "Inputs:\n"
			+ (
				this
				.inputs
				.stream()
				.map(Objects::toString)
				.collect(Collectors.joining("\n"))
			)
		);
	}

	public static class RequiredInput {

		public final String name, type;

		public RequiredInput(String name, String type) {
			this.name = name;
			this.type = type;
		}

		@Override
		public String toString() {
			return "ScriptTemplate$RequiredInput: { name: " + this.name + ", type: " + this.type + " }";
		}
	}
}