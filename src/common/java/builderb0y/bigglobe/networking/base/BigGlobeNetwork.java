package builderb0y.bigglobe.networking.base;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvironmentInterface;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.networking.packets.*;
import builderb0y.bigglobe.versions.EntityVersions;

@EnvironmentInterface(value = EnvType.CLIENT, itf = ClientPlayNetworking.PlayPayloadHandler.class)
public class BigGlobeNetwork implements
	ClientPlayNetworking.PlayPayloadHandler<BigGlobePayload>,
		ServerPlayNetworking.PlayPayloadHandler<BigGlobePayload> {

	public static final Identifier NETWORK_ID = BigGlobeMod.modID("network");
	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Network");
	public static final BigGlobeNetwork INSTANCE = new BigGlobeNetwork();

	public final List<PacketHandler> idToHandler = new ArrayList<>(16);
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
		this.register(WaypointRenameC2SPacket.INSTANCE);
		this.register(WaypointRemoveC2SPacket.INSTANCE);
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

	public @Nullable PacketHandler getHandler(byte id) {
		int unsignedId = Byte.toUnsignedInt(id);
		if (unsignedId < this.idToHandler.size()) return this.idToHandler.get(unsignedId);
		else return null;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void receive(BigGlobePayload payload, ClientPlayNetworking.Context context) {
		byte id = payload.buffer().readByte();
		if (this.getHandler(id) instanceof S2CPlayPacketHandler<?> packetHandler) {
			this.doReceive(context.client(), payload.buffer(), context.responseSender(), packetHandler);
		}
		else {
			LOGGER.warn("No server to client play packet handler registered for ID " + Byte.toUnsignedInt(id));
		}
	}

	@Override
	public void receive(BigGlobePayload payload, ServerPlayNetworking.Context context) {
		byte id = payload.buffer().readByte();
		if (this.getHandler(id) instanceof C2SPlayPacketHandler<?> packetHandler) {
			this.doReceive(EntityVersions.getServer(context.player()), context.player(), payload.buffer(), context.responseSender(), packetHandler);
		}
		else {
			LOGGER.warn("No client to server play packet handler registered for ID " + Byte.toUnsignedInt(id));
		}
	}

	@Environment(EnvType.CLIENT)
	public <T> void doReceive(
		Minecraft client,
		FriendlyByteBuf buffer,
		PacketSender responseSender,
		S2CPlayPacketHandler<T> handler
	) {

		handler.process(handler.decode(buffer), responseSender);
	}

	public <T> void doReceive(
		MinecraftServer server,
		ServerPlayer player,
		FriendlyByteBuf buffer,
		PacketSender responseSender,
		C2SPlayPacketHandler<T> handler
	) {

		handler.process(player, handler.decode(player, buffer), responseSender);
	}

	public void sendToPlayer(ServerPlayer player, FriendlyByteBuf buffer) {
		ServerPlayNetworking.send(player, new BigGlobePayload(buffer));
	}

	public void sendToServer(FriendlyByteBuf buffer) {
		ClientPlayNetworking.send(new BigGlobePayload(buffer));
	}

	public static void init() {
		LOGGER.debug("Initializing common network...");

		PayloadTypeRegistry.serverboundPlay().register(BigGlobePayload.ID, BigGlobePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BigGlobePayload.ID, BigGlobePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(BigGlobePayload.ID, INSTANCE);
		LOGGER.debug("Done initializing common network.");
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		LOGGER.debug("Initializing client network...");
		ClientPlayNetworking.registerGlobalReceiver(BigGlobePayload.ID, INSTANCE);
		LOGGER.debug("Done initializing client network.");
	}
}