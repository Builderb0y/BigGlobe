package builderb0y.bigglobe.networking.base;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import builderb0y.bigglobe.BigGlobeMod;

public record BigGlobePayload(FriendlyByteBuf buffer) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<BigGlobePayload> ID = new CustomPacketPayload.Type<>(BigGlobeMod.modID("payload"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BigGlobePayload> CODEC = new StreamCodec<RegistryFriendlyByteBuf, BigGlobePayload>() {

		@Override
		public BigGlobePayload decode(RegistryFriendlyByteBuf buffer) {
			FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer(buffer.readableBytes()));
			copy.writeBytes(buffer);
			return new BigGlobePayload(copy);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, BigGlobePayload value) {
			buffer.writeBytes(value.buffer, 0, value.buffer.writerIndex());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}