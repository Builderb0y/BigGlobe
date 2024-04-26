package builderb0y.bigglobe.networking.packets;

import java.util.UUID;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.ClientWaypointData;
import builderb0y.bigglobe.hyperspace.ClientWaypointManager;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.C2SPlayPacketHandler;

public class UseWaypointPacket implements C2SPlayPacketHandler<UseWaypointPacket.Data> {

	public static final UseWaypointPacket INSTANCE = new UseWaypointPacket();
	public static boolean teleporting;

	public void send(boolean owned, UUID uuid) {
		PacketByteBuf buffer = this.buffer();
		buffer.writeBoolean(owned).writeUuid(uuid);
		ClientPlayNetworking.send(BigGlobeNetwork.NETWORK_ID, buffer);
	}

	@Override
	public Data decode(ServerPlayerEntity player, PacketByteBuf buffer) {
		boolean owned = buffer.readBoolean();
		UUID owner = owned ? player.getGameProfile().getId() : null;
		UUID uuid = buffer.readUuid();
		return new Data(owner, uuid);
	}

	@Override
	public void process(ServerPlayerEntity player, Data data, PacketSender responseSender) {
		if (player.hasPortalCooldown()) {
			player.setPortalCooldown(20);
			return;
		}
		ClientWaypointManager manager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
		ClientWaypointData waypoint = manager.getWaypoint(data.owner, data.uuid);
		if (waypoint != null) {
			if (player.getEyePos().squaredDistanceTo(waypoint.clientPosition().x(), waypoint.clientPosition().y(), waypoint.clientPosition().z()) <= 1.0D) {
				if (player.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
					ServerWorld destinationWorld = player.getServer().getWorld(waypoint.destination().world());
					if (destinationWorld != null) {
						teleporting = true;
						try {
							ServerPlayerEntity newPlayer = FabricDimensions.teleport(player, destinationWorld, new TeleportTarget(waypoint.destination().position().toMCVec(), player.getVelocity(), player.getYaw(), player.getPitch()));
							if (newPlayer != null) {
								newPlayer.setPortalCooldown(20);
								newPlayer.interactionManager.getGameMode().setAbilities(newPlayer.getAbilities());
								newPlayer.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(newPlayer.getAbilities()));
								WaypointListS2CPacket.INSTANCE.recompute(newPlayer, null);
								WaypointListS2CPacket.INSTANCE.send(newPlayer);
							}
						}
						finally {
							teleporting = false;
						}
					}
					else {
						BigGlobeMod.LOGGER.warn(player + " attempted to use " + waypoint + " but its destination leads to a non-existent world. Curious.");
					}
				}
				else {
					ServerWorld hyperspace = player.getServer().getWorld(HyperspaceConstants.WORLD_KEY);
					if (hyperspace != null) {
						teleporting = true;
						try {
							ServerPlayerEntity newPlayer = FabricDimensions.teleport(player, hyperspace, new TeleportTarget(Vec3d.ZERO, player.getVelocity(), player.getYaw(), player.getPitch()));
							if (newPlayer != null) {
								newPlayer.setPortalCooldown(20);
								WaypointListS2CPacket.INSTANCE.recompute(newPlayer, waypoint.destination());
								WaypointListS2CPacket.INSTANCE.send(newPlayer);
							}
						}
						finally {
							teleporting = false;
						}
					}
				}
			}
			else {
				BigGlobeMod.LOGGER.warn(player + " attempted to use " + waypoint + " without being near it.");
			}
		}
		else {
			BigGlobeMod.LOGGER.warn(player + " attempted to use a " + (data.owner != null ? "private" : "public") + " waypoint that doesn't exist: " + data.uuid);
		}
	}

	public static record Data(UUID owner, UUID uuid) {}
}