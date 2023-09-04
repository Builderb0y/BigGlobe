package builderb0y.bigglobe.overriders.end;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.overriders.VolumetricOverrider;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

public interface EndVolumetricOverrider extends VolumetricOverrider {

	public abstract void override(EndVolumetricOverrider.Context context);

	@Override
	public default void override(VolumetricOverrider.Context context) {
		this.override((EndVolumetricOverrider.Context)(context));
	}

	public static Context lowerRingCloudContext(ScriptStructures structures, EndColumn column) {
		return new SimpleContext(
			structures,
			column,
			column.getLowerRingCloudSampleStartY(),
			column.getLowerRingCloudNoise(),
			-column.settings.ring_clouds.noise().maxValue()
		);
	}

	public static Context upperRingCloudContext(ScriptStructures structures, EndColumn column) {
		return new SimpleContext(
			structures,
			column,
			column.getUpperRingCloudSampleStartY(),
			column.getUpperRingCloudNoise(),
			-column.settings.ring_clouds.noise().maxValue()
		);
	}

	public static Context lowerBridgeCloudContext(ScriptStructures structures, EndColumn column) {
		return new SimpleContext(
			structures,
			column,
			column.getLowerBridgeCloudSampleStartY(),
			column.getLowerBridgeCloudNoise(),
			-column.settings.bridge_clouds.noise().maxValue()
		);
	}

	public static Context upperBridgeCloudContext(ScriptStructures structures, EndColumn column) {
		return new SimpleContext(
			structures,
			column,
			column.getUpperBridgeCloudSampleStartY(),
			column.getUpperBridgeCloudNoise(),
			-column.settings.bridge_clouds.noise().maxValue()
		);
	}

	public static abstract class Context extends VolumetricOverrider.Context {

		public Context(ScriptStructures structureStarts, EndColumn column, int minY, double[] noise) {
			super(structureStarts, column, minY, noise);
		}

		public EndColumn column() {
			return (EndColumn)(this.column);
		}
	}

	public static class SimpleContext extends Context {

		public final double exclusionMultiplier;

		public SimpleContext(ScriptStructures structureStarts, EndColumn column, int minY, double[] noise, double multiplier) {
			super(structureStarts, column, minY, noise);
			this.exclusionMultiplier = multiplier;
		}

		@Override
		public double getExclusionMultiplier(int y) {
			return this.exclusionMultiplier;
		}
	}

	@Wrapper
	public static class Holder extends VolumetricOverrider.Holder<EndVolumetricOverrider> implements EndVolumetricOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				usage,
				new TemplateScriptParser<>(EndVolumetricOverrider.class, usage),
				EndVolumetricOverrider.Context.class
			);
		}

		@Override
		public void override(EndVolumetricOverrider.Context context) {
			try {
				this.script.override(context);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}