package builderb0y.bigglobe.networking.packets;

import java.util.UUID;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.TeleportTarget;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.WaypointManager.ServerWaypointData;
import builderb0y.bigglobe.hyperspace.WaypointManager.ServerWaypointManager;
import builderb0y.bigglobe.hyperspace.WaypointManager.WaypointList;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.C2SPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;

public class ExitHyperspacePacket implements C2SPlayPacketHandler {

	public static final ExitHyperspacePacket INSTANCE = new ExitHyperspacePacket();

	public void send(boolean owned, UUID uuid) {
		PacketByteBuf buffer = this.buffer();
		buffer.writeBoolean(owned).writeUuid(uuid);
		ClientPlayNetworking.send(BigGlobeNetwork.NETWORK_ID, buffer);
	}

	@Override
	public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
		boolean owned = buffer.readBoolean();
		UUID uuid = buffer.readUuid();
		if (player.getWorld().getRegistryKey() != HyperspaceConstants.WORLD_KEY) {
			return;
		}
		if (player.hasPortalCooldown()) {
			player.setPortalCooldown(20);
			return;
		}
		ServerWaypointManager manager = ServerWaypointManager.get(EntityVersions.getServerWorld(player));
		if (manager != null) {
			WaypointList<ServerWaypointData> list = manager.forUUID(owned ? player.getGameProfile().getId() : null, false);
			if (list != null) {
				ServerWaypointData waypoint = list.waypoints.get(uuid);
				if (waypoint != null) {
					ServerWorld destination = server.getWorld(waypoint.world());
					if (destination != null) {
						ServerPlayerEntity newPlayer = FabricDimensions.teleport(player, destination, new TeleportTarget(waypoint.position(), player.getVelocity(), player.getYaw(), player.getPitch()));
						if (newPlayer != null) {
							newPlayer.setPortalCooldown(20);
							newPlayer.interactionManager.getGameMode().setAbilities(newPlayer.getAbilities());
							newPlayer.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(newPlayer.getAbilities()));
						}
					}
					else {
						player.sendMessage(Text.translatable("hyperspace.invalid_destination_dimension"), true);
						list.waypoints.remove(uuid);
					}
				}
				else {
					player.sendMessage(Text.translatable("hyperspace.invalid_destination"), true);
				}
			}
			else {
				BigGlobeMod.LOGGER.warn(player + " attempted to exit hyperspace without any " + (owned ? "private" : "public") + " waypoints. UUID: " + uuid);
			}
		}
		else {
			BigGlobeMod.LOGGER.warn(player + " attempted to exit hyperspace without hyperspace being enabled.");
		}
	}
}