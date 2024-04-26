package builderb0y.bigglobe.networking.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.*;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;
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
public class WaypointListS2CPacket implements S2CPlayPacketHandler<ClientWaypointManager> {

	public static final WaypointListS2CPacket INSTANCE = new WaypointListS2CPacket();

	public void recompute(ServerPlayerEntity player, ServerWaypointData entrance) {
		ServerWaypointManager serverManager = ServerWaypointManager.get(EntityVersions.getServerWorld(player));
		if (serverManager == null) return;
		ClientWaypointManager newManager = new ClientWaypointManager(entrance);
		PackedPosition entrancePosition = entrance != null ? entrance.position() : null;
		serverManager.getRelevantWaypoints(
			player.getGameProfile().getId(),
			player.getWorld().getRegistryKey()
		)
		.forEachOrdered((ServerWaypointData waypoint) -> {
			newManager.addWaypoint(waypoint.toClientData(entrancePosition), false);
		});
		((WaypointTracker)(player)).bigglobe_setWaypointManager(newManager);
	}

	public void send(ServerPlayerEntity player) {
		ClientWaypointManager manager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
		PacketByteBuf buffer = this.buffer();
		buffer.writeUuid(player.getGameProfile().getId());
		boolean isHyperspace = player.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY;
		buffer.writeBoolean(isHyperspace);
		Object2IntMap<RegistryKey<World>> worlds = new Object2IntLinkedOpenHashMap<>(4);
		worlds.defaultReturnValue(-1);
		for (ServerWorld world : BigGlobeMod.getCurrentServer().getWorlds()) {
			worlds.put(world.getRegistryKey(), worlds.size());
		}
		buffer.writeVarInt(worlds.size());
		for (Object2IntMap.Entry<RegistryKey<World>> entry : worlds.object2IntEntrySet()) {
			buffer.writeRegistryKey(entry.getKey());
		}

		//if the waypoint is modified using external NBT editor tools,
		//or if a world disappears while the server is running
		//(do mystcraft-like mods do that?), then our count will be inaccurate.
		//so we need to pre-filter all waypoints before syncing them to the client.
		int waypointCount = Math.toIntExact(manager.getAllWaypoints().filter((ClientWaypointData waypoint) -> worlds.containsKey(waypoint.world())).count());
		buffer.writeVarInt(waypointCount);
		manager.getAllWaypoints().filter((ClientWaypointData waypoint) -> worlds.containsKey(waypoint.world())).forEach((ClientWaypointData waypoint) -> {
			buffer.writeBoolean(waypoint.owner() != null);
			buffer.writeVarInt(worlds.getInt(waypoint.world()));
			waypoint.destination().position().write(buffer);
			buffer.writeUuid(waypoint.uuid());
			if (isHyperspace) waypoint.clientPosition().write(buffer);
		});

		ServerPlayNetworking.send(player, BigGlobeNetwork.NETWORK_ID, buffer);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ClientWaypointManager decode(PacketByteBuf buffer) {
		UUID playerUUID = buffer.readUuid();

		ClientWaypointManager manager = new ClientWaypointManager(null);
		boolean isHyperspace = buffer.readBoolean();
		int numberOfWorlds = buffer.readVarInt();
		List<RegistryKey<World>> worlds = new ArrayList<>(numberOfWorlds);
		for (int worldIndex = 0; worldIndex < numberOfWorlds; worldIndex++) {
			worlds.add(buffer.readRegistryKey(RegistryKeyVersions.world()));
		}
		int numberOfWaypoints = buffer.readVarInt();
		for (int waypointIndex = 0; waypointIndex < numberOfWaypoints; waypointIndex++) {
			boolean isPrivate = buffer.readBoolean();
			UUID owner = isPrivate ? playerUUID : null;
			RegistryKey<World> world = worlds.get(buffer.readVarInt());
			PackedPosition destinationPosition = PackedPosition.read(buffer);
			UUID uuid = buffer.readUuid();
			PackedPosition clientPosition = isHyperspace ? PackedPosition.read(buffer) : destinationPosition;
			ServerWaypointData serverWaypoint = new ServerWaypointData(world, destinationPosition, uuid, owner);
			ClientWaypointData waypoint = new ClientWaypointData(serverWaypoint, clientPosition);
			manager.addWaypoint(waypoint, false);
		}
		return manager;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(ClientWaypointManager data, PacketSender responseSender) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null) {
			player.setPortalCooldown(20);
			ClientWaypointManager.setOnClient(player, data);
		}
	}
}