package builderb0y.bigglobe.structures.scripted;

import java.util.random.RandomGenerator;

import net.minecraft.nbt.NbtCompound;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.*;
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
			LOAD_WORLD = load("world", 1, WorldWrapper.TYPE),
			LOAD_RANDOM = new IdentityCastInsnTree(
				getField(
					LOAD_WORLD,
					FieldInfo.getField(WorldWrapper.class, "permuter")
				),
				type(RandomGenerator.class)
			);

		public final ScriptUsage<GenericScriptTemplateUsage> usage;

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				TemplateScriptParser
				.createFrom(StructurePlacementScript.class, usage)
				.addEnvironment(JavaUtilScriptEnvironment.withRandom(LOAD_RANDOM))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(MinecraftScriptEnvironment.createWithWorld(LOAD_WORLD))
				.addEnvironment(SymmetryScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(CoordinatorScriptEnvironment.create(LOAD_WORLD))
				.addEnvironment(NbtScriptEnvironment.INSTANCE)
				.addEnvironment(WoodPaletteScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("minX",  3, TypeInfos.INT)
					.addVariableLoad("minY",  4, TypeInfos.INT)
					.addVariableLoad("minZ",  5, TypeInfos.INT)
					.addVariableLoad("maxX",  6, TypeInfos.INT)
					.addVariableLoad("maxY",  7, TypeInfos.INT)
					.addVariableLoad("maxZ",  8, TypeInfos.INT)
					.addVariableLoad("midX",  9, TypeInfos.INT)
					.addVariableLoad("midY", 10, TypeInfos.INT)
					.addVariableLoad("midZ", 11, TypeInfos.INT)
					.addVariableLoad("data", 12, NbtScriptEnvironment.NBT_COMPOUND_TYPE)
					.addVariableLoad("distantHorizons", 13, TypeInfos.BOOLEAN)
				)
				.addEnvironment(RandomScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createVariableXYZ(
						ColumnValue.REGISTRY,
						load("column", 2, type(WorldColumn.class))
					)
					.build()
				)
				.addEnvironment(StructureTemplateScriptEnvironment.create(LOAD_WORLD))
				.parse()
			);
			this.usage = usage;
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