package builderb0y.bigglobe.networking.packets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.hyperspace.*;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.C2SPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;

#if MC_VERSION < MC_1_21_0
	import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
#endif

public class UseWaypointPacket implements C2SPlayPacketHandler<Integer> {

	public static final UseWaypointPacket INSTANCE = new UseWaypointPacket();

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
	public void process(ServerPlayerEntity player, Integer data, PacketSender responseSender) {
		if (player.hasPortalCooldown()) {
			EntityVersions.setPortalCooldown(player, 20);
			return;
		}
		if (!BigGlobeConfig.INSTANCE.get().hyperspaceEnabled && player.getWorld().getRegistryKey() != HyperspaceConstants.WORLD_KEY) {
			player.sendMessage(Text.translatable("bigglobe.hyperspace.disabled").formatted(Formatting.RED), true);
			return;
		}
		PlayerWaypointManager manager = PlayerWaypointManager.get(player);
		if (manager == null) {
			BigGlobeMod.LOGGER.warn(player + " has no waypoint manager?");
			return;
		}
		PlayerWaypointData waypoint = manager.getWaypoint(data);

		if (waypoint == null) {
			BigGlobeMod.LOGGER.warn(player + " attempted to use a waypoint that doesn't exist: " + data);
			return;
		}

		if (waypoint.owner() != null && !waypoint.owner().equals(player.getGameProfile().getId())) {
			BigGlobeMod.LOGGER.warn(player + " attempted to use a waypoint that doesn't belong to them: " + waypoint);
			return;
		}

		if (
			player.getWorld().getRegistryKey() != waypoint.displayPosition().world() ||
			!(player.getEyePos().squaredDistanceTo(waypoint.displayPosition().x(), waypoint.displayPosition().y(), waypoint.displayPosition().z()) <= 1.0D)
		) {
			BigGlobeMod.LOGGER.warn(player + " attempted to use " + waypoint + " without being near it.");
			return;
		}

		if (player.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
			ServerWorld destinationWorld = player.getServer().getWorld(waypoint.destinationPosition().world());
			if (destinationWorld != null) {
				manager.entrance = null;
				PackedWorldPos destinationPosition = waypoint.destination().position();
				ServerPlayerEntity newPlayer = EntityVersions.teleport(
					player,
					destinationWorld,
					new Vec3d(
						destinationPosition.x(),
						destinationPosition.y() - 1.0D,
						destinationPosition.z()
					),
					player.getVelocity(),
					player.getYaw(),
					player.getPitch()
				);
				if (newPlayer != null) {
					EntityVersions.setPortalCooldown(newPlayer, 20);
					newPlayer.interactionManager.getGameMode().setAbilities(newPlayer.getAbilities());
					newPlayer.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(newPlayer.getAbilities()));
				}
			}
			else {
				BigGlobeMod.LOGGER.warn(player + " attempted to use " + waypoint + " but its destination leads to a non-existent world. Curious.");
			}
		}
		else {
			ServerWorld hyperspace = player.getServer().getWorld(HyperspaceConstants.WORLD_KEY);
			if (hyperspace != null) {
				manager.entrance = waypoint.destination().position();
				ServerPlayerEntity newPlayer = EntityVersions.teleport(
					player,
					hyperspace,
					Vec3d.ZERO,
					player.getVelocity(),
					player.getYaw(),
					player.getPitch()
				);
				if (newPlayer != null) {
					EntityVersions.setPortalCooldown(newPlayer, 20);
				}
			}
		}
	}
}