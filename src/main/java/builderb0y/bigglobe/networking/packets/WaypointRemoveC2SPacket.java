package builderb0y.bigglobe.networking.packets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.*;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.C2SPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.ItemStackVersions;

public class WaypointRemoveC2SPacket implements C2SPlayPacketHandler<Integer> {

	public static final WaypointRemoveC2SPacket INSTANCE = new WaypointRemoveC2SPacket();

	public void send(int id) {
		PacketByteBuf buffer = this.buffer();
		buffer.writeVarInt(id);
		BigGlobeNetwork.INSTANCE.sendToServer(buffer);
	}

	@Override
	public Integer decode(ServerPlayerEntity player, PacketByteBuf buffer) {
		return buffer.readVarInt();
	}

	@Override
	public void process(ServerPlayerEntity player, Integer id, PacketSender responseSender) {
		if (!player.isSpectator()) {
			ServerWaypointManager serverManager = ServerWaypointManager.get(EntityVersions.getServerWorld(player));
			if (serverManager != null) {
				PlayerWaypointManager serverPlayerManager = PlayerWaypointManager.get(player);
				if (serverPlayerManager != null) {
					PlayerWaypointData waypoint = serverPlayerManager.getWaypoint(id);
					if (waypoint != null) {
						if (player.getEyePos().squaredDistanceTo(waypoint.displayPosition().x(), waypoint.displayPosition().y(), waypoint.displayPosition().z()) <= EntityVersions.getEntityReachDistanceSquared(player)) {
							serverManager.removeWaypoint(id, true);
							Item drop = waypoint.owner() != null ? BigGlobeItems.PRIVATE_WAYPOINT : BigGlobeItems.PUBLIC_WAYPOINT;
							if (drop != null && player.getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
								ItemStack stack = new ItemStack(drop);
								if (waypoint.destination().name() != null) {
									ItemStackVersions.setCustomName(stack, waypoint.destination().name());
								}
								ItemEntity entity = new ItemEntity(player.getWorld(), waypoint.displayPosition().x(), waypoint.displayPosition().y(), waypoint.displayPosition().z(), stack);
								if (player.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
									entity.setNoGravity(true);
								}
								player.getWorld().spawnEntity(entity);
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