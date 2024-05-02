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

public class DangerousRapidsPacket implements S2CPlayPacketHandler<Boolean> {

	public static final DangerousRapidsPacket INSTANCE = new DangerousRapidsPacket();

	@Override
	@Environment(EnvType.CLIENT)
	public Boolean decode(PacketByteBuf buffer) {
		return buffer.readBoolean();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(Boolean data, PacketSender responseSender) {
		ClientState.dangerousRapids = data;
	}

	public void send(ServerPlayerEntity player) {
		boolean dangerousRapids = player.server.getGameRules().get(BigGlobeGameRules.DANGEROUS_RAPIDS).get();
		PacketByteBuf buffer = this.buffer();
		buffer.writeBoolean(dangerousRapids);
		BigGlobeNetwork.INSTANCE.sendToPlayer(player, buffer);
	}
}