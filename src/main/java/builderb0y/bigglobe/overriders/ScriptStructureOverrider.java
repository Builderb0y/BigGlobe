package builderb0y.bigglobe.overriders;

import java.util.random.RandomGenerator;

import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ScriptStructureOverrider extends Overrider {

	public static final MutableScriptEnvironment START_MOVE_DH_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariableLoad("start", 1, StructureStartWrapper.TYPE)
		.addMethodInvokeStatic(ScriptStructureOverrider.class, "move")
		.addVariableLoad("distantHorizons", 4, TypeInfos.BOOLEAN)
	);

	public abstract boolean override(StructureStartWrapper start, WorldColumn column, RandomGenerator random, boolean distantHorizons);

	@SuppressWarnings("deprecation")
	public static void move(StructureStartWrapper start, int yOffset) {
		start.box().move(0, yOffset, 0);
		start.start().getBoundingBox().move(0, yOffset, 0);
		for (StructurePiece piece : start.pieces()) {
			piece.translate(0, yOffset, 0);
		}
	}

	@Wrapper
	public static class Holder extends Overrider.Holder<ScriptStructureOverrider> implements ScriptStructureOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(ScriptStructureOverrider.class, script)
				.addEnvironment(START_MOVE_DH_ENVIRONMENT)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createVariableXYZ(
						ColumnValue.REGISTRY,
						load("column", 2, type(WorldColumn.class))
					)
					.build()
				)
				.addEnvironment(RandomScriptEnvironment.create(
					load("random", 3, type(RandomGenerator.class))
				))
				.addEnvironment(JavaUtilScriptEnvironment.randomOnly(
					load("random", 3, type(RandomGenerator.class))
				))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
			);
		}

		@Override
		public boolean override(StructureStartWrapper start, WorldColumn column, RandomGenerator random, boolean distantHorizons) {
			try {
				return this.script.override(start, column, random, distantHorizons);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return true;
			}
		}
	}
}