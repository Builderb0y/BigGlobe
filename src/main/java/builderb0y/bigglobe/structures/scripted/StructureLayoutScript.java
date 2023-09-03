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
import builderb0y.bigglobe.scripting.wrappers.StructurePlacementScriptEntry;
import builderb0y.bigglobe.util.CheckedList;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructureLayoutScript extends Script {

	public abstract void layout(
		int x,
		int z,
		RandomGenerator random,
		WorldColumn column,
		List<StructurePiece> pieces,
		boolean distantHorizons
	);

	@Wrapper
	public static class Holder extends ScriptHolder<StructureLayoutScript> implements StructureLayoutScript {

		public static final InsnTree LOAD_RANDOM = load("random", 3, type(RandomGenerator.class));

		public final ScriptUsage<GenericScriptTemplateUsage> usage;

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				new TemplateScriptParser<>(StructureLayoutScript.class, usage)
				.addEnvironment(JavaUtilScriptEnvironment.withRandom(LOAD_RANDOM))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(RandomScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.addEnvironment(NbtScriptEnvironment.INSTANCE)
				.addEnvironment(WoodPaletteScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(MinecraftScriptEnvironment.createWithRandom(LOAD_RANDOM))
				.addEnvironment(SymmetryScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(
					new MutableScriptEnvironment()

					.addVariableLoad("originX", 1, TypeInfos.INT)
					.addVariableLoad("originZ", 2, TypeInfos.INT)

					.addVariable(
						"worldSeed",
						getField(
							load("column", 4, type(WorldColumn.class)),
							ColumnScriptEnvironmentBuilder.SEED
						)
					)

					.addFunction(
						"getBiome",
						new FunctionHandler.Named(
							"getBiomeFromColumn(int x, int y, int z)",
							Handlers
							.builder(Holder.class, "getBiome")
							.addImplicitArgument(load("column", 4, type(WorldColumn.class)))
							.addArguments("III")
							.buildFunction()
						)
					)

					.addQualifiedSpecificConstructor(ScriptedStructure.Piece.class, int.class, int.class, int.class, int.class, int.class, int.class, StructurePlacementScriptEntry.class, NbtCompound.class)
					.addMethodInvokes(ScriptedStructure.Piece.class, "withRotation", "rotateAround", "symmetrify", "symmetrifyAround", "offset")
					.addMethod(type(ScriptedStructure.Piece.class), "rotateRandomly", Handlers.builder(ScriptedStructure.Piece.class, "rotateRandomly").addReceiverArgument(ScriptedStructure.Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
					.addMethod(type(ScriptedStructure.Piece.class), "rotateAndFlipRandomly", Handlers.builder(ScriptedStructure.Piece.class, "rotateAndFlipRandomly").addReceiverArgument(ScriptedStructure.Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
					.addFieldGet(ScriptedStructure.Piece.class, "data")
					.addType("ScriptStructurePlacement", StructurePlacementScriptEntry.class)

					.addVariableLoad("pieces", 5, type(CheckedList.class))

					.addVariableLoad("distantHorizons", 6, TypeInfos.BOOLEAN)
				)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createVariableXYZ(
						ColumnValue.REGISTRY,
						load("column", 4, type(WorldColumn.class))
					)
					.build()
				)
				.parse()
			);
			this.usage = usage;
		}

		@Override
		public void layout(
			int x,
			int z,
			RandomGenerator random,
			WorldColumn column,
			List<StructurePiece> pieces,
			boolean distantHorizons
		) {
			try {
				this.script.layout(x, z, random, column, pieces, distantHorizons);
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