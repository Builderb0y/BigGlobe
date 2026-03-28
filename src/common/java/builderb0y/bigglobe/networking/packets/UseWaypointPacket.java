package builderb0y.bigglobe.networking.packets;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.PackedWorldPos;
import builderb0y.bigglobe.hyperspace.PlayerWaypointData;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.C2SPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.GameProfileVersions;

public class UseWaypointPacket implements C2SPlayPacketHandler<Integer> {

	public static final UseWaypointPacket INSTANCE = new UseWaypointPacket();

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
	public void process(ServerPlayer player, Integer data, PacketSender responseSender) {
		if (player.isOnPortalCooldown()) {
			EntityVersions.setPortalCooldown(player, 20);
			return;
		}
		if (!BigGlobeConfig.INSTANCE.get().hyperspaceEnabled && EntityVersions.getWorld(player).dimension() != HyperspaceConstants.WORLD_KEY) {
			player.displayClientMessage(Component.translatable("bigglobe.hyperspace.disabled").withStyle(ChatFormatting.RED), true);
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

		if (waypoint.owner() != null && !waypoint.owner().equals(GameProfileVersions.getUUID(player.getGameProfile()))) {
			BigGlobeMod.LOGGER.warn(player + " attempted to use a waypoint that doesn't belong to them: " + waypoint);
			return;
		}

		if (
			EntityVersions.getWorld(player).dimension() != waypoint.displayPosition().world() ||
			!(player.getEyePosition().distanceToSqr(waypoint.displayPosition().x(), waypoint.displayPosition().y(), waypoint.displayPosition().z()) <= 1.0D)
		) {
			BigGlobeMod.LOGGER.warn(player + " attempted to use " + waypoint + " without being near it.");
			return;
		}

		if (EntityVersions.getWorld(player).dimension() == HyperspaceConstants.WORLD_KEY) {
			ServerLevel destinationWorld = EntityVersions.getServer(player).getLevel(waypoint.destinationPosition().world());
			if (destinationWorld != null) {
				manager.entrance = null;
				PackedWorldPos destinationPosition = waypoint.destination().pos();
				ServerPlayer newPlayer = EntityVersions.teleport(
					player,
					destinationWorld,
					new Vec3(
						destinationPosition.x(),
						destinationPosition.y() - 1.0D,
						destinationPosition.z()
					),
					player.getDeltaMovement(),
					player.getYRot(),
					player.getXRot()
				);
				if (newPlayer != null) {
					EntityVersions.setPortalCooldown(newPlayer, 20);
				}
			}
			else {
				BigGlobeMod.LOGGER.warn(player + " attempted to use " + waypoint + " but its destination leads to a non-existent world. Curious.");
			}
		}
		else {
			ServerLevel hyperspace = EntityVersions.getServer(player).getLevel(HyperspaceConstants.WORLD_KEY);
			if (hyperspace != null) {
				manager.entrance = waypoint.destination().pos();
				ServerPlayer newPlayer = EntityVersions.teleport(
					player,
					hyperspace,
					Vec3.ZERO,
					player.getDeltaMovement(),
					player.getYRot(),
					player.getXRot()
				);
				if (newPlayer != null) {
					EntityVersions.setPortalCooldown(newPlayer, 20);
				}
			}
		}
	}
}