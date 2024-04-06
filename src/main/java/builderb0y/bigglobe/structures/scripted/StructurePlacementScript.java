package builderb0y.bigglobe.structures.scripted;

import net.minecraft.nbt.NbtCompound;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.util.TypeInfos;

public interface StructurePlacementScript extends Script {

	public abstract void place(
		WorldWrapper world,
		int minX, int minY, int minZ,
		int maxX, int maxY, int maxZ,
		int midX, int midY, int midZ,
		int chunkMinX, int chunkMinY, int chunkMinZ,
		int chunkMaxX, int chunkMaxY, int chunkMaxZ,
		NbtCompound data
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
				.addEnvironment(JavaUtilScriptEnvironment.withRandom(WORLD.random))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(MinecraftScriptEnvironment.createWithWorld(WORLD.loadSelf))
				.addEnvironment(SymmetryScriptEnvironment.create(WORLD.random))
				.addEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
				.addEnvironment(NbtScriptEnvironment.createImmutable())
				.addEnvironment(WoodPaletteScriptEnvironment.create(WORLD.random))
				.addEnvironment(RandomScriptEnvironment.create(WORLD.random))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.addEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf))
				.addEnvironment(GridScriptEnvironment.createWithSeed(WORLD.seed))
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
						.addVariableLoad("data", NbtScriptEnvironment.NBT_COMPOUND_TYPE)
						.addVariable("distantHorizons", WORLD.distantHorizons),
						new ExternalEnvironmentParams()
						.withLookup(WORLD.loadSelf)
					);
				})
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
			NbtCompound data
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
					data
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