package builderb0y.bigglobe.overriders.overworld;

import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.OverworldColumn.CaveCell;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.overriders.VolumetricOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface OverworldVolumetricOverrider extends VolumetricOverrider {

	public static final MutableScriptEnvironment EXCLUDE_SURFACE_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addFunctionInvoke(load("context", 1, type(Context.class)), Context.class, "excludeSurface")
	);

	public abstract void override(Context context);

	@Override
	@Deprecated
	public default void override(VolumetricOverrider.Context context) {
		this.override((Context)(context));
	}

	public static abstract class Context extends VolumetricOverrider.Context {

		public final CaveCell caveCell;
		public final double minYD, maxYD;

		public Context(ScriptStructures structureStarts, OverworldColumn column, int minY, double[] noise) {
			super(structureStarts, column, minY, noise);
			this.caveCell = column.getCaveCell();
			this.maxYD = column.getFinalTopHeightD();
			this.minYD = this.maxYD - noise.length;
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

	public static class Holder<T_Overrider extends OverworldVolumetricOverrider> extends VolumetricOverrider.Holder<T_Overrider> implements OverworldVolumetricOverrider {

		public Holder(ScriptParser<T_Overrider> parser) throws ScriptParsingException {
			super(
				parser.addEnvironment(EXCLUDE_SURFACE_ENVIRONMENT),
				OverworldVolumetricOverrider.Context.class
			);
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