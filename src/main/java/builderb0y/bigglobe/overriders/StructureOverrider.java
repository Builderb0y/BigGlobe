package builderb0y.bigglobe.overriders;

import java.util.random.RandomGenerator;

import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructureOverrider extends ColumnScript {

	public abstract boolean override(ScriptedColumnLookup columns, StructureStartWrapper start, RandomGenerator random, boolean distantHorizons);

	@SuppressWarnings("deprecation")
	public static void move(StructureStartWrapper start, int yOffset) {
		start.box().move(0, yOffset, 0);
		start.start().getBoundingBox().move(0, yOffset, 0);
		for (StructurePiece piece : start.pieces()) {
			piece.translate(0, yOffset, 0);
		}
	}

	public static record Entry(Holder script) implements Overrider {

		@Override
		public Type getOverriderType() {
			return Type.STRUCTURE;
		}
	}

	@Wrapper
	public static class Holder extends ScriptHolder<StructureOverrider> implements StructureOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			LoadInsnTree loadRandom = load("random", type(RandomGenerator.class));
			this.script = (
				new TemplateScriptParser<>(StructureOverrider.class, this.usage)
				.addEnvironment(JavaUtilScriptEnvironment.withRandom(loadRandom))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(RandomScriptEnvironment.create(loadRandom))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					registry.setupExternalEnvironment(
						environment
						.addVariableLoad("start", StructureStartWrapper.TYPE)
						.addMethodInvokeStatic(StructureOverrider.class, "move")
						.addVariableLoad("distantHorizons", TypeInfos.BOOLEAN),
						new ExternalEnvironmentParams().withLookup(load("columns", type(ScriptedColumnLookup.class)))
					);
				})
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public boolean override(ScriptedColumnLookup columns, StructureStartWrapper start, RandomGenerator random, boolean distantHorizons) {
			try {
				return this.script.override(columns, start, random, distantHorizons);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return true;
			}
		}
	}
}