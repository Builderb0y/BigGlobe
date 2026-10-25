package builderb0y.bigglobe.platform.fabric.fabric.api.client.networking.v1;

import java.util.HashMap;
import java.util.Map;
import builderb0y.bigglobe.platform.fabric.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClientPlayNetworking {

	public record Context(Minecraft client, PacketSender responseSender) {
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

	public static void send(CustomPacketPayload payload) {
		PacketDistributor.sendToServer(payload);
	}
}
