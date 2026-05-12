package builderb0y.bigglobe.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;

public class LocateStructuresCommand extends AsyncCommand {

	public final HolderSet<Structure> tag;
	public final BigGlobeScriptedChunkGenerator chunkGenerator;
	public final BoundingBox area;
	public final BlockPos center;

	public LocateStructuresCommand(
		CommandSourceStack source,
		HolderSet<Structure> tag,
		BigGlobeScriptedChunkGenerator chunkGenerator,
		BoundingBox area,
		BlockPos center
	) {
		super(source);
		this.tag = tag;
		this.chunkGenerator = chunkGenerator;
		this.area = area;
		this.center = center;
	}

	@Override
	public void run() {
		this
		.chunkGenerator
		.findNearbyStructures(this.source.getLevel(), this.tag, this.area, this.center)
		.unordered()
		.takeWhile((StructureStartWrapper ignored) -> this.isValid())
		.forEach((StructureStartWrapper start) -> {
			this.source.getServer().execute(() -> {
				if (this.isValid()) {
					BlockPos center = start.box().getCenter();
					this.source.sendSuccess(
						() -> (
							Component.translatable(
								"commands.bigglobe.locate.structure.success",
								start.originalID().toString(),
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
							))
						),
						false
					);
				}
			});
		});
		this.source.getServer().execute(() -> {
			if (this.isValid()) this.source.sendSuccess(() -> Component.translatable("commands.bigglobe.locate.done"), true);
		});
	}
}