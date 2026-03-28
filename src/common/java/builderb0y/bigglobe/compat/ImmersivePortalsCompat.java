package builderb0y.bigglobe.compat;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.jetbrains.annotations.Nullable;
import qouteall.dimlib.api.DimensionAPI;
import qouteall.dimlib.api.DimensionAPI.ServerDynamicUpdateListener;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.network.PacketRedirection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.versions.EntityVersions;

public class ImmersivePortalsCompat {

	public static void init() {
		if (InstalledMods.DIMLIB) try {
			DimLibCode.init();
		}
		catch (LinkageError error) {
			InstalledMods.DIMLIB = false;
			BigGlobeMod.LOGGER.error("Failed to setup DimLib integration:", error);
		}
		if (InstalledMods.IMMERSIVE_PORTALS) try {
			IPCode.init();
		}
		catch (LinkageError error) {
			InstalledMods.IMMERSIVE_PORTALS = false;
			BigGlobeMod.LOGGER.error("Exception setting up immersive portals integration:", error);
		}
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		if (InstalledMods.DIMLIB) try {
			DimLibCode.initClient();
		}
		catch (LinkageError error) {
			InstalledMods.DIMLIB = false;
			BigGlobeMod.LOGGER.error("Failed to setup DimLib integration:", error);
		}
		if (InstalledMods.IMMERSIVE_PORTALS) try {
			IPCode.initClient();
		}
		catch (LinkageError error) {
			InstalledMods.IMMERSIVE_PORTALS = false;
			BigGlobeMod.LOGGER.error("Exception setting up immersive portals integration:", error);
		}
	}

	public static void forEachDimension(MinecraftServer server, ServerPlayer player, BiConsumer<ServerLevel, ServerPlayer> action) {
		if (InstalledMods.IMMERSIVE_PORTALS) try {
			IPCode.forEachDimension(server, player, action);
		}
		catch (LinkageError error) {
			InstalledMods.IMMERSIVE_PORTALS = false;
			BigGlobeMod.LOGGER.error("Exception performing action for all dimensions", error);
		}
		else {
			action.accept(EntityVersions.getServerWorld(player), player);
		}
	}

	@Environment(EnvType.CLIENT)
	public static @Nullable LodSystemHolder getLodSystem(ResourceKey<Level> dimensionKey) {
		if (InstalledMods.IMMERSIVE_PORTALS) try {
			return IPCode.getLodSystem(dimensionKey);
		}
		catch (LinkageError error) {
			InstalledMods.IMMERSIVE_PORTALS = false;
			BigGlobeMod.LOGGER.error("Exception getting world renderer for " + dimensionKey, error);
		}
		ClientLevel world = Minecraft.getInstance().level;
		if (world != null && world.dimension() == dimensionKey) {
			return LodSystemHolder.of(Minecraft.getInstance().levelRenderer);
		}
		return null;
	}

	public static class DimLibCode {

		public static void init() {

		}

		@Environment(EnvType.CLIENT)
		public static void initClient() {
			DimensionAPI.CLIENT_DIMENSION_UPDATE_EVENT.register(ClientState::retain);
		}
	}

	public static class IPCode {

		public static Set<ResourceKey<Level>> WORLDS = new HashSet<>();

		public static void init() {
			ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
				WORLDS = new HashSet<>(server.levelKeys());
			});
			DimensionAPI.SERVER_DIMENSION_DYNAMIC_UPDATE_EVENT.register(new ServerDynamicUpdateListener() {

				@Override
				public void run(MinecraftServer server, Set<ResourceKey<Level>> worlds) {
					for (ResourceKey<Level> world : worlds) {
						//worlds that are in the new set, but not the old set, must have been added.
						if (!WORLDS.remove(world)) {
							this.onWorldAdded(server, world);
						}
					}
					//worlds that are in the old set, but not the new set, must have been removed.
					for (ResourceKey<Level> world : WORLDS) {
						this.onWorldRemoved(server, world);
					}
					WORLDS = new HashSet<>(worlds);
				}

				public void onWorldAdded(MinecraftServer server, ResourceKey<Level> worldKey) {
					ServerLevel serverWorld = server.getLevel(worldKey);
					if (serverWorld != null) { //dunno why it would ever be null, but handle this sanely anyway.
						for (ServerPlayer player : server.getPlayerList().getPlayers()) {
							PacketRedirection.withForceRedirect(
								serverWorld, () -> {
									ClientState.sync(serverWorld, player);
								}
							);
						}
					}
				}

				public void onWorldRemoved(MinecraftServer server, ResourceKey<Level> worldKey) {

				}
			});
		}

		@Environment(EnvType.CLIENT)
		public static void initClient() {

		}

		public static void forEachDimension(MinecraftServer server, ServerPlayer player, BiConsumer<ServerLevel, ServerPlayer> action) {
			for (ServerLevel serverWorld : server.getAllLevels()) {
				PacketRedirection.withForceRedirect(serverWorld, () -> action.accept(serverWorld, player));
			}
		}

		@Environment(EnvType.CLIENT)
		public static @Nullable LodSystemHolder getLodSystem(ResourceKey<Level> dimensionKey) {
			return LodSystemHolder.of(ClientWorldLoader.WORLD_RENDERER_MAP.get(dimensionKey));
		}
	}
}