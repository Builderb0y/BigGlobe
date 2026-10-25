package builderb0y.bigglobe.networking.base;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;
import builderb0y.bigglobe.platform.fabric.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;

public interface S2CPlayPacketHandler<T> extends PacketHandler {

	@Environment(EnvType.CLIENT)
	public abstract T decode(
		FriendlyByteBuf buffer
	);

	@Environment(EnvType.CLIENT)
	public abstract void process(
		T data,
		PacketSender responseSender
	);
}