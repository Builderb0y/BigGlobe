package builderb0y.bigglobe.networking.base;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.networking.packets.*;

public class BigGlobeNetwork implements ClientPlayNetworking.PlayChannelHandler, ServerPlayNetworking.PlayChannelHandler {

	public static final Identifier NETWORK_ID = BigGlobeMod.modID("network");
	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Network");
	public static final BigGlobeNetwork INSTANCE = new BigGlobeNetwork();

	public final List<PacketHandler> idToHandler = new ArrayList<>(2);
	public final Object2ByteMap<PacketHandler> handlerToId = new Object2ByteOpenHashMap<>(2);

	public BigGlobeNetwork() {
		this.handlerToId.defaultReturnValue((byte)(-1));
		this.register(SettingsSyncS2CPacketHandler.INSTANCE);
		this.register(TimeSpeedS2CPacketHandler.INSTANCE);
		this.register(DangerousRapidsPacket.INSTANCE);
		this.register(WaypointListS2CPacket.INSTANCE);
		this.register(WaypointAddS2CPacket.INSTANCE);
		this.register(WaypointRemoveS2CPacket.INSTANCE);
		this.register(UseWaypointPacket.INSTANCE);
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
	public void receive(
		MinecraftClient client,
		ClientPlayNetworkHandler networkHandler,
		PacketByteBuf buffer,
		PacketSender responseSender
	) {
		byte id = buffer.readByte();
		if (this.getHandler(id) instanceof S2CPlayPacketHandler<?> packetHandler) {
			this.doReceive(client, buffer, responseSender, packetHandler);
		}
		else {
			LOGGER.warn("No server to client play packet handler registered for ID " + Byte.toUnsignedInt(id));
		}
	}

	@Environment(EnvType.CLIENT)
	public <T> void doReceive(
		MinecraftClient client,
		PacketByteBuf buffer,
		PacketSender responseSender,
		S2CPlayPacketHandler<T> handler
	) {
		T data = handler.decode(buffer);
		client.executeSync(() -> {
			handler.process(data, responseSender);
		});
	}

	@Override
	public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler networkHandler, PacketByteBuf buffer, PacketSender responseSender) {
		byte id = buffer.readByte();
		if (this.getHandler(id) instanceof C2SPlayPacketHandler<?> packetHandler) {
			this.doReceive(server, player, buffer, responseSender, packetHandler);
		}
		else {
			LOGGER.warn("No client to server play packet handler registered for ID " + Byte.toUnsignedInt(id));
		}
	}

	public <T> void doReceive(
		MinecraftServer server,
		ServerPlayerEntity player,
		PacketByteBuf buffer,
		PacketSender responseSender,
		C2SPlayPacketHandler<T> handler
	) {
		T data = handler.decode(player, buffer);
		server.executeSync(() -> {
			handler.process(player, data, responseSender);
		});
	}

	public static void init() {
		LOGGER.debug("Initializing common network...");
		ServerPlayNetworking.registerGlobalReceiver(NETWORK_ID, INSTANCE);
		LOGGER.debug("Done initializing common network.");
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		LOGGER.debug("Initializing client network...");
		ClientPlayNetworking.registerGlobalReceiver(NETWORK_ID, INSTANCE);
		LOGGER.debug("Done initializing client network.");
	}
}