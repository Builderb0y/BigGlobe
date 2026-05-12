package builderb0y.bigglobe.structures.scripted;

import java.util.random.RandomGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.entries.StructurePlacementScriptEntry;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.Piece;
import builderb0y.bigglobe.util.CheckedList;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructureLayoutScript extends Script {

	public abstract void layout(
		ScriptedColumnLookup columns,
		int originX,
		int originZ,
		long worldSeed,
		RandomGenerator random,
		CheckedList<StructurePiece> pieces
	);

	@Deprecated
	public static boolean distantHorizons(Hints hints) {
		return hints.isLod();
	}

	@Wrapper
	public static class Catcher extends ScriptCatcher<StructureLayoutScript> implements StructureLayoutScript {

		public static final InsnTree LOAD_RANDOM = load("random", type(RandomGenerator.class));

		public Catcher(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(StructureLayoutScript.class, this.usage, registry.parserFlags())
				.configureEnvironment(JavaUtilScriptEnvironment.withRandom(LOAD_RANDOM))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.configureEnvironment(RandomScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.createWithSeed(load("worldSeed", TypeInfos.LONG)))
				.configureEnvironment(StructureScriptEnvironment.live())
				.configureEnvironment(NbtScriptEnvironment.createMutable())
				.configureEnvironment(WoodPaletteScriptEnvironment.create(LOAD_RANDOM))
				.configureEnvironment(MinecraftScriptEnvironment.createWithRandom(LOAD_RANDOM))
				.configureEnvironment(SymmetryScriptEnvironment.create(LOAD_RANDOM))
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					LoadInsnTree loadLookup = load("columns", type(ScriptedColumnLookup.class));
					registry.setupExternalEnvironment(
						environment
						.addVariableLoad("worldSeed", TypeInfos.LONG)
						.addFieldGet(ScriptedStructure.Piece.class, "data")
						.addVariableLoad("originX", TypeInfos.INT)
						.addVariableLoad("originZ", TypeInfos.INT)
						.addQualifiedSpecificConstructor(Piece.class, int.class, int.class, int.class, int.class, int.class, int.class, StructurePlacementScriptEntry.class, CompoundTag.class)
						.addMethodInvokes(Piece.class, "withRotation", "rotateAround", "symmetrify", "symmetrifyAround", "offset")
						.addMethodMultiInvokes(Piece.class, "rotateRandomly", "rotateAndFlipRandomly")
						.addMethod(type(Piece.class), "rotateRandomly", Handlers.builder(Piece.class, "rotateRandomly").addReceiverArgument(Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
						.addMethod(type(Piece.class), "rotateAndFlipRandomly", Handlers.builder(Piece.class, "rotateAndFlipRandomly").addReceiverArgument(Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
						.addType("ScriptStructurePlacement", StructurePlacementScriptEntry.class)
						.addVariableLoad("pieces", type(CheckedList.class))
						.addVariable("hints", Handlers.builder(ScriptedColumnLookup.HINTS).addImplicitArgument(loadLookup).buildVariable())
						.configure(ScriptedColumn.hintsEnvironment())
						.addVariable("distantHorizons", Handlers.builder(StructureLayoutScript.class, "distantHorizons").addImplicitArgument(load("hints", type(Hints.class))).buildVariable()),

						new ExternalEnvironmentParams()
						.withLookup(loadLookup)
						.withXZ(
							load("originX", TypeInfos.INT),
							load("originZ", TypeInfos.INT)
						)
					);
				})
				.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public void layout(
			ScriptedColumnLookup columns,
			int originX,
			int originZ,
			long worldSeed,
			RandomGenerator random,
			CheckedList<StructurePiece> pieces
		) {
			NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				this.script.layout(columns, originX, originZ, worldSeed, random, pieces);
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