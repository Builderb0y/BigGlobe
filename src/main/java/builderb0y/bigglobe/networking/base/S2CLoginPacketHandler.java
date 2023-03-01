package builderb0y.bigglobe.networking.base;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvironmentInterface;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;

@EnvironmentInterface(value = EnvType.CLIENT, itf = ClientLoginNetworking.LoginQueryRequestHandler.class)
public interface S2CLoginPacketHandler extends PacketHandler, ClientLoginNetworking.LoginQueryRequestHandler {

	@Override
	@Environment(EnvType.CLIENT)
	public abstract CompletableFuture<@Nullable PacketByteBuf> receive(MinecraftClient client, ClientLoginNetworkHandler handler, PacketByteBuf buffer, Consumer<GenericFutureListener<? extends Future<? super Void>>> listenerAdder);
}