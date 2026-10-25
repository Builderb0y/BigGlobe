package builderb0y.bigglobe.networking.packets;

import java.util.Objects;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.ServerWaypointData;
import builderb0y.bigglobe.hyperspace.ServerWaypointManager;
import builderb0y.bigglobe.items.AuraBottleItem;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.C2SPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.GameProfileVersions;
import builderb0y.bigglobe.versions.ItemStackVersions;

public class WaypointRenameC2SPacket implements C2SPlayPacketHandler<WaypointRenameC2SPacket.Data> {

	public static final WaypointRenameC2SPacket INSTANCE = new WaypointRenameC2SPacket();

	public void send(int id, InteractionHand hand) {
		FriendlyByteBuf buffer = this.buffer();
		buffer.writeVarInt(id);
		buffer.writeEnum(hand);
		BigGlobeNetwork.INSTANCE.sendToServer(buffer);
	}

	@Override
	public Data decode(ServerPlayer player, FriendlyByteBuf buffer) {
		return new Data(buffer.readVarInt(), buffer.readEnum(InteractionHand.class));
	}

	@Override
	public void process(ServerPlayer player, Data data, PacketSender responseSender) {
		if (!player.isSpectator()) {
			ServerWaypointManager manager = ServerWaypointManager.get(EntityVersions.getServerWorld(player));
			if (manager != null) {
				ServerWaypointData waypoint = manager.getWaypoint(data.id);
				if (waypoint != null) {
					if (waypoint.owner() == null || waypoint.owner().equals(GameProfileVersions.getUUID(player.getGameProfile()))) {
						if (
							(
								EntityVersions.getWorld(player).dimension() == HyperspaceConstants.WORLD_KEY ||
								EntityVersions.getWorld(player).dimension() == waypoint.pos().world()
							)
							&& player.getEyePosition().distanceToSqr(waypoint.pos().x(), waypoint.pos().y(), waypoint.pos().z()) <= EntityVersions.getEntityReachDistanceSquared(player)
						) {
							ItemStack heldItem = player.getItemInHand(data.hand);
							if (heldItem.getItem() == Items.NAME_TAG) {
								Component name = ItemStackVersions.getCustomName(heldItem);
								if (!Objects.equals(name, waypoint.name())) {
									if (!player.isCreative()) {
										heldItem.shrink(1);
									}
									player.swing(data.hand);
									manager.removeWaypoint(data.id, true);
									manager.addWaypoint(waypoint.withName(name), true);
								}
							}
							else if (heldItem.getItem() instanceof AuraBottleItem bottle) {
								if (waypoint.color() != bottle.color) {
									if (!player.isCreative()) {
										heldItem.shrink(1);
									}
									player.swing(data.hand);
									manager.removeWaypoint(data.id, true);
									manager.addWaypoint(waypoint.withColor(bottle.color), true);
								}
							}
							else {
								BigGlobeMod.LOGGER.warn(player + " attempted to modify a waypoint without holding an item that can modify waypoints.");
							}
						}
						else {
							BigGlobeMod.LOGGER.warn(player + " attempted to modify a waypoint without being near it: " + waypoint);
						}
					}
					else {
						BigGlobeMod.LOGGER.warn(player + " attempted to modify a waypoint which doesn't belong to them: " + waypoint);
					}
				}
				else {
					BigGlobeMod.LOGGER.warn(player + " attempted to modify a non-existent waypoint with ID " + data.id);
				}
			}
			else {
				BigGlobeMod.LOGGER.warn(player + " attempted to modify a waypoint while hyperspace is disabled.");
			}
		}
		else {
			BigGlobeMod.LOGGER.warn(player + " attempted to modify a waypoint while in spectator mode.");
		}
	}

	public static record Data(int id, InteractionHand hand) {

	}
}