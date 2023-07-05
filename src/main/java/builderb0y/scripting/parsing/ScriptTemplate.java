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

	@Override
	public int hashCode() {
		return this.source.hashCode() * 31 + this.inputs.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof ScriptTemplate that &&
			this.source.equals(that.source) &&
			this.inputs.equals(that.inputs)
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

		@Override
		public int hashCode() {
			return this.name.hashCode() * 31 + this.type.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof RequiredInput that &&
				this.name.equals(that.name) &&
				this.type.equals(that.type)
			);
		}
	}
}