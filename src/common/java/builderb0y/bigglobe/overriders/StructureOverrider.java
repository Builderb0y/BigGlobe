package builderb0y.bigglobe.overriders;

import java.util.random.RandomGenerator;

import net.minecraft.world.level.levelgen.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.GridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.NbtScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructureOverrider extends ColumnScript {

	public abstract boolean override(
		ScriptedColumnLookup columns,
		StructureStartWrapper start,
		RandomGenerator random,
		long seed
	);

	@SuppressWarnings("deprecation")
	public static void move(StructureStartWrapper start, int yOffset) {
		start.box().move(0, yOffset, 0);
		start.start().getBoundingBox().move(0, yOffset, 0);
		for (StructurePiece piece : start.pieces()) {
			piece.move(0, yOffset, 0);
		}
	}

	public static boolean moveToRange(StructureStartWrapper start, int minY, int maxY, RandomGenerator random) {
		int minMove = minY - start.minY();
		int maxMove = maxY - start.maxY();
		if (maxMove > minMove) {
			move(start, random.nextInt(minMove, maxMove));
			return true;
		}
		else {
			return false;
		}
	}

	public static record Entry(Catcher script) implements Overrider {

		@Override
		public Type getOverriderType() {
			return Overrider.Type.STRUCTURE;
		}
	}

	@Wrapper
	public static class Catcher extends ScriptCatcher<StructureOverrider> implements StructureOverrider {

		public Catcher(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			LoadInsnTree loadRandom = load("random", type(RandomGenerator.class));
			LoadInsnTree loadLookup = load("columns", type(ScriptedColumnLookup.class));
			this.script = (
				new TemplateScriptParser<>(StructureOverrider.class, this.usage, registry.parserFlags())
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.createWithSeed(load("seed", TypeInfos.LONG)))
				.configureEnvironment(StructureScriptEnvironment.live())
				.configureEnvironment(NbtScriptEnvironment.createMutable())
				.configure((ExpressionParser parser) -> {
					parser
					.environment
					.mutable()
					.addFieldGet(ScriptedStructure.Piece.class, "data")
					.addVariableLoad("start", StructureStartWrapper.TYPE)
					.addMethodInvokeStatics(StructureOverrider.class, "move", "moveToRange")
					.addMethod(
						Handlers
						.methodBuilder(StructureOverrider.class, "moveToRange")
						.addReceiverArgument(StructureStartWrapper.class)
						.addArguments("II", loadRandom)
						.buildMethod()
					)
					.addVariable(Handlers.methodBuilder(ScriptedColumnLookup.HINTS).addImplicitArgument(loadLookup).buildVariable())
					;
					registry.setupEnvironment(
						parser,
						new ExternalEnvironmentParams().withLookup("columns", loadLookup)
					);
				})
				.addImportedValue("random", load("random", type(RandomGenerator.class)))
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public boolean override(ScriptedColumnLookup columns, StructureStartWrapper start, RandomGenerator random, long seed) {
			NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				return this.script.override(columns, start, random, seed);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return true;
			}
			finally {
				manager.used = used;
			}
		}
	}
}