package builderb0y.bigglobe.networking.base;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

public interface PacketHandler {

	public default byte getId() {
		return BigGlobeNetwork.INSTANCE.getId(this);
	}

	public default FriendlyByteBuf buffer() {
		FriendlyByteBuf buffer = PacketByteBufs.create();
		buffer.writeByte(this.getId());
		return buffer;
	}
}