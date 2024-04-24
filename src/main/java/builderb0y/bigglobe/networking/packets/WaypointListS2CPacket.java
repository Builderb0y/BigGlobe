package builderb0y.bigglobe.networking.packets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.hyperspace.WaypointManager.ClientWaypointManager;
import builderb0y.bigglobe.hyperspace.WaypointManager.ServerWaypointManager;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.versions.EntityVersions;

public class WaypointListS2CPacket implements S2CPlayPacketHandler<ClientWaypointManager> {

	public static final WaypointListS2CPacket INSTANCE = new WaypointListS2CPacket();

	@Override
	@Environment(EnvType.CLIENT)
	public ClientWaypointManager decode(PacketByteBuf buffer) {
		return ClientWaypointManager.fromByteBuffer(buffer);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(ClientWaypointManager data, PacketSender responseSender) {
		ClientWaypointManager.setInstance(data);
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null) player.setPortalCooldown(20);
	}

	public void send(ServerPlayerEntity player, Vec3d entrance) {
		ServerWaypointManager manager = ServerWaypointManager.get(EntityVersions.getServerWorld(player));
		if (manager != null) {
			PacketByteBuf buffer = this.buffer();
			manager.toByteBuffer(buffer, player, entrance);
			ServerPlayNetworking.send(player, BigGlobeNetwork.NETWORK_ID, buffer);
		}
	}
}