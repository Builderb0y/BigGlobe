package builderb0y.bigglobe.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.PackedWorldPos;
import builderb0y.bigglobe.hyperspace.ServerWaypointData;
import builderb0y.bigglobe.hyperspace.ServerWaypointManager;

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
			ServerWaypointManager manager = ServerWaypointManager.get(serverWorld);
			if (
				manager != null &&
				manager.addWaypoint(
					new ServerWaypointData(
						new PackedWorldPos(
							serverWorld.getRegistryKey(),
							context.getBlockPos().getX() + 0.5D,
							context.getBlockPos().getY() + 2.5D,
							context.getBlockPos().getZ() + 0.5D
						),
						manager.nextID(),
						context.getPlayer() != null ? context.getPlayer().getGameProfile().getId() : null
					),
					true
				)
			) {
				context.getStack().decrement(1);
				return ActionResult.SUCCESS;
			}
		}
		return super.useOnBlock(context);
	}
}