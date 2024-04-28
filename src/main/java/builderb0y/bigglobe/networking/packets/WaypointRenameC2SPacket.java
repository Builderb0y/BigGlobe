package builderb0y.bigglobe.networking.packets;

import java.util.Objects;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.ServerWaypointData;
import builderb0y.bigglobe.hyperspace.ServerWaypointManager;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.C2SPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;

public class WaypointRenameC2SPacket implements C2SPlayPacketHandler<WaypointRenameC2SPacket.Data> {

	public static final WaypointRenameC2SPacket INSTANCE = new WaypointRenameC2SPacket();

	public void send(int id, Hand hand) {
		PacketByteBuf buffer = this.buffer();
		buffer.writeVarInt(id);
		buffer.writeEnumConstant(hand);
		ClientPlayNetworking.send(BigGlobeNetwork.NETWORK_ID, buffer);
	}

	@Override
	public Data decode(ServerPlayerEntity player, PacketByteBuf buffer) {
		return new Data(buffer.readVarInt(), buffer.readEnumConstant(Hand.class));
	}

	@Override
	public void process(ServerPlayerEntity player, Data data, PacketSender responseSender) {
		ServerWaypointManager manager = ServerWaypointManager.get(EntityVersions.getServerWorld(player));
		if (manager != null) {
			ServerWaypointData waypoint = manager.getWaypoint(data.id);
			if (waypoint != null) {
				if (waypoint.owner() == null || waypoint.owner().equals(player.getGameProfile().getId())) {
					if (
						(
							player.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY ||
							player.getWorld().getRegistryKey() == waypoint.position().world()
						)
						&& player.getEyePos().squaredDistanceTo(waypoint.position().x(), waypoint.position().y(), waypoint.position().z()) <= EntityVersions.getReachDistanceSquared(player)
					) {
						ItemStack heldItem = player.getStackInHand(data.hand);
						if (heldItem.getItem() == Items.NAME_TAG) {
							Text name = heldItem.hasCustomName() ? heldItem.getName() : null;
							if (!Objects.equals(name, waypoint.name())) {
								if (!player.isCreative()) {
									heldItem.decrement(1);
								}
								player.swingHand(data.hand);
								manager.removeWaypoint(data.id, true);
								manager.addWaypoint(waypoint.withName(name), true);
							}
						}
						else {
							BigGlobeMod.LOGGER.warn(player + " attempted to rename a waypoint without holding a nametag.");
						}
					}
					else {
						BigGlobeMod.LOGGER.warn(player + " attempted to rename a waypoint without being near it: " + waypoint);
					}
				}
				else {
					BigGlobeMod.LOGGER.warn(player + " attempted to rename a waypoint which doesn't belong to them: " + waypoint);
				}
			}
			else {
				BigGlobeMod.LOGGER.warn(player + " attempted to rename a non-existent waypoint with ID " + data.id);
			}
		}
		else {
			BigGlobeMod.LOGGER.warn(player + " attempted to rename a waypoint while hyperspace is disabled.");
		}
	}

	public static record Data(int id, Hand hand) {}
}