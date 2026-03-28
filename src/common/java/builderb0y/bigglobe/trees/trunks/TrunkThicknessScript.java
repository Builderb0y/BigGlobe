package builderb0y.bigglobe.trees.trunks;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

public interface TrunkThicknessScript extends Script {

	public abstract double getThickness(double height, double fraction, double minThickness);

	@Wrapper
	public static class Holder extends ScriptHolder<TrunkThicknessScript> implements TrunkThicknessScript {

		public Holder(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(TrunkThicknessScript.class, this.usage, registry.parserFlags())
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
					.configureEnvironment((MutableScriptEnvironment environment) -> {
						environment
							.addVariableLoad("height", TypeInfos.DOUBLE)
							.addVariableLoad("fraction", TypeInfos.DOUBLE)
							.addVariableLoad("minThickness", TypeInfos.DOUBLE);
					})
					.parse(new ScriptClassLoader())
			);
		}

		@Override
		public double getThickness(double height, double fraction, double minThickness) {
			try {
				return this.script.getThickness(height, fraction, minThickness);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return minThickness;
			}
		}
	}
}