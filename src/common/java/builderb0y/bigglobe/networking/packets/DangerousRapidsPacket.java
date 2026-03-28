package builderb0y.bigglobe.networking.packets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.GameruleVersions;

public class DangerousRapidsPacket implements S2CPlayPacketHandler<Boolean> {

	public static final DangerousRapidsPacket INSTANCE = new DangerousRapidsPacket();

	@Override
	@Environment(EnvType.CLIENT)
	public Boolean decode(FriendlyByteBuf buffer) {
		return buffer.readBoolean();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(Boolean data, PacketSender responseSender) {
		ClientState.forEach((ClientState state) -> state.dangerousRapids = data);
	}

	public void send(ServerPlayer player) {
		boolean dangerousRapids = GameruleVersions.dangerousRapids(EntityVersions.getServerWorld(player));
		FriendlyByteBuf buffer = this.buffer();
		buffer.writeBoolean(dangerousRapids);
		BigGlobeNetwork.INSTANCE.sendToPlayer(player, buffer);
	}
}