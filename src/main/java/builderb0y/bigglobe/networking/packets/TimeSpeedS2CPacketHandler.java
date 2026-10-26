package builderb0y.bigglobe.networking.packets;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;
import builderb0y.bigglobe.platform.fabric.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.GameruleVersions;

public class TimeSpeedS2CPacketHandler implements S2CPlayPacketHandler<Double> {

	public static final TimeSpeedS2CPacketHandler INSTANCE = new TimeSpeedS2CPacketHandler();

	@Override
	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public Double decode(FriendlyByteBuf buffer) {
		return buffer.readDouble();
	}

	@Override
	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public void process(Double data, PacketSender responseSender) {
		ClientState.forEach((ClientState state) -> state.timeSpeed = data);
	}

	public void send(ServerPlayer player) {
		double speed = GameruleVersions.daylightCycleSpeed(EntityVersions.getServerWorld(player));
		FriendlyByteBuf buffer = this.buffer();
		buffer.writeDouble(speed);
		BigGlobeNetwork.INSTANCE.sendToPlayer(player, buffer);
	}
}