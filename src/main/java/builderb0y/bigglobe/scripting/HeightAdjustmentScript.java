package builderb0y.bigglobe.scripting;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.util.TypeInfos;

public interface HeightAdjustmentScript extends Script {

	public abstract double evaluate(double baseValue, double seaLevel, double y);

	public static class Holder extends ScriptHolder<HeightAdjustmentScript> implements HeightAdjustmentScript {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, HeightAdjustmentScript script) {
			super(usage, script);
		}

		@Override
		public double evaluate(double baseValue, double seaLevel, double y) {
			try {
				return this.script.evaluate(baseValue, seaLevel, y);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return 0.0D;
			}
		}
	}

	@Wrapper
	public static class TemperatureHolder extends Holder {

		public TemperatureHolder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				usage,
				new TemplateScriptParser<>(HeightAdjustmentScript.class, usage)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("temperature", 1, TypeInfos.DOUBLE)
					.addVariableLoad("overworld/temperature", 1, TypeInfos.DOUBLE)
					.addVariableLoad("bigglobe:overworld/temperature", 1, TypeInfos.DOUBLE)
					.addVariableLoad("sea_level", 3, TypeInfos.DOUBLE)
					.addVariableLoad("overworld/sea_level", 3, TypeInfos.DOUBLE)
					.addVariableLoad("bigglobe:overworld/sea_level", 3, TypeInfos.DOUBLE)
					.addVariableLoad("y", 5, TypeInfos.DOUBLE)
				)
				.parse()
			);
		}
	}

	@Wrapper
	public static class FoliageHolder extends Holder {

		public FoliageHolder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				usage,
				new TemplateScriptParser<>(HeightAdjustmentScript.class, usage)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("foliage", 1, TypeInfos.DOUBLE)
					.addVariableLoad("overworld/foliage", 1, TypeInfos.DOUBLE)
					.addVariableLoad("bigglobe:overworld/foliage", 1, TypeInfos.DOUBLE)
					.addVariableLoad("sea_level", 3, TypeInfos.DOUBLE)
					.addVariableLoad("overworld/sea_level", 3, TypeInfos.DOUBLE)
					.addVariableLoad("bigglobe:overworld/sea_level", 3, TypeInfos.DOUBLE)
					.addVariableLoad("y", 5, TypeInfos.DOUBLE)
				)
				.parse()
			);
		}
	}
}