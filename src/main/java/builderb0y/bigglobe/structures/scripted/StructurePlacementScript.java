package builderb0y.bigglobe.structures.scripted;

import java.util.random.RandomGenerator;

import net.minecraft.nbt.NbtCompound;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.IdentityCastInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
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
		NbtCompound data,
		boolean distantHorizons
	);

	@Wrapper
	public static class Holder extends ScriptHolder<StructurePlacementScript> implements StructurePlacementScript {

		public static final InsnTree
			LOAD_WORLD = load("world", WorldWrapper.TYPE),
			LOAD_RANDOM = new IdentityCastInsnTree(
				getField(
					LOAD_WORLD,
					FieldInfo.getField(WorldWrapper.class, "permuter")
				),
				type(RandomGenerator.class)
			);

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				usage,
				new TemplateScriptParser<>(StructurePlacementScript.class, usage)
				.addEnvironment(JavaUtilScriptEnvironment.withRandom(LOAD_RANDOM))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(MinecraftScriptEnvironment.createWithWorld(LOAD_WORLD))
				.addEnvironment(SymmetryScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(CoordinatorScriptEnvironment.create(LOAD_WORLD))
				.addEnvironment(NbtScriptEnvironment.INSTANCE)
				.addEnvironment(WoodPaletteScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("minX", TypeInfos.INT)
					.addVariableLoad("minY", TypeInfos.INT)
					.addVariableLoad("minZ", TypeInfos.INT)
					.addVariableLoad("maxX", TypeInfos.INT)
					.addVariableLoad("maxY", TypeInfos.INT)
					.addVariableLoad("maxZ", TypeInfos.INT)
					.addVariableLoad("midX", TypeInfos.INT)
					.addVariableLoad("midY", TypeInfos.INT)
					.addVariableLoad("midZ", TypeInfos.INT)
					.addVariableLoad("data", NbtScriptEnvironment.NBT_COMPOUND_TYPE)
					.addVariableLoad("distantHorizons", TypeInfos.BOOLEAN)
				)
				.addEnvironment(RandomScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createVariableXYZ(
						ColumnValue.REGISTRY,
						load("column", type(WorldColumn.class))
					)
					.build()
				)
				.addEnvironment(StructureTemplateScriptEnvironment.create(LOAD_WORLD))
				.parse()
			);
		}

		@Override
		public void place(
			WorldWrapper world,
			WorldColumn column,
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			int midX, int midY, int midZ,
			NbtCompound data,
			boolean distantHorizons
		) {
			try {
				this.script.place(
					world,
					column,
					minX, minY, minZ,
					maxX, maxY, maxZ,
					midX, midY, midZ,
					data,
					distantHorizons
				);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}