package builderb0y.bigglobe.structures.scripted;

import java.util.random.RandomGenerator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.entries.StructurePlacementScriptEntry;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.Piece;
import builderb0y.bigglobe.util.CheckedList;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.*;
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
			LoadInsnTree loadLookup = load("columns", type(ScriptedColumnLookup.class));
			this.script = (
				new TemplateScriptParser<>(StructureLayoutScript.class, this.usage, registry.parserFlags())
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.createWithSeed(load("worldSeed", TypeInfos.LONG)))
				.configureEnvironment(StructureScriptEnvironment.live())
				.configureEnvironment(NbtScriptEnvironment.createMutable())
				.configureEnvironment(SymmetryScriptEnvironment.create(LOAD_RANDOM))
				.configure((ExpressionParser parser) -> {
					parser
					.environment
					.mutable()
					.addVariableLoad("worldSeed", TypeInfos.LONG)
					.addFieldGet(ScriptedStructure.Piece.class, "data")
					.addVariableLoad("originX", TypeInfos.INT)
					.addVariableLoad("originZ", TypeInfos.INT)
					.addQualifiedSpecificConstructor(Piece.class, int.class, int.class, int.class, int.class, int.class, int.class, StructurePlacementScriptEntry.class, CompoundTag.class)
					.addMethodInvokes(Piece.class, "withRotation", "rotateAround", "symmetrify", "symmetrifyAround", "offset")
					.addMethodMultiInvokes(Piece.class, "rotateRandomly", "rotateAndFlipRandomly")
					.addMethod(Handlers.methodBuilder(Piece.class, "rotateRandomly").addReceiverArgument(Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
					.addMethod(Handlers.methodBuilder(Piece.class, "rotateAndFlipRandomly").addReceiverArgument(Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
					.addType("ScriptStructurePlacement", StructurePlacementScriptEntry.class)
					.addVariableLoad("pieces", type(CheckedList.class))
					;
					registry.setupEnvironment(
						parser,
						new ExternalEnvironmentParams()
						.withLookup("columns", loadLookup)
						.withXZ(
							load("originX", TypeInfos.INT),
							load("originZ", TypeInfos.INT)
						)
					);
				})
				.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
				.addImportedValue("random", LOAD_RANDOM)
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