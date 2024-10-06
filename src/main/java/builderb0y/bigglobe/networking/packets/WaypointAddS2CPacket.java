package builderb0y.bigglobe.networking.packets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import builderb0y.bigglobe.hyperspace.*;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.util.NbtIo2;
import builderb0y.bigglobe.util.TextCoding;

/**
schema:
flags (byte).
	if the added waypoint is owned by the recipient of this packet,
	then the flags byte has the {@link #HAS_OWNER} bit set.
	if the recipient of this packet is currently in hyperspace,
	and therefore the displayed position of the waypoint does not match its destination position,
	then the {@link #HAS_DISPLAYED_POSITION} bit is set.
	if the added waypoint has a name, then the {@link #HAS_NAME} bit is set.
ID of the added waypoint (varint).
entity ID (varint).
the waypoint's destination dimension (RegistryKey<World>).
the waypoint's destination position's packed X coordinate (int).
the waypoint's destination position's packed Y coordinate (int).
the waypoint's destination position's packed Z coordinate (int).
if the {@link #HAS_DISPLAYED_POSITION} bit is set:
	the waypoint's display position's packed X coordinate (int).
	the waypoint's display position's packed Y coordinate (int).
	the waypoint's display position's packed Z coordinate (int).
if hasName:
	name (NBT element).
*/
public class WaypointAddS2CPacket implements S2CPlayPacketHandler<SyncedWaypointData> {

	public static final WaypointAddS2CPacket INSTANCE = new WaypointAddS2CPacket();
	public static final int
		HAS_OWNER = 1 << 0,
		HAS_DISPLAYED_POSITION = 1 << 1,
		HAS_NAME = 1 << 2;

	public void send(ServerPlayerWaypointManager manager, SyncedWaypointData waypoint) {
		int flags = 0;
		if (waypoint.owned()) flags |= HAS_OWNER;
		if (manager.entrance != null) flags |= HAS_DISPLAYED_POSITION;
		NbtElement name = TextCoding.toNbt(waypoint.name());
		if (name != null) flags |= HAS_NAME;

		PacketByteBuf buffer = this.buffer();
		buffer.writeByte(flags);
		buffer.writeVarInt(waypoint.id());
		buffer.writeVarInt(waypoint.entityId());
		waypoint.destinationPosition().write(buffer);
		if (manager.entrance != null) waypoint.displayedPosition().writePositionOnly(buffer);
		if (name != null) NbtIo2.write(buffer, name);

		BigGlobeNetwork.INSTANCE.sendToPlayer(manager.serverPlayer(), buffer);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public SyncedWaypointData decode(PacketByteBuf buffer) {
		int flags = buffer.readByte();
		boolean owned = (flags & HAS_OWNER) != 0;
		boolean hasDisplayedPosition = (flags & HAS_DISPLAYED_POSITION) != 0;
		boolean hasName = (flags & HAS_NAME) != 0;
		int id = buffer.readVarInt();
		int entityID = buffer.readVarInt();
		PackedWorldPos destination = PackedWorldPos.read(buffer);
		PackedWorldPos displayedPosition = hasDisplayedPosition ? PackedWorldPos.readPositionOnly(buffer, HyperspaceConstants.WORLD_KEY) : destination;
		Text name = hasName ? TextCoding.read(buffer) : null;
		return new SyncedWaypointData(id, entityID, owned, destination, displayedPosition, name);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(SyncedWaypointData waypoint, PacketSender responseSender) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null) {
			PlayerWaypointManager manager = PlayerWaypointManager.get(player);
			if (manager != null) {
				manager.addWaypoint(waypoint.resolve(player), true);
			}
		}
	}
}