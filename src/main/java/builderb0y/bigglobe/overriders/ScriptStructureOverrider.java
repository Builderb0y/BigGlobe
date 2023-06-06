package builderb0y.bigglobe.overriders;

import java.util.random.RandomGenerator;

import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ScriptStructureOverrider extends Script {

	public abstract boolean override(StructureStartWrapper start, WorldColumn column, RandomGenerator random);

	@SuppressWarnings("deprecation")
	public static void move(StructureStartWrapper start, int yOffset) {
		start.box().move(0, yOffset, 0);
		start.start().getBoundingBox().move(0, yOffset, 0);
		for (StructurePiece piece : start.pieces()) {
			piece.translate(0, yOffset, 0);
		}
	}

	@Wrapper
	public static class Holder extends ScriptHolder<ScriptStructureOverrider> implements ScriptStructureOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(ScriptStructureOverrider.class, script)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("start", 1, StructureStartWrapper.TYPE)
					.addMethodInvokeStatic(ScriptStructureOverrider.class, "move")
				)
				.addEnvironment(
					ColumnScriptEnvironment.createVariableXYZ(
						ColumnValue.REGISTRY,
						load("column", 2, type(WorldColumn.class))
					)
					.mutable
				)
				.addEnvironment(RandomScriptEnvironment.create(
					load("random", 3, type(RandomGenerator.class))
				))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.parse()
			);
		}

		@Override
		public boolean override(StructureStartWrapper start, WorldColumn column, RandomGenerator random) {
			try {
				return this.script.override(start, column, random);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return true;
			}
		}
	}
}