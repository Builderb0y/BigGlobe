package builderb0y.bigglobe.overriders.nether;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.NetherColumn;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.overriders.VolumetricOverrider;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.settings.NetherSettings.NetherCavernSettings;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public interface NetherVolumetricOverrider extends VolumetricOverrider {

	public abstract void override(NetherVolumetricOverrider.Context context);

	@Override
	public default void override(VolumetricOverrider.Context context) {
		this.override((NetherVolumetricOverrider.Context)(context));
	}

	public static Context caveContext(ScriptStructures structures, NetherColumn column) {
		ColumnYToDoubleScript.Holder noiseThreshold = column.getLocalCell().settings.caves().noise_threshold();
		return new Context(structures, column, column.getFinalBottomHeightI(), column.getCaveNoise()) {

			@Override
			public double getExclusionMultiplier(int y) {
				return noiseThreshold.evaluate(this.column, y);
			}
		};
	}

	public static Context cavernContext(ScriptStructures structures, NetherColumn column) {
		NetherCavernSettings caverns = column.getLocalCell().settings.caverns();
		Grid3D noise = caverns.noise();
		double exclusionMultiplier = Math.max(-noise.minValue(), noise.maxValue());
		return new Context(structures, column, caverns.min_y(), column.getCavernNoise()) {

			@Override
			public double getExclusionMultiplier(int y) {
				return exclusionMultiplier;
			}
		};
	}

	public static abstract class Context extends VolumetricOverrider.Context {

		public final NetherColumn.LocalCell localCell;

		public Context(ScriptStructures structureStarts, NetherColumn column, int minY, double[] noise) {
			super(structureStarts, column, minY, noise);
			this.localCell = column.getLocalCell();
		}

		public NetherColumn column() {
			return (NetherColumn)(this.column);
		}
	}

	@Wrapper
	public static class Holder extends VolumetricOverrider.Holder<NetherVolumetricOverrider> implements NetherVolumetricOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(NetherVolumetricOverrider.class, script),
				NetherVolumetricOverrider.Context.class
			);
		}

		@Override
		public void override(NetherVolumetricOverrider.Context context) {
			try {
				this.script.override(context);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}