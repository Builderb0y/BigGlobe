package builderb0y.bigglobe.structures.scripted;

import org.objectweb.asm.Opcodes;

import net.minecraft.nbt.NbtCompound;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptInputs.SerializableScriptInputs;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructurePlacementScript extends Script {

	public abstract void place(
		WorldWrapper world,
		WorldColumn column,
		int minX, int minY, int minZ,
		int maxX, int maxY, int maxZ,
		int midX, int midY, int midZ,
		NbtCompound data
	);

	public default void place(
		WorldWrapper world,
		WorldColumn column,
		int minX, int minY, int minZ,
		int maxX, int maxY, int maxZ,
		NbtCompound data
	) {
		this.place(
			world,
			column,
			minX, minY, minZ,
			maxX, maxY, maxZ,
			(minX + maxX) >> 1,
			(minY + maxY) >> 1,
			(minZ + maxZ) >> 1,
			data
		);
	}

	@Wrapper
	public static class Holder extends ScriptHolder<StructurePlacementScript> implements StructurePlacementScript {

		public final SerializableScriptInputs inputs;

		public Holder(SerializableScriptInputs inputs) throws ScriptParsingException {
			super(
				new TemplateScriptParser<>(StructurePlacementScript.class, inputs.buildScriptInputs())
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(new MinecraftScriptEnvironment(
					load("world", 1, WorldWrapper.TYPE)
				))
				.addEnvironment(NbtScriptEnvironment.INSTANCE)
				.addEnvironment(WoodPaletteScriptEnvironment.INSTANCE)
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("minX",  3, TypeInfos.INT)
					.addVariableLoad("minY",  4, TypeInfos.INT)
					.addVariableLoad("minZ",  5, TypeInfos.INT)
					.addVariableLoad("maxX",  6, TypeInfos.INT)
					.addVariableLoad("maxY",  7, TypeInfos.INT)
					.addVariableLoad("maxZ",  8, TypeInfos.INT)
					.addVariableLoad("midX",  9, TypeInfos.INT)
					.addVariableLoad("midY", 10, TypeInfos.INT)
					.addVariableLoad("midZ", 11, TypeInfos.INT)
					.addVariableLoad("data", 12, NbtScriptEnvironment.NBT_COMPOUND_TYPE)
				)
				.addEnvironment(new RandomScriptEnvironment(
					getField(
						load("world", 1, WorldWrapper.TYPE),
						field(
							Opcodes.ACC_PUBLIC,
							WorldWrapper.TYPE,
							"permuter",
							type(Permuter.class)
						)
					)
				))
				.addEnvironment(
					ColumnScriptEnvironment.createVariableXYZ(
						ColumnValue.REGISTRY,
						load("column", 2, type(WorldColumn.class))
					)
					.mutable
				)
				.parse()
			);
			this.inputs = inputs;
		}

		@Override
		public void place(
			WorldWrapper world,
			WorldColumn column,
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			int midX, int midY, int midZ,
			NbtCompound data
		) {
			try {
				this.script.place(
					world,
					column,
					minX, minY, minZ,
					maxX, maxY, maxZ,
					midX, midY, midZ,
					data
				);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}