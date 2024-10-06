package builderb0y.bigglobe.networking.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import builderb0y.bigglobe.hyperspace.*;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.util.NbtIo2;
import builderb0y.bigglobe.util.TextCoding;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

/**
schema:
is hyperspace dimension (boolean).
number of worlds (varint).
worlds:
	registry key.
number of waypoints (varint).
waypoints:
	flags (byte).
		if the recipient of this packet is also the owner of the waypoint,
		then the {@link #OWNED_BY_RECIPIENT} flag is set.
		if the waypoint is public, then the {@link #OWNED_BY_RECIPIENT} flag is NOT set.
		if the waypoint has a name, then the {@link #HAS_NAME} flag is set.
	ID (varint).
	entity ID (varint).
	destination world ID (varint).
	destination packed X (int).
	destination packed Y (int).
	destination packed Z (int).
	if hyperspace:
		display position packed X (int).
		display position packed Y (int).
		display position packed Z (int).
	if hasName:
		name (NBT element).
*/
public class WaypointListS2CPacket implements S2CPlayPacketHandler<List<SyncedWaypointData>> {

	public static final WaypointListS2CPacket INSTANCE = new WaypointListS2CPacket();
	public static final int
		OWNED_BY_RECIPIENT = 1 << 0,
		HAS_NAME = 1 << 1;

	public void send(ServerPlayerEntity player) {
		PlayerWaypointManager manager = PlayerWaypointManager.get(player);
		if (manager == null) return;
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
			int flags = 0;
			if (waypoint.owner() != null) flags |= OWNED_BY_RECIPIENT;
			Text name = waypoint.destination().name();
			NbtElement nbtName = TextCoding.toNbt(name);
			if (nbtName != null) flags |= HAS_NAME;
			buffer.writeByte(flags);
			buffer.writeVarInt(waypoint.id());
			buffer.writeVarInt(waypoint.destination().entityId());
			waypoint.destinationPosition().writeBulk(buffer, worlds);
			if (isHyperspace) waypoint.displayPosition().writePositionOnly(buffer);
			if (nbtName != null) NbtIo2.write(buffer, nbtName);
		}

		BigGlobeNetwork.INSTANCE.sendToPlayer(player, buffer);
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
			int flags = buffer.readByte();
			boolean owned = (flags & OWNED_BY_RECIPIENT) != 0;
			boolean hasName = (flags & HAS_NAME) != 0;
			int id = buffer.readVarInt();
			int entityID = buffer.readVarInt();
			PackedWorldPos destination = PackedWorldPos.readBulk(buffer, worlds);
			PackedWorldPos displayPosition = isHyperspace ? PackedWorldPos.readPositionOnly(buffer, HyperspaceConstants.WORLD_KEY) : destination;
			Text name = hasName ? TextCoding.read(buffer) : null;
			waypoints.add(new SyncedWaypointData(id, entityID, owned, destination, displayPosition, name));
		}
		return waypoints;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(List<SyncedWaypointData> waypoints, PacketSender responseSender) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null) {
			EntityVersions.setPortalCooldown(player, 20);
			PlayerWaypointManager manager = PlayerWaypointManager.get(player);
			if (manager != null) {
				manager.clear();
				for (SyncedWaypointData waypoint : waypoints) {
					manager.addWaypoint(waypoint.resolve(player), true);
				}
			}
		}
	}
}