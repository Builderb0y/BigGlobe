package builderb0y.bigglobe.structures.scripted;

import net.minecraft.nbt.NbtCompound;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.ExternalData;
import builderb0y.bigglobe.scripting.wrappers.ExternalImage;
import builderb0y.bigglobe.scripting.wrappers.ExternalImage.ColorScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructurePlacementScript extends Script {

	public abstract void place(
		WorldWrapper world,
		int minX, int minY, int minZ,
		int maxX, int maxY, int maxZ,
		int midX, int midY, int midZ,
		int chunkMinX, int chunkMinY, int chunkMinZ,
		int chunkMaxX, int chunkMaxY, int chunkMaxZ,
		ScriptedStructure.Piece piece
	);

	@Wrapper
	public static class Holder extends ScriptHolder<StructurePlacementScript> implements StructurePlacementScript {

		public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

		public Holder(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(StructurePlacementScript.class, usage)
				.configureEnvironment(JavaUtilScriptEnvironment.withRandom(WORLD.random))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.configureEnvironment(MinecraftScriptEnvironment.createWithWorld(WORLD.loadSelf))
				.configureEnvironment(SymmetryScriptEnvironment.create(WORLD.random))
				.configureEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
				.configureEnvironment(NbtScriptEnvironment.createImmutable())
				.configureEnvironment(WoodPaletteScriptEnvironment.create(WORLD.random))
				.configureEnvironment(RandomScriptEnvironment.create(WORLD.random))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.createWithSeed(WORLD.seed))
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.configureEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf))
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					registry.setupExternalEnvironment(
						environment
						.addVariableLoad("minX", TypeInfos.INT)
						.addVariableLoad("minY", TypeInfos.INT)
						.addVariableLoad("minZ", TypeInfos.INT)
						.addVariableLoad("maxX", TypeInfos.INT)
						.addVariableLoad("maxY", TypeInfos.INT)
						.addVariableLoad("maxZ", TypeInfos.INT)
						.addVariableLoad("midX", TypeInfos.INT)
						.addVariableLoad("midY", TypeInfos.INT)
						.addVariableLoad("midZ", TypeInfos.INT)
						.addVariableLoad("chunkMinX", TypeInfos.INT)
						.addVariableLoad("chunkMinY", TypeInfos.INT)
						.addVariableLoad("chunkMinZ", TypeInfos.INT)
						.addVariableLoad("chunkMaxX", TypeInfos.INT)
						.addVariableLoad("chunkMaxY", TypeInfos.INT)
						.addVariableLoad("chunkMaxZ", TypeInfos.INT)
						.addVariableLoad("piece", type(ScriptedStructure.Piece.class))
						.addVariableGetField(load("piece", type(ScriptedStructure.Piece.class)), ScriptedStructure.Piece.class, "data")
						.addVariable("distantHorizons", WORLD.distantHorizons),
						new ExternalEnvironmentParams()
						.withLookup(WORLD.loadSelf)
					);
				})
				.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
				.addEnvironment(ExternalImage.ENVIRONMENT)
				.addEnvironment(ExternalData.ENVIRONMENT)
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public void place(
			WorldWrapper world,
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			int midX, int midY, int midZ,
			int chunkMinX, int chunkMinY, int chunkMinZ,
			int chunkMaxX, int chunkMaxY, int chunkMaxZ,
			ScriptedStructure.Piece piece
		) {
			NumberArray.Direct.Manager manager = NumberArray.Direct.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				this.script.place(
					world,
					minX, minY, minZ,
					maxX, maxY, maxZ,
					midX, midY, midZ,
					chunkMinX, chunkMinY, chunkMinZ,
					chunkMaxX, chunkMaxY, chunkMaxZ,
					piece
				);
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