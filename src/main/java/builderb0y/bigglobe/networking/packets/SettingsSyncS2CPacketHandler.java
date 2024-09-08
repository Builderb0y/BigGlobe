package builderb0y.bigglobe.networking.packets;

import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.ClientState.Syncing;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyDepthSorter;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.util.NbtIo2;
import builderb0y.bigglobe.versions.EntityVersions;

public class SettingsSyncS2CPacketHandler implements S2CPlayPacketHandler<ClientGeneratorParams> {

	public static final SettingsSyncS2CPacketHandler INSTANCE = new SettingsSyncS2CPacketHandler();

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ClientGeneratorParams decode(PacketByteBuf buffer) {
		try {
			GZIPInputStream stream = new GZIPInputStream(new ByteBufInputStream(buffer));
			NbtElement syncingNbt = NbtIo2.read(stream);
			NbtElement paramsNbt = NbtIo2.read(stream);
			if (syncingNbt == NbtEnd.INSTANCE) {
				if (paramsNbt == NbtEnd.INSTANCE) return null;
				else throw new IllegalStateException("Received params NBT, but not syncing NBT?");
			}
			Syncing syncing = BigGlobeAutoCodec.AUTO_CODEC.decode(Syncing.CODER, syncingNbt, NbtOps.INSTANCE);
			return ColumnEntryRegistry.Loading.OVERRIDE.apply(new ColumnEntryRegistry.Loading(syncing.lookup()), (ColumnEntryRegistry.Loading loading) -> {
				syncing.parse();
				ClientGeneratorParams params = BigGlobeAutoCodec.AUTO_CODEC.decode(ClientGeneratorParams.NULLABLE_CODER, paramsNbt, syncing.createOps(NbtOps.INSTANCE, false));
				if (params != null) params.compile(loading);
				return params;
			});
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Exception decoding client generator params:", exception);
			throw new RuntimeException(exception);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(ClientGeneratorParams data, PacketSender responseSender) {
		ClientState.generatorParams = data;
		DependencyDepthSorter.start(data.compiledWorldTraits, data.columnEntryRegistry.registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY), "client");
	}

	public void send(ServerPlayerEntity player) {
		ClientState.Syncing syncing;
		ClientGeneratorParams params;
		if (EntityVersions.getServerWorld(player).getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			syncing = new ClientState.Syncing(generator);
			params = new ClientGeneratorParams(generator, syncing);
		}
		else {
			syncing = null;
			params = null;
		}
		NbtElement syncingNbt = BigGlobeAutoCodec.AUTO_CODEC.encode(ClientState.Syncing.CODER, syncing, NbtOps.INSTANCE);
		NbtElement paramsNbt = BigGlobeAutoCodec.AUTO_CODEC.encode(ClientGeneratorParams.NULLABLE_CODER, params, NbtOps.INSTANCE);
		PacketByteBuf buffer = this.buffer();
		try {
			GZIPOutputStream stream = new GZIPOutputStream(new ByteBufOutputStream(buffer));
			NbtIo2.write(stream, syncingNbt);
			NbtIo2.write(stream, paramsNbt);
			stream.finish();
			BigGlobeNetwork.INSTANCE.sendToPlayer(player, buffer);
		}
		catch (IOException exception) {
			throw new AssertionError("ByteBufOutputStream threw an IOException?", exception);
		}
	}
}