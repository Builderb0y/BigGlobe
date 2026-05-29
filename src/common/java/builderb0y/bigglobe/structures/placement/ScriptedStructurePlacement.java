package builderb0y.bigglobe.structures.placement;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted2.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.scripting.wrappers.entries.StructureEntry;
import builderb0y.bigglobe.structures.management.SmartStructurePlacement;
import builderb0y.bigglobe.util.CheckedList;
import builderb0y.bigglobe.util.CheckedList.NullPolicy;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.TypeInfo;
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

public class ScriptedStructurePlacement extends StructurePlacement implements SmartStructurePlacement {

	public final StructurePlacementScript.Catcher placement;

	public ScriptedStructurePlacement(StructurePlacementScript.Catcher placement) {
		super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty());
		this.placement = placement;
	}

	@Override
	public Stream<StructureStartWrapper> bigglobe_generateStructuresInArea(Context context) {
		List<StructureStartWrapper> starts = new CheckedList<>(StructureStartWrapper.class, NullPolicy.IGNORE);
		this.placement.populateStructures(
			context.columns(),
			starts,
			context,
			context.hashedWorldSeed(),
			context.area().minX(),
			context.area().minY(),
			context.area().minZ(),
			context.area().maxX(),
			context.area().maxY(),
			context.area().maxZ()
		);
		return starts.stream();
	}

	@Override
	public boolean isStructureChunk(ChunkGeneratorStructureState calculator, int chunkX, int chunkZ) {
		return false;
	}

	@Override
	public boolean isPlacementChunk(ChunkGeneratorStructureState calculator, int chunkX, int chunkZ) {
		return false;
	}

	@Override
	public StructurePlacementType<?> type() {
		return BigGlobeStructurePlacementTypes.SCRIPTED;
	}

	public static StructureStartWrapper createNear(Context context, int x, int y, int z, StructureEntry structure) {
		return context.createNear(new BlockPos(x, y, z), structure);
	}

	public static StructureStartWrapper createAt(Context context, int x, int y, int z, StructureEntry structure) {
		return context.createAt(new BlockPos(x, y, z), structure);
	}

	public static boolean override(StructureStartWrapper start, Context context) {
		return context.override(start);
	}

	public static interface StructurePlacementScript extends Script {

		public abstract void populateStructures(
			ScriptedColumnLookup columns,
			List<StructureStartWrapper> starts,
			Context context,
			long worldSeed,
			int regionMinX,
			int regionMinY,
			int regionMinZ,
			int regionMaxX,
			int regionMaxY,
			int regionMaxZ
		);

		@Wrapper
		public static class Catcher extends ScriptCatcher<StructurePlacementScript> implements StructurePlacementScript {

			public Catcher(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				this.script = (
					new TemplateScriptParser<>(StructurePlacementScript.class, this.usage, registry.parserFlags())
					.configureEnvironment(JavaUtilScriptEnvironment.withoutRandom())
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.addEnvironment(RandomScriptEnvironment.BASE)
					.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
					.configureEnvironment(GridScriptEnvironment.createWithSeed(load("worldSeed", TypeInfos.LONG)))
					.addEnvironment(WoodPaletteScriptEnvironment.BASE)
					.configureEnvironment(MinecraftScriptEnvironment.create())
					.configureEnvironment(StructureScriptEnvironment.live())
					.configureEnvironment((MutableScriptEnvironment environment) -> {
						LoadInsnTree loadLookup = load("columns", InsnTrees.type(ScriptedColumnLookup.class));
						registry.setupEnvironment(
							environment
							.addVariableLoad("starts", TypeInfo.of(List.class))
							.addVariableLoad("worldSeed", TypeInfos.LONG)
							.addVariableLoad("regionMinX", TypeInfos.INT)
							.addVariableLoad("regionMinY", TypeInfos.INT)
							.addVariableLoad("regionMinZ", TypeInfos.INT)
							.addVariableLoad("regionMaxX", TypeInfos.INT)
							.addVariableLoad("regionMaxY", TypeInfos.INT)
							.addVariableLoad("regionMaxZ", TypeInfos.INT)
							.addFunction("createStartNear", Handlers.builder(ScriptedStructurePlacement.class, "createNear").addArguments(load("context", TypeInfo.of(Context.class)), "III", StructureEntry.class).buildFunction())
							.addFunction("createStartAt", Handlers.builder(ScriptedStructurePlacement.class, "createAt").addArguments(load("context", TypeInfo.of(Context.class)), "III", StructureEntry.class).buildFunction())
							.addMethod(InsnTrees.type(StructureStartWrapper.class), "override", Handlers.builder(ScriptedStructurePlacement.class, "override").addReceiverArgument(StructureStartWrapper.class).addImplicitArgument(load("context", TypeInfo.of(Context.class))).buildMethod())
							.addVariableRenamedInvoke(loadLookup, "hints", ScriptedColumnLookup.HINTS)
							.configure(ScriptedColumn.baseEnvironment(null, loadLookup, registry.columnCompileContext.columnTypeInfo())),
							new ExternalEnvironmentParams().withLookup(loadLookup)
						);
					})
					.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
					.parse(new ScriptClassLoader(registry.loader))
				);
			}

			@Override
			public void populateStructures(
				ScriptedColumnLookup columns,
				List<StructureStartWrapper> starts,
				Context context,
				long worldSeed,
				int regionMinX,
				int regionMinY,
				int regionMinZ,
				int regionMaxX,
				int regionMaxY,
				int regionMaxZ
			) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					this.script.populateStructures(
						columns,
						starts,
						context,
						worldSeed,
						regionMinX,
						regionMinY,
						regionMinZ,
						regionMaxX,
						regionMaxY,
						regionMaxZ
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
}