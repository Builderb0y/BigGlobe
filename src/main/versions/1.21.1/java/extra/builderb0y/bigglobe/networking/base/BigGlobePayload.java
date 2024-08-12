package builderb0y.bigglobe.networking.base;

import io.netty.buffer.Unpooled;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import builderb0y.bigglobe.BigGlobeMod;

public record BigGlobePayload(PacketByteBuf buffer) implements CustomPayload {

	public static final CustomPayload.Id<BigGlobePayload> ID = new CustomPayload.Id<>(BigGlobeMod.modID("payload"));
	public static final PacketCodec<RegistryByteBuf, BigGlobePayload> CODEC = new PacketCodec<RegistryByteBuf, BigGlobePayload>() {

		@Override
		public BigGlobePayload decode(RegistryByteBuf buffer) {
			PacketByteBuf copy = new PacketByteBuf(Unpooled.buffer(buffer.readableBytes()));
			copy.writeBytes(buffer);
			return new BigGlobePayload(copy);
		}

		@Override
		public void encode(RegistryByteBuf buffer, BigGlobePayload value) {
			buffer.writeBytes(value.buffer, 0, value.buffer.writerIndex());
		}
	};

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}