package builderb0y.bigglobe.networking.packets;

import java.util.UUID;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import builderb0y.bigglobe.hyperspace.ClientWaypointData;
import builderb0y.bigglobe.hyperspace.PackedPosition;
import builderb0y.bigglobe.hyperspace.ServerWaypointData;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class WaypointAddS2CPacket implements S2CPlayPacketHandler<ClientWaypointData> {

	public static final WaypointAddS2CPacket INSTANCE = new WaypointAddS2CPacket();

	public void send(ServerPlayerEntity player, ClientWaypointData waypoint) {
		PacketByteBuf buffer = this.buffer();
		buffer.writeBoolean(waypoint.owner() != null);
		if (waypoint.owner() != null) buffer.writeUuid(waypoint.owner());
		buffer.writeRegistryKey(waypoint.world());
		waypoint.destination().position().write(buffer);
		buffer.writeUuid(waypoint.uuid());
		waypoint.clientPosition().write(buffer);
		ServerPlayNetworking.send(player, BigGlobeNetwork.NETWORK_ID, buffer);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ClientWaypointData decode(PacketByteBuf buffer) {
		boolean owned = buffer.readBoolean();
		UUID owner = owned ? buffer.readUuid() : null;
		RegistryKey<World> world = buffer.readRegistryKey(RegistryKeyVersions.world());
		PackedPosition destination = PackedPosition.read(buffer);
		UUID uuid = buffer.readUuid();
		PackedPosition clientPosition = PackedPosition.read(buffer);
		return new ClientWaypointData(new ServerWaypointData(world, destination, uuid, owner), clientPosition);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(ClientWaypointData waypoint, PacketSender responseSender) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		((WaypointTracker)(player)).bigglobe_getWaypointManager().addWaypoint(waypoint, true);
	}
}