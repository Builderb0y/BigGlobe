package builderb0y.bigglobe.networking.packets;

import java.util.UUID;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
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

public class ExitHyperspacePacket implements C2SPlayPacketHandler<ExitHyperspacePacket.Data> {

	public static final ExitHyperspacePacket INSTANCE = new ExitHyperspacePacket();

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
		if (player.getWorld().getRegistryKey() != HyperspaceConstants.WORLD_KEY) {
			return;
		}
		if (player.hasPortalCooldown()) {
			player.setPortalCooldown(20);
			return;
		}
		ServerWaypointManager manager = ServerWaypointManager.get(EntityVersions.getServerWorld(player));
		if (manager != null) {
			WaypointList<ServerWaypointData> list = manager.forUUID(data.owner, false);
			if (list != null) {
				ServerWaypointData waypoint = list.waypoints.get(data.uuid);
				if (waypoint != null) {
					ServerWorld destination = BigGlobeMod.getCurrentServer().getWorld(waypoint.world());
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
						list.waypoints.remove(data.uuid);
					}
				}
				else {
					player.sendMessage(Text.translatable("hyperspace.invalid_destination"), true);
				}
			}
			else {
				BigGlobeMod.LOGGER.warn(player + " attempted to exit hyperspace without any " + (data.owner != null ? "private" : "public") + " waypoints. UUID: " + data.uuid);
			}
		}
		else {
			BigGlobeMod.LOGGER.warn(player + " attempted to exit hyperspace without hyperspace being enabled.");
		}
	}

	public static record Data(UUID owner, UUID uuid) {}
}