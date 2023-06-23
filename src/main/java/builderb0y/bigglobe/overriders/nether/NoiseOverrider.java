package builderb0y.bigglobe.overriders.nether;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.NetherColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.overriders.AbstractCaveExclusionContext;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.overriders.overworld.OverworldCaveOverrider;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.StructureScriptEnvironment;
import builderb0y.bigglobe.settings.NetherSettings.NetherCavernSettings;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface NoiseOverrider extends Script {

	public abstract void override(Context context);

	@Wrapper
	public static class Holder extends ScriptHolder<NoiseOverrider> implements NoiseOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(NoiseOverrider.class, script)
				.addEnvironment(Environment.INSTANCE)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createFixedXZVariableY(
						ColumnValue.REGISTRY,
						getField(
							load("context", 1, type(Context.class)),
							FieldInfo.getField(Context.class, "column")
						),
						null
					)
					.addXZ("x", "z")
					.build()
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

	public static class Environment {

		public static final MutableScriptEnvironment INSTANCE = new MutableScriptEnvironment();

		static {
			InsnTree loadContext = load("context", 1, type(Context.class));
			INSTANCE
			.addAll(StructureScriptEnvironment.INSTANCE)
			.addVariableGetField(loadContext, AbstractCaveExclusionContext.class, "structureStarts")
			.addFunctionMultiInvokes(loadContext, OverworldCaveOverrider.Context.class, "excludeSurface")
			.addFunctionMultiInvokes(loadContext, AbstractCaveExclusionContext.class, "exclude", "excludeCuboid", "excludeCylinder", "excludeSphere")
			;
		}
	}

	public static abstract class Context extends AbstractCaveExclusionContext {

		public final NetherColumn column;

		public Context(NetherColumn column, ScriptStructures structureStarts, int topI, int bottomI, double[] noise) {
			super(structureStarts, topI, bottomI, noise);
			this.column = column;
		}

		public static Context caves(NetherColumn column, ScriptStructures structureStarts) {
			return new CaveContext(
				column,
				structureStarts,
				column.getFinalTopHeightI(),
				column.getFinalBottomHeightI(),
				column.caveNoise,
				column.getLocalCell().settings.caves().noise_threshold()
			);
		}

		public static Context caverns(NetherColumn column, ScriptStructures structureStarts) {
			NetherCavernSettings caverns = column.getLocalCell().settings.caverns();
			Grid3D noise = caverns.noise();
			return new CavernContext(
				column,
				structureStarts,
				caverns.max_y(),
				caverns.min_y(),
				column.cavernNoise,
				Math.max(-noise.minValue(), noise.maxValue())
			);
		}

		@Override
		public WorldColumn getColumn() {
			return this.column;
		}
	}

	public static class CaveContext extends Context {

		public final ColumnYToDoubleScript.Holder width;

		public CaveContext(NetherColumn column, ScriptStructures structureStarts, int topI, int bottomI, double[] noise, ColumnYToDoubleScript.Holder width) {
			super(column, structureStarts, topI, bottomI, noise);
			this.width = width;
		}

		@Override
		public double getExclusionMultiplier(int y) {
			return this.width.evaluate(this.column, y);
		}
	}

	public static class CavernContext extends Context {

		public final double maxNoise;

		public CavernContext(NetherColumn column, ScriptStructures structureStarts, int topI, int bottomI, double[] noise, double maxNoise) {
			super(column, structureStarts, topI, bottomI, noise);
			this.maxNoise = maxNoise;
		}

		@Override
		public double getExclusionMultiplier(int y) {
			return this.maxNoise;
		}
	}
}