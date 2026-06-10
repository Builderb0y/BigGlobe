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
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.ColorScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.GridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.scripting.wrappers.entries.StructureEntry;
import builderb0y.bigglobe.structures.management.SmartStructurePlacement;
import builderb0y.bigglobe.util.CheckedList;
import builderb0y.bigglobe.util.CheckedList.NullPolicy;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.*;
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
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
					.configureEnvironment(GridScriptEnvironment.createWithSeed(load("worldSeed", TypeInfos.LONG)))
					.configureEnvironment(StructureScriptEnvironment.live())
					.configure((ExpressionParser parser) -> {
						LoadInsnTree loadLookup = load("columns", InsnTrees.type(ScriptedColumnLookup.class));
						parser
						.environment
						.mutable()
						.addVariableLoad("starts", TypeInfo.of(List.class))
						.addVariableLoad("worldSeed", TypeInfos.LONG)
						.addVariableLoad("regionMinX", TypeInfos.INT)
						.addVariableLoad("regionMinY", TypeInfos.INT)
						.addVariableLoad("regionMinZ", TypeInfos.INT)
						.addVariableLoad("regionMaxX", TypeInfos.INT)
						.addVariableLoad("regionMaxY", TypeInfos.INT)
						.addVariableLoad("regionMaxZ", TypeInfos.INT)
						.addFunction(Handlers.methodBuilder(ScriptedStructurePlacement.class, "createNear").exposedName("createStartNear").addArguments(load("context", TypeInfo.of(Context.class)), "III", StructureEntry.class).buildFunction())
						.addFunction(Handlers.methodBuilder(ScriptedStructurePlacement.class, "createAt").exposedName("createStartAt").addArguments(load("context", TypeInfo.of(Context.class)), "III", StructureEntry.class).buildFunction())
						.addMethod(Handlers.methodBuilder(ScriptedStructurePlacement.class, "override").addReceiverArgument(StructureStartWrapper.class).addImplicitArgument(load("context", TypeInfo.of(Context.class))).buildMethod())
						.addVariableRenamedInvoke(loadLookup, "hints", ScriptedColumnLookup.HINTS)
						;
						registry.setupEnvironment(
							parser,
							new ExternalEnvironmentParams().withLookup("columns", loadLookup)
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