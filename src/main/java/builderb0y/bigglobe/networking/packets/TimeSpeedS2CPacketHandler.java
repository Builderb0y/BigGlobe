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
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.gamerules.BigGlobeGameRules;

public class TimeSpeedS2CPacketHandler implements S2CPlayPacketHandler {

	public static final TimeSpeedS2CPacketHandler INSTANCE = new TimeSpeedS2CPacketHandler();

	@Override
	@Environment(EnvType.CLIENT)
	public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
		ClientState.timeSpeed = buffer.readDouble();
	}

	public void send(ServerPlayerEntity player) {
		double speed = player.server.getGameRules().get(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED).get();
		PacketByteBuf buffer = this.buffer();
		buffer.writeDouble(speed);
		ServerPlayNetworking.send(player, BigGlobeNetwork.NETWORK_ID, buffer);
	}
}