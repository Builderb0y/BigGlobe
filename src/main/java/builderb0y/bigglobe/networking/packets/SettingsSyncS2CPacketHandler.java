package builderb0y.bigglobe.networking.packets;

import java.util.Objects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.ClientState.TemplateRegistry;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.util.NbtIo2;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.scripting.parsing.ScriptParsingException;

public class SettingsSyncS2CPacketHandler implements S2CPlayPacketHandler<ClientGeneratorParams> {

	public static final SettingsSyncS2CPacketHandler INSTANCE = new SettingsSyncS2CPacketHandler();

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ClientGeneratorParams decode(PacketByteBuf buffer) {
		NbtElement nbt = NbtIo2.readCompressed(buffer);
		if (nbt instanceof NbtEnd) {
			return null;
		}
		NbtElement templates = Objects.requireNonNull(((NbtCompound)(nbt)).get("templates"), "Missing templates");
		TemplateRegistry templateRegistry;
		try {
			templateRegistry = BigGlobeAutoCodec.AUTO_CODEC.decode(TemplateRegistry.CODER, templates, NbtOps.INSTANCE);
		}
		catch (DecodeException exception) {
			BigGlobeNetwork.LOGGER.error("Exception decoding script templates: ", exception);
			throw new RuntimeException(exception);
		}
		ClientGeneratorParams settings;
		try {
			settings = BigGlobeAutoCodec.AUTO_CODEC.decode(ClientGeneratorParams.NULLABLE_CODER, nbt, templateRegistry.createOps(NbtOps.INSTANCE));
		}
		catch (DecodeException exception) {
			BigGlobeNetwork.LOGGER.error("Exception decoding client generator params: ", exception);
			throw new RuntimeException(exception);
		}
		try {
			settings.compile();
		}
		catch (ScriptParsingException exception) {
			BigGlobeNetwork.LOGGER.error("Exception compiling client generator params: ", exception);
			throw new RuntimeException(exception);
		}
		return settings;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(ClientGeneratorParams data, PacketSender responseSender) {
		ClientState.generatorParams = data;
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
		BigGlobeNetwork.INSTANCE.sendToPlayer(player, buffer);
	}
}