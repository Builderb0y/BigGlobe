package builderb0y.bigglobe.overriders.overworld;

import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.OverworldColumn.CaveCell;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.overriders.VolumetricOverrider;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface OverworldVolumetricOverrider extends VolumetricOverrider {

	public static final MutableScriptEnvironment EXCLUDE_SURFACE_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addFunctionInvoke(load("context", type(Context.class)), Context.class, "excludeSurface")
	);

	public abstract void override(Context context);

	@Override
	@Deprecated
	public default void override(VolumetricOverrider.Context context) {
		this.override((Context)(context));
	}

	public static Context caveContext(ScriptStructures structures, OverworldColumn column) {
		ColumnYToDoubleScript.Holder noiseThreshold = column.getCaveCell().settings.noise_threshold;
		double noiseMin = column.getCaveCell().settings.noise.minValue();
		return new Context(
			structures,
			column,
			column.getFinalTopHeightI() - column.getCaveCell().settings.depth,
			column.getCaveNoise()
		) {

			@Override
			public double getExclusionMultiplier(int y) {
				return noiseThreshold.evaluate(this.column(), y) - noiseMin;
			}
		};
	}

	public static abstract class Context extends VolumetricOverrider.Context {

		public final CaveCell caveCell;
		public final double minYD, maxYD;

		public Context(ScriptStructures structureStarts, OverworldColumn column, int minY, NumberArray noise) {
			super(structureStarts, column, minY, noise);
			this.caveCell = column.getCaveCell();
			this.maxYD = column.getFinalTopHeightD();
			this.minYD = this.maxYD - noise.length();
		}

		public OverworldColumn column() {
			return (OverworldColumn)(this.column);
		}

		public void excludeSurface(double multiplier) {
			if (!(multiplier > 0.0D)) return;
			double baseY = this.maxYD;
			double width = this.caveCell.settings.getEffectiveWidth(this.column(), baseY);
			double intersection = baseY - width * 2.0D;
			multiplier /= width;
			int minY = Math.max(BigGlobeMath.ceilI(intersection), this.minY);
			int maxY = this.maxY;
			for (int y = minY; y < maxY; y++) {
				this.excludeUnchecked(y, BigGlobeMath.squareD((y - intersection) * multiplier));
			}
		}
	}

	public static class Holder extends VolumetricOverrider.Holder<OverworldVolumetricOverrider> implements OverworldVolumetricOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, BetterRegistry.Lookup betterRegistryLookup) {
			super(usage, betterRegistryLookup);
		}

		@Override
		public Class<? extends VolumetricOverrider.Context> getContextClass() {
			return OverworldVolumetricOverrider.Context.class;
		}

		@Override
		public Class<OverworldVolumetricOverrider> getScriptClass() {
			return OverworldVolumetricOverrider.class;
		}

		@Override
		public MutableScriptEnvironment setupEnvironment(MutableScriptEnvironment environment) {
			return super.setupEnvironment(environment).addAll(EXCLUDE_SURFACE_ENVIRONMENT);
		}

		@Override
		public void override(OverworldVolumetricOverrider.Context context) {
			try {
				this.script.override(context);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}