package builderb0y.bigglobe.platform.fabric.fabric.api.networking.v1;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class ServerPlayNetworking {

	public record Context(ServerPlayer player, PacketSender responseSender) {
		public MinecraftServer server() {
			return this.player.server;
		}
	}

	@FunctionalInterface
	public interface PlayPayloadHandler<T extends CustomPacketPayload> {
		void receive(T payload, Context context);
	}

	public static final Map<CustomPacketPayload.Type<?>, PlayPayloadHandler<?>> RECEIVERS = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static <T extends CustomPacketPayload> void registerGlobalReceiver(
		CustomPacketPayload.Type<T> type,
		PlayPayloadHandler<T> handler
	) {
		RECEIVERS.put(type, handler);
	}

	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}
}
