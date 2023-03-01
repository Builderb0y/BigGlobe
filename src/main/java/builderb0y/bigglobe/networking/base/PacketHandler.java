package builderb0y.bigglobe.networking.base;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import net.minecraft.network.PacketByteBuf;

public interface PacketHandler {

	public default byte getId() {
		return BigGlobeNetwork.INSTANCE.getId(this);
	}

	public default PacketByteBuf buffer() {
		PacketByteBuf buffer = PacketByteBufs.create();
		buffer.writeByte(this.getId());
		return buffer;
	}
}