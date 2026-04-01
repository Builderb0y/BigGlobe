package builderb0y.bigglobe.commands;

import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.phys.Vec3;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.structures.ActiveStructurePlacementCalculator;
import builderb0y.bigglobe.structures.StructurePlacementCalculator;
import builderb0y.bigglobe.structures.StructurePlacementCalculator.FinalStructures;
import builderb0y.bigglobe.structures.StructurePlacementCalculator.StructureGenerationParams;
import builderb0y.bigglobe.structures.placement.StreamableStructurePlacement;

public class LocateStructuresCommand extends AsyncCommand {

	public final HolderSet<Structure> tag;
	public final ChunkGeneratorStructureState calculator;
	public final int centerChunkX, centerChunkZ, chunkRadius;

	public LocateStructuresCommand(
		CommandSourceStack source,
		HolderSet<Structure> tag,
		ChunkGeneratorStructureState calculator,
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
		Set<Structure> finding = this.tag.stream().map(Holder<Structure>::value).collect(Collectors.toSet());
		BigGlobeScriptedChunkGenerator generator = BigGlobeCommands.generator(this.source);
		ActiveStructurePlacementCalculator manager = (ActiveStructurePlacementCalculator)(generator.structureManager);
		Hints hints = ColumnUsage.GENERIC.normalHints();
		ScriptedColumnLookup.Impl lookup = generator.newColumnLookup(this.source.getLevel(), hints);
		this.tag.stream().flatMap((Holder<Structure> structureEntry) -> {
				return this.calculator.getPlacementsForStructure(structureEntry).stream();
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
						this.source.getLevel(),
						chunkPos
					)
				);
				for (StructureStart start : starts) {
					if (finding.contains(start.getStructure())) {
						this.source.getServer().execute(() -> {
							if (this.isValid()) this.source.sendSuccess(
								() -> {
									BlockPos center = start.getBoundingBox().getCenter();
									return Component.translatable(
											"commands.bigglobe.locate.structure.success",
											StructurePlacementCalculator.structureName(start.getStructure()),
											center.getX(),
											center.getY(),
											center.getZ(),
											BigGlobeCommands.format(
												this.source.getPosition().distanceTo(
													Vec3.atCenterOf(center)
												)
											)
										)
											.withStyle((Style style) -> (

												style
													.withHoverEvent(new HoverEvent.ShowText(Component.translatable("commands.bigglobe.locate.clickToTeleport")))
													.withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + center.getX() + " " + center.getY() + " " + center.getZ()))

											));
								}, false
							);
						});
					}
				}
			});
		this.source.getServer().execute(() -> {
			if (this.isValid()) this.source.sendSuccess(() -> Component.translatable("commands.bigglobe.locate.done"), true);
		});
	}
}