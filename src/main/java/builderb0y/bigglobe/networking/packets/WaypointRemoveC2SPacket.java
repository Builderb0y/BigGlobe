package builderb0y.bigglobe.networking.packets;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.PlayerWaypointData;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.hyperspace.ServerWaypointManager;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.C2SPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.GameruleVersions;
import builderb0y.bigglobe.versions.ItemStackVersions;

public class WaypointRemoveC2SPacket implements C2SPlayPacketHandler<Integer> {

	public static final WaypointRemoveC2SPacket INSTANCE = new WaypointRemoveC2SPacket();

	public void send(int id) {
		FriendlyByteBuf buffer = this.buffer();
		buffer.writeVarInt(id);
		BigGlobeNetwork.INSTANCE.sendToServer(buffer);
	}

	@Override
	public Integer decode(ServerPlayer player, FriendlyByteBuf buffer) {
		return buffer.readVarInt();
	}

	@Override
	public void process(ServerPlayer player, Integer id, PacketSender responseSender) {
		if (!player.isSpectator()) {
			ServerWaypointManager serverManager = ServerWaypointManager.get(EntityVersions.getServerWorld(player));
			if (serverManager != null) {
				PlayerWaypointManager serverPlayerManager = PlayerWaypointManager.get(player);
				if (serverPlayerManager != null) {
					PlayerWaypointData waypoint = serverPlayerManager.getWaypoint(id);
					if (waypoint != null) {
						if (player.getEyePosition().distanceToSqr(waypoint.displayPosition().x(), waypoint.displayPosition().y(), waypoint.displayPosition().z()) <= EntityVersions.getEntityReachDistanceSquared(player)) {
							serverManager.removeWaypoint(id, true);
							Item drop = waypoint.owner() != null ? BigGlobeItems.PRIVATE_WAYPOINT : BigGlobeItems.PUBLIC_WAYPOINT;
							if (GameruleVersions.entityDrops(EntityVersions.getServerWorld(player))) {
								ItemStack stack = new ItemStack(drop);
								if (waypoint.destination().name() != null) {
									ItemStackVersions.setCustomName(stack, waypoint.destination().name());
								}
								ItemEntity entity = new ItemEntity(EntityVersions.getWorld(player), waypoint.displayPosition().x(), waypoint.displayPosition().y(), waypoint.displayPosition().z(), stack);
								if (EntityVersions.getWorld(player).dimension() == HyperspaceConstants.WORLD_KEY) {
									entity.setNoGravity(true);
								}
								EntityVersions.getWorld(player).addFreshEntity(entity);
							}
						}
						else {
							BigGlobeMod.LOGGER.warn(player + " attempted to destroy a waypoint without being near it: " + waypoint);
						}
					}
					else {
						BigGlobeMod.LOGGER.warn(player + " attempted to destroy a non-existent waypoint with ID " + id);
					}
				}
				else {
					BigGlobeMod.LOGGER.warn(player + " has no waypoint manager?");
				}
			}
			else {
				BigGlobeMod.LOGGER.warn(player + " attempted to destroy a waypoint while hyperspace is disabled.");
			}
		}
		else {
			BigGlobeMod.LOGGER.warn(player + " attempted to destroy a waypoint while in spectator mode.");
		}
	}
}