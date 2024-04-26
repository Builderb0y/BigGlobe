package builderb0y.bigglobe.networking.packets;

import java.util.UUID;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;

public class WaypointRemoveS2CPacket implements S2CPlayPacketHandler<WaypointRemoveS2CPacket.Data> {

	public static final WaypointRemoveS2CPacket INSTANCE = new WaypointRemoveS2CPacket();

	public void send(ServerPlayerEntity player, UUID uuid, boolean owned) {
		PacketByteBuf buffer = this.buffer();
		buffer.writeUuid(uuid);
		buffer.writeBoolean(owned);
		ServerPlayNetworking.send(player, BigGlobeNetwork.NETWORK_ID, buffer);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Data decode(PacketByteBuf buffer) {
		return new Data(buffer.readUuid(), buffer.readBoolean());
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(Data data, PacketSender responseSender) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null) {
			((WaypointTracker)(player)).bigglobe_getWaypointManager().removeWaypoint(data.owned ? player.getGameProfile().getId() : null, data.uuid, true);
		}
	}

	public static record Data(UUID uuid, boolean owned) {}
}