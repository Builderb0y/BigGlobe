package builderb0y.bigglobe.platform.fabric.fabric.api.networking.v1;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public class PacketByteBufs {

	public static FriendlyByteBuf create() {
		return new FriendlyByteBuf(Unpooled.buffer());
	}
}
