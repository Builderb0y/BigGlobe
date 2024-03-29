package builderb0y.bigglobe.networking.packets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.util.NbtIo2;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.scripting.parsing.ScriptParsingException;

public class SettingsSyncS2CPacketHandler implements S2CPlayPacketHandler {

	public static final SettingsSyncS2CPacketHandler INSTANCE = new SettingsSyncS2CPacketHandler();

	@Override
	@Environment(EnvType.CLIENT)
	public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buffer, PacketSender responseSender) {
		NbtElement nbt = NbtIo2.readCompressed(buffer);
		ClientGeneratorParams settings;
		try {
			settings = BigGlobeAutoCodec.AUTO_CODEC.decode(ClientGeneratorParams.NULLABLE_CODER, nbt, NbtOps.INSTANCE);
		}
		catch (DecodeException exception) {
			BigGlobeNetwork.LOGGER.error("", exception);
			throw new RuntimeException(exception);
		}
		try {
			settings.compile();
		}
		catch (ScriptParsingException exception) {
			BigGlobeNetwork.LOGGER.error("Server sent scripts that failed to be compiled: ", exception);
			throw new RuntimeException(exception);
		}
		ClientState.generatorParams = settings;
	}

	public void send(ServerPlayerEntity player) {
		ClientGeneratorParams params;
		if (EntityVersions.getServerWorld(player).getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			params = new ClientGeneratorParams(generator);
		}
		else {
			params = null;
		}
		NbtElement nbt = BigGlobeAutoCodec.AUTO_CODEC.encode(ClientGeneratorParams.NULLABLE_CODER, params, NbtOps.INSTANCE);
		PacketByteBuf buffer = this.buffer();
		NbtIo2.writeCompressed(buffer, nbt);
		ServerPlayNetworking.send(player, BigGlobeNetwork.NETWORK_ID, buffer);
	}
}