package builderb0y.bigglobe.commands;

import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.SuggestCommand;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.structures.ActiveStructureManager;
import builderb0y.bigglobe.structures.StructureManager;
import builderb0y.bigglobe.structures.StructureManager.FinalStructures;
import builderb0y.bigglobe.structures.StructureManager.StructureGenerationParams;
import builderb0y.bigglobe.structures.placement.StreamableStructurePlacement;

public class LocateStructuresCommand extends AsyncCommand {

	public final RegistryEntryList<Structure> tag;
	public final StructurePlacementCalculator calculator;
	public final int centerChunkX, centerChunkZ, chunkRadius;

	public LocateStructuresCommand(
		ServerCommandSource source,
		RegistryEntryList<Structure> tag,
		StructurePlacementCalculator calculator,
		int centerChunkX,
		int centerChunkZ,
		int chunkRadius
	) {
		super(source);
		this.tag = tag;
		this.calculator = calculator;
		this.centerChunkX = centerChunkX;
		this.centerChunkZ = centerChunkZ;
		this.chunkRadius = chunkRadius;
	}

	@Override
	public void run() {
		Set<Structure> finding = this.tag.stream().map(RegistryEntry<Structure>::value).collect(Collectors.toSet());
		BigGlobeScriptedChunkGenerator generator = BigGlobeCommands.generator(this.source);
		ActiveStructureManager manager = (ActiveStructureManager)(generator.structureManager);
		Hints hints = ColumnUsage.GENERIC.normalHints();
		ScriptedColumnLookup.Impl lookup = generator.newColumnLookup(this.source.getWorld(), hints);
		this.tag.stream().flatMap((RegistryEntry<Structure> structureEntry) -> {
			return this.calculator.getPlacements(structureEntry).stream();
		})
		.distinct()
		.flatMap((StructurePlacement placement) -> {
			return ((StreamableStructurePlacement)(placement)).bigglobe_getNearbyStartChunks(
				generator,
				this.calculator,
				this.centerChunkX,
				this.centerChunkZ,
				this.chunkRadius
			);
		})
		//.sorted(StreamableStructurePlacement.distanceComparator(this.centerChunkX, this.centerChunkZ))
		.takeWhile((ChunkPos ignored) -> this.isValid())
		.forEachOrdered((ChunkPos chunkPos) -> {
			FinalStructures starts = manager.getFinalStructures(
				new StructureGenerationParams(
					generator,
					lookup,
					//should be thread-safe for all the things
					//StructureGenerationParams does with this.
					this.source.getWorld(),
					chunkPos
				)
			);
			for (StructureStart start : starts) {
				if (finding.contains(start.getStructure())) {
					this.source.getServer().execute(() -> {
						if (this.isValid()) this.source.sendFeedback(() -> {
							BlockPos center = start.getBoundingBox().getCenter();
							return Text.translatable(
								"commands.bigglobe.locate.structure.success",
								StructureManager.structureName(start.getStructure()),
								center.getX(),
								center.getY(),
								center.getZ(),
								BigGlobeCommands.format(
									this.source.getPosition().distanceTo(
										Vec3d.ofCenter(center)
									)
								)
							)
							.styled((Style style) -> (
								style
								.withHoverEvent(new HoverEvent.ShowText(Text.translatable("commands.bigglobe.locate.clickToTeleport")))
								.withClickEvent(new SuggestCommand("/tp @s " + center.getX() + " " + center.getY() + " " + center.getZ()))
							));
						}, false);
					});
				}
			}
		});
		this.source.getServer().execute(() -> {
			if (this.isValid()) this.source.sendFeedback(() -> Text.translatable("commands.bigglobe.locate.done"), true);
		});
	}
}