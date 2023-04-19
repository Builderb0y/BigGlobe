package builderb0y.bigglobe.overriders.overworld;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.OverworldColumn.CaveCell;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.overriders.AbstractCaveExclusionContext;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironment;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface OverworldCaveOverrider extends Script {

	public abstract void override(Context context);

	@Wrapper
	public static class Holder extends ScriptHolder<OverworldCaveOverrider> implements OverworldCaveOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(OverworldCaveOverrider.class, script)
				.addEnvironment(OverworldCaveOverrider.Environment.INSTANCE)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(
					ColumnScriptEnvironment.createFixedXZVariableY(
						ColumnValue.REGISTRY,
						getField(
							load("context", 1, type(Context.class)),
							FieldInfo.getField(Context.class, "column")
						),
						null
					)
					.addXZ("x", "z")
					.mutable
				)
				.parse()
			);
		}

		@Override
		public void override(Context context) {
			try {
				this.script.override(context);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}

	public static class Environment extends OverworldDataOverrider.Environment {

		public static final Environment INSTANCE = new Environment();

		public Environment() {
			super();

			InsnTree loadContext = load("context", 1, type(Context.class));
			InsnTree loadColumn = InsnTrees.getField(loadContext, FieldInfo.getField(Context.class, "column"));
			this.addVariableGetFields(loadContext, AbstractCaveExclusionContext.class, "structureStarts", "rawGeneration");
			this.addDistanceFunctions(loadColumn);
			this.addFunctionMultiInvokes(loadContext, Context.class, "excludeSurface");
			this.addFunctionMultiInvokes(loadContext, AbstractCaveExclusionContext.class, "exclude", "excludeCuboid", "excludeCylinder", "excludeSphere");
		}
	}

	public static class Context extends AbstractCaveExclusionContext {

		public final OverworldColumn column;
		public final CaveCell caveCell;
		public final LocalOverworldCaveSettings caveSettings;
		public final double topD, bottomD;
		public final double ledgeMin;

		public Context(ScriptStructures structureStarts, OverworldColumn column, boolean rawGeneration) {
			super(structureStarts, rawGeneration, column.getFinalTopHeightI(), column.getFinalTopHeightI() - column.getCaveCell().settings.depth(), column.caveNoise);
			this.column          = column;
			this.caveCell        = column.getCaveCell();
			this.caveSettings    = this.caveCell.settings;
			this.topD            = column.getFinalTopHeightD();
			this.bottomD         = this.topD - this.caveSettings.depth();
			this.ledgeMin        = this.caveSettings.ledge_noise() != null ? this.caveSettings.ledge_noise().minValue() : 0.0D;
		}

		@Override
		public WorldColumn getColumn() {
			return this.column;
		}

		@Override
		public double getExclusionMultiplier(int y) {
			double width = this.caveSettings.getWidthSquared(this.column, y);
			width -= this.ledgeMin * width;
			return width;
		}

		//////////////////////////////// script methods ////////////////////////////////

		public void excludeSurface(double multiplier) {
			if (!(multiplier > 0.0D)) return;
			double baseY = this.topD;
			double width = this.caveSettings.getWidth(this.column, baseY);
			double intersection = baseY - width * 2.0D;
			multiplier /= width;
			int minY = Math.max(BigGlobeMath.ceilI(intersection), this.bottomI);
			int maxY = this.topI;
			for (int y = minY; y < maxY; y++) {
				this.excludeUnchecked(y, BigGlobeMath.squareD((y - intersection) * multiplier));
			}
		}
	}
}