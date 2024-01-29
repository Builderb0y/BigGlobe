package builderb0y.bigglobe.scripting.interfaces;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.util.TypeInfos;

public interface HeightAdjustmentScript extends Script {

	public abstract double evaluate(double baseValue, double seaLevel, double y);

	public static abstract class Holder extends ScriptHolder<HeightAdjustmentScript> implements HeightAdjustmentScript {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
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

	public static class TemperatureHolder extends Holder {

		public TemperatureHolder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(HeightAdjustmentScript.class, usage)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("temperature",                    TypeInfos.DOUBLE)
					.addVariableLoad("overworld/temperature",          TypeInfos.DOUBLE)
					.addVariableLoad("bigglobe:overworld/temperature", TypeInfos.DOUBLE)
					.addVariableLoad("sea_level",                      TypeInfos.DOUBLE)
					.addVariableLoad("overworld/sea_level",            TypeInfos.DOUBLE)
					.addVariableLoad("bigglobe:overworld/sea_level",   TypeInfos.DOUBLE)
					.addVariableLoad("y",                              TypeInfos.DOUBLE)
				)
				.parse()
			);
		}
	}

	public static class FoliageHolder extends Holder {

		public FoliageHolder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(HeightAdjustmentScript.class, usage)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("foliage",                      TypeInfos.DOUBLE)
					.addVariableLoad("overworld/foliage",            TypeInfos.DOUBLE)
					.addVariableLoad("bigglobe:overworld/foliage",   TypeInfos.DOUBLE)
					.addVariableLoad("sea_level",                    TypeInfos.DOUBLE)
					.addVariableLoad("overworld/sea_level",          TypeInfos.DOUBLE)
					.addVariableLoad("bigglobe:overworld/sea_level", TypeInfos.DOUBLE)
					.addVariableLoad("y",                            TypeInfos.DOUBLE)
				)
				.parse()
			);
		}
	}
}