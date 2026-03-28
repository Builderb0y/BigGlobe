package builderb0y.bigglobe.networking.packets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;

public class WaypointRemoveS2CPacket implements S2CPlayPacketHandler<Integer> {

	public static final WaypointRemoveS2CPacket INSTANCE = new WaypointRemoveS2CPacket();

	public void send(ServerPlayer player, int id) {
		FriendlyByteBuf buffer = this.buffer();
		buffer.writeVarInt(id);
		BigGlobeNetwork.INSTANCE.sendToPlayer(player, buffer);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Integer decode(FriendlyByteBuf buffer) {
		return buffer.readVarInt();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(Integer data, PacketSender responseSender) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			PlayerWaypointManager manager = PlayerWaypointManager.get(player);
			if (manager != null) {
				manager.removeWaypoint(data, true);
			}
		}
	}
}