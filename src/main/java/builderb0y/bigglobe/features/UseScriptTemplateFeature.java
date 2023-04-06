package builderb0y.bigglobe.features;

import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;

import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.ScriptedFeature.FeatureScript;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.scripting.parsing.ScriptParsingException;

public class UseScriptTemplateFeature extends Feature<UseScriptTemplateFeature.Config> {

	public UseScriptTemplateFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public UseScriptTemplateFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		Config config = context.getConfig();
		FeatureScript.Holder script;
		try {
			script = config.getCompiledScript();
		}
		catch (WrongFeatureTypeException exception) {
			long time = System.currentTimeMillis();
			if (time >= config.nextWarning) {
				ScriptLogger.LOGGER.error(exception.getMessage());
				config.nextWarning = time + 5000L;
			}
			return false;
		}
		catch (ScriptParsingException exception) {
			long time = System.currentTimeMillis();
			if (time >= config.nextWarning) {
				ScriptLogger.LOGGER.error(
					"Script template failed to compile:\n" +
					"Script source was:\n" + (
						ScriptLogger.addLineNumbers(
							config.getScriptSource()
						)
					) +
					"\nInputs were:\n" + (
						config
						.inputs
						.entrySet()
						.stream()
						.map(entry -> '\t' + entry.getKey() + ": " + entry.getValue())
						.collect(Collectors.joining("\n"))
					),
					exception
				);
				config.nextWarning = time + 5000L;
			}
			return false;
		}
		return ScriptedFeature.generate(context, script);
	}

	public static class Config implements FeatureConfig {

		public final RegistryEntry<ConfiguredFeature<?, ?>> script;
		public final Map<String, String> inputs;
		public transient FeatureScript.Holder compiledScript;
		public transient long nextWarning = Long.MIN_VALUE;
		public transient ScriptParsingException compileError;

		public Config(RegistryEntry<ConfiguredFeature<?, ?>> script, Map<String, String> inputs) {
			this.script = script;
			this.inputs = inputs;
		}

		public String getScriptSource() {
			FeatureConfig config = this.script.value().config();
			if (config instanceof DefineScriptTemplateFeature.Config c) {
				return c.script();
			}
			else {
				throw new WrongFeatureTypeException("script should point to feature of type bigglobe:define_script_template, but was " + Registries.FEATURE.getId(this.script.value().feature()));
			}
		}

		public String[] getScriptInputs() {
			FeatureConfig config = this.script.value().config();
			if (config instanceof DefineScriptTemplateFeature.Config c) {
				return c.inputs();
			}
			else {
				throw new WrongFeatureTypeException("script should point to feature of type bigglobe:define_script_template, but was " + Registries.FEATURE.getId(this.script.value().feature()));
			}
		}

		public FeatureScript.Holder getCompiledScript() throws ScriptParsingException {
			if (this.compiledScript == null) {
				if (this.compileError != null) {
					throw new ScriptParsingException(this.compileError);
				}
				try {
					this.compiledScript = new ScriptedFeature.FeatureScript.Holder(this.getScriptSource(), this.inputs, this.getScriptInputs());
				}
				catch (ScriptParsingException exception) {
					this.compileError = exception;
					throw exception;
				}
			}
			return this.compiledScript;
		}
	}

	public static class WrongFeatureTypeException extends RuntimeException {

		public WrongFeatureTypeException() {}

		public WrongFeatureTypeException(String message) {
			super(message);
		}

		public WrongFeatureTypeException(String message, Throwable cause) {
			super(message, cause);
		}

		public WrongFeatureTypeException(Throwable cause) {
			super(cause);
		}
	}
}