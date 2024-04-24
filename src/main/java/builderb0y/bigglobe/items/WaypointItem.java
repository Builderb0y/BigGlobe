package builderb0y.bigglobe.items;

import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.WaypointManager.ServerWaypointData;
import builderb0y.bigglobe.hyperspace.WaypointManager.ServerWaypointManager;

public class WaypointItem extends Item {

	public WaypointItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (
			BigGlobeEntityTypes.WAYPOINT != null &&
			context.getWorld().getRegistryKey() != HyperspaceConstants.WORLD_KEY &&
			context.getWorld() instanceof ServerWorld serverWorld
		) {
			WaypointEntity entity = BigGlobeEntityTypes.WAYPOINT.create(serverWorld);
			if (entity != null) {
				entity.setPosition(
					context.getBlockPos().getX() + 0.5D,
					context.getBlockPos().getY() + 1.5D,
					context.getBlockPos().getZ() + 0.5D
				);
				if (serverWorld.isSpaceEmpty(entity)) {
					if (context.getStack().hasCustomName()) {
						entity.setCustomName(context.getStack().getName());
					}
					entity.data = new ServerWaypointData(
						serverWorld.getRegistryKey(),
						entity.getPos(),
						UUID.randomUUID(),
						context.getPlayer() != null ? context.getPlayer().getGameProfile().getId() : null
					);
					serverWorld.spawnEntity(entity);
					context.getStack().decrement(1);
					ServerWaypointManager manager = ServerWaypointManager.get(serverWorld);
					if (manager != null) {
						manager.addWaypoint(entity.data);
					}
					return ActionResult.SUCCESS;
				}
			}
		}
		return super.useOnBlock(context);
	}
}