package builderb0y.bigglobe.networking.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.netty.util.concurrent.GenericFutureListener;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginSynchronizer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.networking.packets.TimeSpeedS2CPacketHandler;
import builderb0y.bigglobe.networking.packets.SettingsSyncS2CPacketHandler;

public class BigGlobeNetwork implements C2SLoginPacketHandler, C2SPlayPacketHandler, S2CLoginPacketHandler, S2CPlayPacketHandler {

	public static final Identifier NETWORK_ID = BigGlobeMod.modID("network");
	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Network");
	public static final BigGlobeNetwork INSTANCE = new BigGlobeNetwork();

	public final List<PacketHandler> idToHandler = new ArrayList<>(1);
	public final Object2ByteMap<PacketHandler> handlerToId = new Object2ByteOpenHashMap<>(1);

	public BigGlobeNetwork() {
		this.handlerToId.defaultReturnValue((byte)(-1));
		this.register(SettingsSyncS2CPacketHandler.INSTANCE);
		this.register(TimeSpeedS2CPacketHandler.INSTANCE);
	}

	public byte nextId() {
		int id = this.idToHandler.size();
		if (id < 255) return (byte)(id);
		else throw new IllegalStateException("Too many packet handlers registered on " + this);
	}

	public void register(PacketHandler handler) {
		byte id = this.nextId();
		this.idToHandler.add(handler);
		this.handlerToId.put(handler, id);
	}

	public byte getId(PacketHandler handler) {
		byte id = this.handlerToId.getByte(handler);
		if (id != -1) return id;
		else throw new IllegalStateException(handler + " not registered on " + this);
	}

	public PacketHandler getHandler(byte id) {
		int unsignedId = Byte.toUnsignedInt(id);
		if (unsignedId < this.idToHandler.size()) return this.idToHandler.get(unsignedId);
		else return null;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public CompletableFuture<@Nullable PacketByteBuf> receive(MinecraftClient client, ClientLoginNetworkHandler handler, PacketByteBuf buffer, Consumer<GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>>> listenerAdder) {
		byte id = buffer.readByte();
		if (this.getHandler(id) instanceof S2CLoginPacketHandler packetHandler) {
			return packetHandler.receive(client, handler, buffer, listenerAdder);
		}
		else {
			LOGGER.warn("No server to client login packet handler registered for ID " + Byte.toUnsignedInt(id));
			return null;
		}
	}

	@Override
	public void receive(MinecraftServer server, ServerLoginNetworkHandler handler, boolean understood, PacketByteBuf buffer, LoginSynchronizer synchronizer, PacketSender responseSender) {
		byte id = buffer.readByte();
		if (this.getHandler(id) instanceof C2SLoginPacketHandler packetHandler) {
			packetHandler.receive(server, handler, understood, buffer, synchronizer, responseSender);
		}
		else {
			LOGGER.warn("No client to server login packet handler registered for ID " + Byte.toUnsignedInt(id));
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void receive(MinecraftClient client, ClientPlayNetworkHandler networkHandler, PacketByteBuf buffer, PacketSender responseSender) {
		byte id = buffer.readByte();
		if (this.getHandler(id) instanceof S2CPlayPacketHandler packetHandler) {
			packetHandler.receive(client, networkHandler, buffer, responseSender);
		}
		else {
			LOGGER.warn("No server to client play packet handler registered for ID " + Byte.toUnsignedInt(id));
		}
	}

	@Override
	public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler networkHandler, PacketByteBuf buffer, PacketSender responseSender) {
		byte id = buffer.readByte();
		if (this.getHandler(id) instanceof C2SPlayPacketHandler packetHandler) {
			packetHandler.receive(server, player, networkHandler, buffer, responseSender);
		}
		else {
			LOGGER.warn("No client to server play packet handler registered for ID " + Byte.toUnsignedInt(id));
		}
	}

	public static void init() {
		LOGGER.debug("Initializing common network...");
		ServerLoginNetworking.registerGlobalReceiver(NETWORK_ID, INSTANCE);
		ServerPlayNetworking.registerGlobalReceiver(NETWORK_ID, INSTANCE);
		LOGGER.debug("Done initializing common network.");
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		LOGGER.debug("Initializing client network...");
		ClientLoginNetworking.registerGlobalReceiver(NETWORK_ID, INSTANCE);
		ClientPlayNetworking.registerGlobalReceiver(NETWORK_ID, INSTANCE);
		LOGGER.debug("Done initializing client network.");
	}
}