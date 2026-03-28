package builderb0y.bigglobe.networking.base;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface C2SPlayPacketHandler<T> extends PacketHandler {

	public abstract T decode(
		ServerPlayer player,
		FriendlyByteBuf buffer
	);

	public abstract void process(
		ServerPlayer player,
		T data,
		PacketSender responseSender
	);
}