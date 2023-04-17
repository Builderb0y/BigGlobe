package builderb0y.scripting.parsing;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class ScriptTemplate {

	public static final AutoCoder<ScriptTemplate> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(ScriptTemplate.class);
	public static final     Codec<ScriptTemplate> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(CODER);

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