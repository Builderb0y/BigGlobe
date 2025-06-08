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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyDepthSorter;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.lods.LodSystem;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.util.NbtIo2;
import builderb0y.bigglobe.versions.EntityVersions;

public class SettingsSyncS2CPacketHandler implements S2CPlayPacketHandler<SettingsSyncS2CPacketHandler.Receiving> {

	public static final SettingsSyncS2CPacketHandler INSTANCE = new SettingsSyncS2CPacketHandler();

	/**
	note: some received scripts depend on biome information,
	which is stored in the dynamic registries on the world object.
	as such, compilation must be delayed until the world is set.
	which means I can't call {@link #compile()} on the network thread.
	*/
	public static class Receiving {

		/** contains registry data needed by paramsNbt. */
		public final ClientState.Syncing clientState;
		/** contains usage of registry data. */
		public final NbtElement paramsNbt;

		public Receiving(ClientState.Syncing clientState, NbtElement paramsNbt) {
			this.clientState = clientState;
			this.paramsNbt = paramsNbt;
		}

		public ClientGeneratorParams compile() throws Exception {
			return ColumnEntryRegistry.Loading.OVERRIDE.apply(new ColumnEntryRegistry.Loading(this.clientState.lookup(), true), (ColumnEntryRegistry.Loading loading) -> {
				this.clientState.parse();
				ClientGeneratorParams params = BigGlobeAutoCodec.AUTO_CODEC.decode(ClientGeneratorParams.NULLABLE_CODER, this.paramsNbt, this.clientState.createOps(NbtOps.INSTANCE, false));
				if (params != null) params.compile(loading);
				return params;
			});
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable Receiving decode(PacketByteBuf buffer) {
		try {
			GZIPInputStream stream = new GZIPInputStream(new ByteBufInputStream(buffer));
			NbtElement syncingNbt = NbtIo2.read(stream);
			NbtElement paramsNbt = NbtIo2.read(stream);
			if (syncingNbt == NbtEnd.INSTANCE) {
				if (paramsNbt == NbtEnd.INSTANCE) return null;
				else throw new IllegalStateException("Received params NBT, but not syncing NBT?");
			}
			return new Receiving(
				BigGlobeAutoCodec.AUTO_CODEC.decode(ClientState.Syncing.CODER, syncingNbt, NbtOps.INSTANCE),
				paramsNbt
			);
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Exception decoding client generator params:", exception);
			throw new RuntimeException(exception);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process(Receiving receiving, PacketSender responseSender) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world != null) {
			ClientGeneratorParams data;
			try {
				//receiving can be null if the player is in a non-scripted dimension.
				data = receiving != null ? receiving.compile() : null;
			}
			catch (Exception exception) {
				BigGlobeMod.LOGGER.error("Failed to compile worldgen data from server!", exception);
				data = null;
			}
			ClientState state = ClientState.getOrCreate(world);
			state.generatorParams = data;
			if (data != null) {
				if (data.columnEntryRegistry != null && BigGlobeConfig.INSTANCE.get().dataPackDebugging.dependencyGraphs) {
					DependencyDepthSorter.start(
						data.compiledWorldTraits,
						data.columnEntryRegistry.registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY),
						"client"
					);
				}
			}
			LodSystem.reload((LodSystemHolder)(MinecraftClient.getInstance().worldRenderer), world, data);
		}
	}

	public void send(ServerWorld world, ServerPlayerEntity player) {
		ClientState.Syncing syncing;
		ClientGeneratorParams params;
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
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