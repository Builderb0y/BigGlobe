package builderb0y.bigglobe.networking.base;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginSynchronizer;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;

public interface C2SLoginPacketHandler extends PacketHandler, ServerLoginNetworking.LoginQueryResponseHandler {

	@Override
	public abstract void receive(MinecraftServer server, ServerLoginNetworkHandler handler, boolean understood, PacketByteBuf buffer, LoginSynchronizer synchronizer, PacketSender responseSender);
}