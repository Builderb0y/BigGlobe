package builderb0y.bigglobe.structures.placement;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.scripting.bytecode.InsnTrees;
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

public class ScriptedStructurePlacement extends StructurePlacement implements StreamableStructurePlacement {

	public final StructurePlacementScript.Catcher placement;

	public ScriptedStructurePlacement(StructurePlacementScript.Catcher placement) {
		super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty());
		this.placement = placement;
	}

	@Override
	public Stream<ChunkPos> bigglobe_getNearbyStartChunks(
		BigGlobeScriptedChunkGenerator generator,
		ChunkGeneratorStructureState calculator,
		int centerChunkX,
		int centerChunkZ,
		int chunkRange
	) {
		Stream.Builder<ChunkPos> builder = Stream.builder();
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		Hints hints = ColumnUsage.GENERIC.maybeDhHints(distantHorizons);
		ScriptedColumnLookup lookup = new ScriptedColumnLookup.Impl(
			generator.columnEntryRegistry.columnFactory,
			new Params(
				generator.columnSeed,
				0,
				0,
				generator.height.min_y(),
				generator.height.max_y(),
				hints,
				generator.compiledWorldTraits
			)
		);
		this.placement.getNearbyStartChunks(
			builder,
			lookup,
			generator.columnSeed,
			centerChunkX,
			centerChunkZ,
			chunkRange
		);
		return builder.build();
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

	public static void outputStart(Stream.Builder<ChunkPos> builder, int x, int z) {
		builder.accept(new ChunkPos(x, z));
	}

	public static interface StructurePlacementScript extends Script {

		public abstract void getNearbyStartChunks(
			Stream.Builder<ChunkPos> builder,
			ScriptedColumnLookup columns,
			long worldSeed,
			int centerChunkX,
			int centerChunkZ,
			int chunkRange
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
					.configureEnvironment((MutableScriptEnvironment environment) -> {
						LoadInsnTree loadLookup = load("columns", InsnTrees.type(ScriptedColumnLookup.class));
						registry.setupExternalEnvironment(
							environment
							.addVariableLoad("worldSeed", TypeInfos.LONG)
							.addVariableLoad("centerChunkX", TypeInfos.INT)
							.addVariableLoad("centerChunkZ", TypeInfos.INT)
							.addVariableLoad("chunkRange", TypeInfos.INT)
							.addVariable("hints", Handlers.builder(ScriptedColumnLookup.HINTS).addImplicitArgument(loadLookup).buildVariable())
							.configure(ScriptedColumn.hintsEnvironment())
							.addFunction("addStart", Handlers.builder(ScriptedStructurePlacement.class, "outputStart").addArguments(load("builder", InsnTrees.type(Stream.Builder.class)), "II").buildFunction()),

							new ExternalEnvironmentParams()
							.withLookup(loadLookup)
						);
					})
					.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
					.parse(new ScriptClassLoader(registry.loader))
				);
			}

			@Override
			public void getNearbyStartChunks(
				Stream.Builder<ChunkPos> builder,
				ScriptedColumnLookup columns,
				long worldSeed,
				int centerChunkX,
				int centerChunkZ,
				int chunkRange
			) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					this.script.getNearbyStartChunks(
						builder,
						columns,
						worldSeed,
						centerChunkX,
						centerChunkZ,
						chunkRange
					);
				}
				finally {
					manager.used = used;
				}
			}
		}
	}
}