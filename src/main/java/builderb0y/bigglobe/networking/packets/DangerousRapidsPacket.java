package builderb0y.bigglobe.networking.packets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.gamerules.BigGlobeGameRules;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;

public class DangerousRapidsPacket implements S2CPlayPacketHandler {

	public static final DangerousRapidsPacket INSTANCE = new DangerousRapidsPacket();

	@Override
	@Environment(EnvType.CLIENT)
	public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
		ClientState.dangerousRapids = buffer.readBoolean();
	}

	public void send(ServerPlayerEntity player) {
		boolean dangerousRapids = player.server.getGameRules().get(BigGlobeGameRules.DANGEROUS_RAPIDS).get();
		PacketByteBuf buffer = this.buffer();
		buffer.writeBoolean(dangerousRapids);
		ServerPlayNetworking.send(player, BigGlobeNetwork.NETWORK_ID, buffer);
	}
}