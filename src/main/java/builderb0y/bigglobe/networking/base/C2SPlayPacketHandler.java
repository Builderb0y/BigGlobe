package builderb0y.bigglobe.networking.base;

import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public interface C2SPlayPacketHandler<T> extends PacketHandler {

	public abstract T decode(
		ServerPlayerEntity player,
		PacketByteBuf buffer
	);

	public abstract void process(
		ServerPlayerEntity player,
		T data,
		PacketSender responseSender
	);
}