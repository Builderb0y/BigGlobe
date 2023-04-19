package builderb0y.bigglobe.structures.scripted;

import java.util.List;
import java.util.random.RandomGenerator;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.wrappers.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.BiomeTagKey;
import builderb0y.bigglobe.scripting.wrappers.StructurePlacementScriptEntry;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptInputs.SerializableScriptInputs;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructureLayoutScript extends Script {

	public abstract void layout(int x, int z, RandomGenerator random, WorldColumn column, List<StructurePiece> pieces);

	@Wrapper
	public static class Holder extends ScriptHolder<StructureLayoutScript> implements StructureLayoutScript {

		public final SerializableScriptInputs inputs;

		public Holder(SerializableScriptInputs inputs) throws ScriptParsingException {
			super(
				new TemplateScriptParser<>(StructureLayoutScript.class, inputs.buildScriptInputs())
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(new RandomScriptEnvironment(load("random", 3, type(RandomGenerator.class))))
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.addEnvironment(NbtScriptEnvironment.INSTANCE)
				.addEnvironment(WoodPaletteScriptEnvironment.INSTANCE)
				.addEnvironment(
					new MutableScriptEnvironment()

					.addVariableLoad("x", 1, TypeInfos.INT)
					.addVariableLoad("z", 2, TypeInfos.INT)

					.addType("Biome",                BiomeEntry.TYPE)
					.addType("BiomeTag",             BiomeTagKey.TYPE)
					.addFieldInvokes(BiomeEntry.class, "temperature", "downfall")
					.addFunction(
						"getBiome",
						new FunctionHandler.Named(
							"getBiomeFromColumn(int x, int y, int z)",
							(parser, name, arguments) -> {
								InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, types("III"), CastMode.IMPLICIT_NULL, arguments);
								if (castArguments == null) return null;
								return new CastResult(
									invokeStatic(
										MethodInfo.getMethod(Holder.class, "getBiome"),
										load("column", 4, type(WorldColumn.class)),
										castArguments[0],
										castArguments[1],
										castArguments[2]
									),
									castArguments != arguments
								);
							}
						)
					)
					.addCastConstant(BiomeEntry .CONSTANT_FACTORY, "Biome",    true)
					.addCastConstant(BiomeTagKey.CONSTANT_FACTORY, "BiomeTag", true)

					.addType("ScriptStructurePiece", ScriptedStructure.Piece.class)
					.addQualifiedSpecificConstructor(ScriptedStructure.Piece.class, int.class, int.class, int.class, int.class, int.class, int.class, StructurePlacementScriptEntry.class, NbtCompound.class)
					.addMethodInvoke(ScriptedStructure.Piece.class, "withRotation")
					.addCastConstant(StructurePlacementScriptEntry.CONSTANT_FACTORY, "StructurePlacementScript", true)

					.addVariableLoad("pieces", 5, type(List.class))
				)
				.addEnvironment(
					ColumnScriptEnvironment.createVariableXYZ(
						ColumnValue.REGISTRY,
						load("column", 4, type(WorldColumn.class))
					)
					.mutable
				)
				.parse()
			);
			this.inputs = inputs;
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

		public static BiomeEntry getBiome(WorldColumn column, int x, int y, int z) {
			column.setPos(x, z);
			return new BiomeEntry(column.getBiome(y));
		}
	}
}