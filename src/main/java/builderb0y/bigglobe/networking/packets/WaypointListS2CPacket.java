package builderb0y.bigglobe.networking.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
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

import builderb0y.bigglobe.hyperspace.*;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

/**
schema:
player UUID (UUID).
is hyperspace dimension (boolean).
number of worlds (varint).
worlds:
	registry key.
number of waypoints (varint).
waypoints:
	is private (boolean).
	destination world ID (varint).
	destination packed X (int).
	destination packed Y (int).
	destination packed Z (int).
	UUID (UUID).
	if hyperspace:
		display position packed X (int).
		display position packed Y (int).
		display position packed Z (int).
*/
public class WaypointListS2CPacket implements S2CPlayPacketHandler<List<SyncedWaypointData>> {

	public static final WaypointListS2CPacket INSTANCE = new WaypointListS2CPacket();

	public void send(ServerPlayerEntity player) {
		PlayerWaypointManager manager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
		PacketByteBuf buffer = this.buffer();

		boolean isHyperspace = player.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY;
		buffer.writeBoolean(isHyperspace);
		Object2IntMap<RegistryKey<World>> worlds = new Object2IntLinkedOpenHashMap<>(4);
		worlds.defaultReturnValue(-1);
		worlds.put(HyperspaceConstants.WORLD_KEY, 0);
		ToIntFunction<RegistryKey<World>> computer = (RegistryKey<World> $) -> worlds.size();
		int waypointCount = 0;
		for (PlayerWaypointData data : manager.getAllWaypoints()) {
			worlds.computeIfAbsent(data.destination().position().world(), computer);
			waypointCount++;
		}
		buffer.writeVarInt(worlds.size());
		for (Object2IntMap.Entry<RegistryKey<World>> entry : worlds.object2IntEntrySet()) {
			buffer.writeRegistryKey(entry.getKey());
		}

		buffer.writeVarInt(waypointCount);
		for (PlayerWaypointData waypoint : manager.getAllWaypoints()) {
			buffer.writeUuid(waypoint.uuid());
			buffer.writeBoolean(waypoint.owner() != null);
			waypoint.destinationPosition().writeBulk(buffer, worlds);
			if (isHyperspace) waypoint.displayPosition().writeBulk(buffer, worlds);
		}

		ServerPlayNetworking.send(player, BigGlobeNetwork.NETWORK_ID, buffer);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public List<SyncedWaypointData> decode(PacketByteBuf buffer) {
		boolean isHyperspace = buffer.readBoolean();
		int worldCount = buffer.readVarInt();
		List<RegistryKey<World>> worlds = new ArrayList<>(worldCount);
		for (int worldIndex = 0; worldIndex < worldCount; worldIndex++) {
			worlds.add(buffer.readRegistryKey(RegistryKeyVersions.world()));
		}
		int waypointCount = buffer.readVarInt();
		List<SyncedWaypointData> waypoints = new ArrayList<>(waypointCount);
		for (int waypointIndex = 0; waypointIndex < waypointCount; waypointIndex++) {
			UUID uuid = buffer.readUuid();
			boolean owned = buffer.readBoolean();
			PackedWorldPos destination = PackedWorldPos.readBulk(buffer, worlds);
			PackedWorldPos displayPosition = isHyperspace ? PackedWorldPos.readBulk(buffer, worlds) : destination;
			waypoints.add(new SyncedWaypointData(uuid, owned, destination, displayPosition));
		}
		return waypoints;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(List<SyncedWaypointData> waypoints, PacketSender responseSender) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null) {
			player.setPortalCooldown(20);
			PlayerWaypointManager manager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
			manager.clear();
			for (SyncedWaypointData waypoint : waypoints) {
				manager.addWaypoint(waypoint.resolve(player), true);
			}
		}
	}
}