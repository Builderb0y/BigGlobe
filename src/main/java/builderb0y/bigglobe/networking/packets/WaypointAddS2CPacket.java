package builderb0y.bigglobe.networking.packets;

import java.util.UUID;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;

import builderb0y.bigglobe.hyperspace.PackedWorldPos;
import builderb0y.bigglobe.hyperspace.ServerPlayerWaypointManager;
import builderb0y.bigglobe.hyperspace.SyncedWaypointData;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;

public class WaypointAddS2CPacket implements S2CPlayPacketHandler<SyncedWaypointData> {

	public static final WaypointAddS2CPacket INSTANCE = new WaypointAddS2CPacket();
	public static final int
		HAS_OWNER = 1 << 0,
		HAS_DISPLAYED_POSITION = 1 << 1;

	public void send(ServerPlayerWaypointManager manager, SyncedWaypointData waypoint) {
		int flags = 0;
		if (waypoint.owned()) flags |= HAS_OWNER;
		if (manager.entrance != null) flags |= HAS_DISPLAYED_POSITION;

		PacketByteBuf buffer = this.buffer();
		buffer.writeByte(flags);
		buffer.writeUuid(waypoint.uuid());
		waypoint.destinationPosition().write(buffer);
		if (manager.entrance != null) waypoint.displayedPosition().write(buffer);

		ServerPlayNetworking.send(manager.serverPlayer(), BigGlobeNetwork.NETWORK_ID, buffer);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public SyncedWaypointData decode(PacketByteBuf buffer) {
		int flags = buffer.readByte();
		boolean owned = (flags & HAS_OWNER) != 0;
		boolean hasDisplayedPosition = (flags & HAS_DISPLAYED_POSITION) != 0;
		UUID uuid = buffer.readUuid();
		PackedWorldPos destination = PackedWorldPos.read(buffer);
		PackedWorldPos displayedPosition = hasDisplayedPosition ? PackedWorldPos.read(buffer) : destination;
		return new SyncedWaypointData(uuid, owned, destination, displayedPosition);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(SyncedWaypointData waypoint, PacketSender responseSender) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		((WaypointTracker)(player)).bigglobe_getWaypointManager().addWaypoint(waypoint.resolve(player), true);
	}
}