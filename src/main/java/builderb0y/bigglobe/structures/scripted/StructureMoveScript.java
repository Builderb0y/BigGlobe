package builderb0y.bigglobe.structures.scripted;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.scripting.NbtScriptEnvironment;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructurePieceWrapper;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptInputs.SerializableScriptInputs;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.TypeInfos;

public interface StructureMoveScript extends Script {

	public abstract void move(
		int deltaX,
		int deltaY,
		int deltaZ,
		StructureStartWrapper start,
		StructurePiece piece,
		NbtCompound data
	);

	@Wrapper
	public static class Holder extends ScriptHolder<StructureMoveScript> implements StructureMoveScript {

		public Holder(SerializableScriptInputs inputs) throws ScriptParsingException {
			super(
				new TemplateScriptParser<>(StructureMoveScript.class, inputs.buildScriptInputs())
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(NbtScriptEnvironment.INSTANCE)
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("deltaX", 1, TypeInfos.INT)
					.addVariableLoad("deltaY", 2, TypeInfos.INT)
					.addVariableLoad("deltaZ", 3, TypeInfos.INT)
					.addVariableLoad("start", 4, StructureStartWrapper.TYPE)
					.addVariableLoad("piece", 5, StructurePieceWrapper.TYPE)
					.addVariableLoad("data", 6, NbtScriptEnvironment.NBT_COMPOUND_TYPE)
				)
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.parse()
			);
		}

		@Override
		public void move(
			int deltaX,
			int deltaY,
			int deltaZ,
			StructureStartWrapper start,
			StructurePiece piece,
			NbtCompound data
		) {
			try {
				this.script.move(deltaX, deltaY, deltaZ, start, piece, data);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}