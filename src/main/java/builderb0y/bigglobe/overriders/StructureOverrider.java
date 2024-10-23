package builderb0y.bigglobe.overriders;

import java.util.random.RandomGenerator;

import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.Piece;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
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
		long seed,
		Hints hints
	);

	@SuppressWarnings("deprecation")
	public static void move(StructureStartWrapper start, int yOffset) {
		start.box().move(0, yOffset, 0);
		start.start().getBoundingBox().move(0, yOffset, 0);
		for (StructurePiece piece : start.pieces()) {
			piece.translate(0, yOffset, 0);
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

	public static record Entry(Holder script) implements Overrider {

		@Override
		public Type getOverriderType() {
			return Overrider.Type.STRUCTURE;
		}
	}

	@Wrapper
	public static class Holder extends ScriptHolder<StructureOverrider> implements StructureOverrider {

		public Holder(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			LoadInsnTree loadRandom = load("random", type(RandomGenerator.class));
			this.script = (
				new TemplateScriptParser<>(StructureOverrider.class, this.usage)
				.configureEnvironment(JavaUtilScriptEnvironment.withRandom(loadRandom))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.configureEnvironment(RandomScriptEnvironment.create(loadRandom))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.createWithSeed(load("seed", TypeInfos.LONG)))
				.configureEnvironment(MinecraftScriptEnvironment.createWithRandom(loadRandom))
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.configureEnvironment(NbtScriptEnvironment.createMutable())
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					registry.setupExternalEnvironment(
						environment
						.addFieldGet(Piece.class, "data")
						.addVariableLoad("start", StructureStartWrapper.TYPE)
						.addMethodInvokeStatics(StructureOverrider.class, "move", "moveToRange")
						.addMethod(
							type(StructureStartWrapper.class),
							"moveToRange",
							Handlers
							.builder(StructureOverrider.class, "moveToRange")
							.addReceiverArgument(StructureStartWrapper.class)
							.addArguments("II", loadRandom)
							.buildMethod()
						)
						.addVariableLoad("hints", type(Hints.class))
						.configure(ScriptedColumn.hintsEnvironment())
						.addVariableRenamedInvoke(load("hints", type(Hints.class)), "distantHorizons", MethodInfo.getMethod(Hints.class, "isLod")),
						new ExternalEnvironmentParams().withLookup(load("columns", type(ScriptedColumnLookup.class)))
					);
				})
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public boolean override(ScriptedColumnLookup columns, StructureStartWrapper start, RandomGenerator random, long seed, Hints hints) {
			NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				return this.script.override(columns, start, random, seed, hints);
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