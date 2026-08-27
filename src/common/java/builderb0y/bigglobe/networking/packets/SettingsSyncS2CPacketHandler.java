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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyDepthSorter;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.base.S2CPlayPacketHandler;
import builderb0y.bigglobe.rendering.lods.LodSystem;
import builderb0y.bigglobe.util.NbtIo2;

public class SettingsSyncS2CPacketHandler implements S2CPlayPacketHandler<SettingsSyncS2CPacketHandler.Receiving> {

	public static final SettingsSyncS2CPacketHandler INSTANCE = new SettingsSyncS2CPacketHandler();

	/**
	note: some received scripts depend on biome information,
	which is stored in the dynamic registries on the world object.
	as such, compilation must be delayed until the world is set.
	which means I can't call {@link #compile()} on the network thread.
	*/
	public static class Receiving {

		/**
		contains registry data needed by paramsNbt.
		*/
		public final ClientState.Syncing clientState;
		/**
		contains usage of registry data.
		*/
		public final Tag paramsNbt;

		public Receiving(ClientState.Syncing clientState, Tag paramsNbt) {
			this.clientState = clientState;
			this.paramsNbt = paramsNbt;
		}

		public ClientGeneratorParams compile() throws Exception {
			ColumnEntryRegistry.Loading loading = new ColumnEntryRegistry.Loading(this.clientState.lookup(), true, this.clientState.alwaysGenerateTheSameWorld);
			return ColumnEntryRegistry.Loading.OVERRIDE.get(
				loading,
				() -> {
					ClientGeneratorParams params = this.clientState.parse(this);
					if (params != null) params.compile(loading);
					return params;
				}
			);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable Receiving decode(FriendlyByteBuf buffer) {
		try {
			GZIPInputStream stream = new GZIPInputStream(new ByteBufInputStream(buffer));
			Tag syncingNbt = NbtIo2.read(stream);
			Tag paramsNbt = NbtIo2.read(stream);
			if (syncingNbt == EndTag.INSTANCE) {
				if (paramsNbt == EndTag.INSTANCE) return null;
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
		ClientLevel world = Minecraft.getInstance().level;
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
						data.columnEntryRegistry.registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_VALUE_REGISTRY_KEY),
						"client"
					);
				}
			}
			LodSystem.reload(LodSystemHolder.of(Minecraft.getInstance().levelRenderer), world, data);
		}
	}

	public void send(ServerLevel world, ServerPlayer player) {
		ClientState.Syncing syncing;
		ClientGeneratorParams params;
		if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			syncing = new ClientState.Syncing(generator);
			params = new ClientGeneratorParams(generator, syncing);
		}
		else {
			syncing = null;
			params = null;
		}
		Tag syncingNbt = BigGlobeAutoCodec.AUTO_CODEC.encode(ClientState.Syncing.CODER, syncing, NbtOps.INSTANCE);
		Tag paramsNbt = BigGlobeAutoCodec.AUTO_CODEC.encode(ClientGeneratorParams.NULLABLE_CODER, params, NbtOps.INSTANCE);
		FriendlyByteBuf buffer = this.buffer();
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