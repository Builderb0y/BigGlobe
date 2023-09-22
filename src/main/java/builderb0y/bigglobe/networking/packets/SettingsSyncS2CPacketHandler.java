package builderb0y.bigglobe.networking.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.settings.OverworldClientSettings;
import builderb0y.bigglobe.util.NbtIo2;
import builderb0y.bigglobe.versions.EntityVersions;

public class SettingsSyncS2CPacketHandler implements S2CPlayPacketHandler {

	public static final SettingsSyncS2CPacketHandler INSTANCE = new SettingsSyncS2CPacketHandler();

	@Override
	@Environment(EnvType.CLIENT)
	public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
		NbtElement nbt = NbtIo2.readCompressed(buffer);
		OverworldClientSettings settings;
		try {
			settings = BigGlobeAutoCodec.AUTO_CODEC.decode(OverworldClientSettings.NULLABLE_CODER, nbt, NbtOps.INSTANCE);
		}
		catch (DecodeException exception) {
			BigGlobeNetwork.LOGGER.error("", exception);
			return;
		}
		ClientState.settings = settings;
	}

	public void send(ServerPlayerEntity player) {
		OverworldClientSettings settings;
		if (EntityVersions.getServerWorld(player).getChunkManager().getChunkGenerator() instanceof BigGlobeOverworldChunkGenerator generator) {
			settings = OverworldClientSettings.of(
				generator.seed,
				generator.settings
			);
		}
		else {
			settings = null;
		}
		NbtElement nbt = BigGlobeAutoCodec.AUTO_CODEC.encode(OverworldClientSettings.NULLABLE_CODER, settings, NbtOps.INSTANCE);
		PacketByteBuf buffer = this.buffer();
		NbtIo2.writeCompressed(buffer, nbt);
		ServerPlayNetworking.send(player, BigGlobeNetwork.NETWORK_ID, buffer);
	}
}