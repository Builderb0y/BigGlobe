package builderb0y.bigglobe.structures.scripted;

import java.util.random.RandomGenerator;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.StructurePlacementScriptEntry;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.Piece;
import builderb0y.bigglobe.util.CheckedList;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructureLayoutScript extends Script {

	public abstract void layout(
		ScriptedColumnLookup lookup,
		int originX,
		int originZ,
		RandomGenerator random,
		CheckedList<StructurePiece> pieces,
		boolean distantHorizons
	);

	@Wrapper
	public static class Holder extends ScriptHolder<StructureLayoutScript> implements StructureLayoutScript {

		public static final InsnTree LOAD_RANDOM = load("random", type(RandomGenerator.class));

		public Holder(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
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
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					registry.setupExternalEnvironment(
						environment
						.addVariableLoad("originX", TypeInfos.INT)
						.addVariableLoad("originZ", TypeInfos.INT)
						.addQualifiedSpecificConstructor(Piece.class, int.class, int.class, int.class, int.class, int.class, int.class, StructurePlacementScriptEntry.class, NbtCompound.class)
						.addMethodInvokes(Piece.class, "withRotation", "rotateAround", "symmetrify", "symmetrifyAround", "offset")
						.addMethod(type(Piece.class), "rotateRandomly", Handlers.builder(Piece.class, "rotateRandomly").addReceiverArgument(Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
						.addMethod(type(Piece.class), "rotateAndFlipRandomly", Handlers.builder(Piece.class, "rotateAndFlipRandomly").addReceiverArgument(Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
						.addType("ScriptStructurePlacement", StructurePlacementScriptEntry.class)
						.addVariableLoad("pieces", type(CheckedList.class))
						.addVariableLoad("distantHorizons", TypeInfos.BOOLEAN),
						new ExternalEnvironmentParams()
						.withLookup(load("lookup", type(ScriptedColumnLookup.class)))
						.withX(load("originX", TypeInfos.INT))
						.withZ(load("originZ", TypeInfos.INT))
					);
				})
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public void layout(
			ScriptedColumnLookup lookup,
			int originX,
			int originZ,
			RandomGenerator random,
			CheckedList<StructurePiece> pieces,
			boolean distantHorizons
		) {
			try {
				this.script.layout(lookup, originX, originZ, random, pieces, distantHorizons);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}