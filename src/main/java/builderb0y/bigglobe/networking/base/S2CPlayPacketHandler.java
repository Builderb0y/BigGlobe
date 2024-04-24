package builderb0y.bigglobe.networking.base;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.network.PacketByteBuf;

public interface S2CPlayPacketHandler<T> extends PacketHandler {

	@Environment(EnvType.CLIENT)
	public abstract T decode(
		PacketByteBuf buffer
	);


	@Environment(EnvType.CLIENT)
	public abstract void process(
		T data,
		PacketSender responseSender
	);
}