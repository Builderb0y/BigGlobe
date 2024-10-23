package builderb0y.bigglobe.structures.scripted;

import java.util.random.RandomGenerator;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.ExternalData;
import builderb0y.bigglobe.scripting.wrappers.ExternalImage;
import builderb0y.bigglobe.scripting.wrappers.ExternalImage.ColorScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructurePlacementScriptEntry;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.Piece;
import builderb0y.bigglobe.util.CheckedList;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructureLayoutScript extends Script {

	public abstract void layout(
		ScriptedColumnLookup lookup,
		int originX,
		int originZ,
		long seed,
		RandomGenerator random,
		CheckedList<StructurePiece> pieces,
		Hints hints
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
				.configureEnvironment(JavaUtilScriptEnvironment.withRandom(LOAD_RANDOM))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.configureEnvironment(RandomScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.createWithSeed(load("seed", TypeInfos.LONG)))
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.configureEnvironment(NbtScriptEnvironment.createMutable())
				.configureEnvironment(WoodPaletteScriptEnvironment.create(LOAD_RANDOM))
				.configureEnvironment(MinecraftScriptEnvironment.createWithRandom(LOAD_RANDOM))
				.configureEnvironment(SymmetryScriptEnvironment.create(LOAD_RANDOM))
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					registry.setupExternalEnvironment(
						environment
						.addFieldGet(ScriptedStructure.Piece.class, "data")
						.addVariableLoad("originX", TypeInfos.INT)
						.addVariableLoad("originZ", TypeInfos.INT)
						.addQualifiedSpecificConstructor(Piece.class, int.class, int.class, int.class, int.class, int.class, int.class, StructurePlacementScriptEntry.class, NbtCompound.class)
						.addMethodInvokes(Piece.class, "withRotation", "rotateAround", "symmetrify", "symmetrifyAround", "offset")
						.addMethod(type(Piece.class), "rotateRandomly", Handlers.builder(Piece.class, "rotateRandomly").addReceiverArgument(Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
						.addMethod(type(Piece.class), "rotateAndFlipRandomly", Handlers.builder(Piece.class, "rotateAndFlipRandomly").addReceiverArgument(Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
						.addType("ScriptStructurePlacement", StructurePlacementScriptEntry.class)
						.addVariableLoad("pieces", type(CheckedList.class))
						.addVariableLoad("hints", type(Hints.class))
						.configure(ScriptedColumn.hintsEnvironment())
						.addVariableRenamedInvoke(load("hints", type(Hints.class)), "distantHorizons", MethodInfo.getMethod(Hints.class, "isLod")),

						new ExternalEnvironmentParams()
						.withLookup(load("lookup", type(ScriptedColumnLookup.class)))
						.withXZ(
							load("originX", TypeInfos.INT),
							load("originZ", TypeInfos.INT)
						)
					);
				})
				.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
				.addEnvironment(ExternalImage.ENVIRONMENT)
				.addEnvironment(ExternalData.ENVIRONMENT)
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public void layout(
			ScriptedColumnLookup lookup,
			int originX,
			int originZ,
			long seed,
			RandomGenerator random,
			CheckedList<StructurePiece> pieces,
			Hints hints
		) {
			NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				this.script.layout(lookup, originX, originZ, seed, random, pieces, hints);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
			finally {
				manager.used = used;
			}
		}
	}
}