package builderb0y.bigglobe.networking.packets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.gamerules.BigGlobeGameRules;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;

public class TimeSpeedS2CPacketHandler implements S2CPlayPacketHandler<Double> {

	public static final TimeSpeedS2CPacketHandler INSTANCE = new TimeSpeedS2CPacketHandler();

	@Override
	@Environment(EnvType.CLIENT)
	public Double decode(PacketByteBuf buffer) {
		return buffer.readDouble();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(Double data, PacketSender responseSender) {
		ClientState.timeSpeed = data;
	}

	public void send(ServerPlayerEntity player) {
		double speed = player.server.getGameRules().get(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED).get();
		PacketByteBuf buffer = this.buffer();
		buffer.writeDouble(speed);
		BigGlobeNetwork.INSTANCE.sendToPlayer(player, buffer);
	}
}