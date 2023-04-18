package builderb0y.bigglobe.structures.scripted;

import java.util.List;
import java.util.random.RandomGenerator;

import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.NbtScriptEnvironment;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.WoodPaletteScriptEnvironment;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public interface StructureLayoutScript extends Script {

	public abstract void layout(int x, int z, RandomGenerator random, WorldColumn column, List<StructurePiece> pieces);

	@Wrapper
	public static class Holder extends ScriptHolder<StructureLayoutScript> implements StructureLayoutScript {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(StructureLayoutScript.class, script)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.addEnvironment(NbtScriptEnvironment.INSTANCE)
				.addEnvironment(WoodPaletteScriptEnvironment.INSTANCE)
				//todo: add constructor for scripted feature piece.
				.parse()
			);
		}

		@Override
		public void layout(int x, int z, RandomGenerator random, WorldColumn column, List<StructurePiece> pieces) {
			try {
				this.script.layout(x, z, random, column, pieces);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}