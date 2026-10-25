package net.fabricmc.fabric.api.networking.v1;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface PacketSender {

	default void sendPacket(CustomPacketPayload payload) {
	}

	default void sendPacket(Packet<?> packet) {
	}

	default FriendlyByteBuf createPacketByteBuf() {
		return PacketByteBufs.create();
	}
}
