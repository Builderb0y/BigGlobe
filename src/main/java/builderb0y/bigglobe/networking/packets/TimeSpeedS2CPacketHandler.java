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
import builderb0y.bigglobe.versions.EntityVersions;

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
		ClientState.forEach((ClientState state) -> state.timeSpeed = data);
	}

	public void send(ServerPlayerEntity player) {
		double speed = EntityVersions.getServerWorld(player).getGameRules().get(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED).get();
		PacketByteBuf buffer = this.buffer();
		buffer.writeDouble(speed);
		BigGlobeNetwork.INSTANCE.sendToPlayer(player, buffer);
	}
}