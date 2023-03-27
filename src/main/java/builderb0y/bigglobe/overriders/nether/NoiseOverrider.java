package builderb0y.bigglobe.overriders.nether;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.NetherColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.overriders.AbstractCaveExclusionContext;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.overriders.overworld.OverworldCaveOverrider;
import builderb0y.bigglobe.scripting.ColumnYScriptEnvironment;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.StructureScriptEnvironment;
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
					new ColumnYScriptEnvironment(
						getField(
							load("context", 1, type(Context.class)),
							FieldInfo.getField(Context.class, "column")
						),
						null,
						true
					)
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

	public static class Environment extends MutableScriptEnvironment {

		public static final Environment INSTANCE = new Environment();

		public Environment() {
			this.addAll(StructureScriptEnvironment.INSTANCE);
			InsnTree loadContext = load("context", 1, type(Context.class));
			this.addVariableGetFields(loadContext, AbstractCaveExclusionContext.class, "structureStarts", "rawGeneration");
			this.addFunctionMultiInvokes(loadContext, OverworldCaveOverrider.Context.class, "excludeSurface");
			this.addFunctionMultiInvokes(loadContext, AbstractCaveExclusionContext.class, "exclude", "excludeCuboid", "excludeCylinder", "excludeSphere");
		}
	}

	public static abstract class Context extends AbstractCaveExclusionContext {

		public final NetherColumn column;

		public Context(NetherColumn column, ScriptStructures structureStarts, boolean rawGeneration, int topI, int bottomI, double[] noise) {
			super(structureStarts, rawGeneration, topI, bottomI, noise);
			this.column = column;
		}

		@Override
		public WorldColumn getColumn() {
			return this.column;
		}
	}

	public static class CaveContext extends Context {

		public final double maxNoise;

		public CaveContext(NetherColumn column, ScriptStructures structureStarts, boolean rawGeneration) {
			super(column, structureStarts, rawGeneration, column.getFinalTopHeightI(), column.getFinalBottomHeightI(), column.caveNoise);
			this.maxNoise = column.getLocalCell().settings.caves().noise().maxValue();
		}

		@Override
		public double getExclusionMultiplier(int y) {
			return this.maxNoise;
		}
	}

	public static class CavernContext extends Context {

		public final double maxNoise;

		public CavernContext(NetherColumn column, ScriptStructures structureStarts, boolean rawGeneration) {
			super(column, structureStarts, rawGeneration, column.getLocalCell().settings.caverns().max_y(), column.getLocalCell().settings.caverns().min_y(), column.cavernNoise);
			Grid3D noise = column.getLocalCell().settings.caverns().noise();
			this.maxNoise = Math.max(-noise.minValue(), noise.maxValue());
		}

		@Override
		public double getExclusionMultiplier(int y) {
			return this.maxNoise;
		}
	}
}