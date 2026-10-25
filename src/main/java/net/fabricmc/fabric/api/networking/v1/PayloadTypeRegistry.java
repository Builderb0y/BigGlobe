package net.fabricmc.fabric.api.networking.v1;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface PayloadTypeRegistry<B> {

	<T extends CustomPacketPayload> CustomPacketPayload.Type<T> register(
		CustomPacketPayload.Type<T> type,
		StreamCodec<? super B, T> codec
	);

	class Impl<B> implements PayloadTypeRegistry<B> {
		public final Map<CustomPacketPayload.Type<?>, StreamCodec<? super B, ?>> payloads = new HashMap<>();

		@Override
		@SuppressWarnings("unchecked")
		public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> register(
			CustomPacketPayload.Type<T> type,
			StreamCodec<? super B, T> codec
		) {
			this.payloads.put(type, codec);
			return type;
		}
	}

	Impl<RegistryFriendlyByteBuf> PLAY_C2S = new Impl<>();
	Impl<RegistryFriendlyByteBuf> PLAY_S2C = new Impl<>();

	static PayloadTypeRegistry<RegistryFriendlyByteBuf> playC2S() {
		return PLAY_C2S;
	}

	static PayloadTypeRegistry<RegistryFriendlyByteBuf> playS2C() {
		return PLAY_S2C;
	}
}
