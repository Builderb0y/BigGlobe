package builderb0y.bigglobe;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.bigglobe.mixins.ClientWorld_CustomTimeSpeed;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.packets.SettingsSyncS2CPacketHandler;
import builderb0y.bigglobe.networking.packets.TimeSpeedS2CPacketHandler;
import builderb0y.bigglobe.settings.OverworldClientSettings;
import builderb0y.bigglobe.util.ClientWorldEvents;

public class ClientState {

	public static OverworldClientSettings settings;
	/** used by {@link ClientWorld_CustomTimeSpeed}. */
	public static double timeSpeed = 1.0D;

	static {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			initClient();
		}
	}

	/** called by the server to sync overworld settings to the client. */
	public static void sync(ServerPlayerEntity player) {
		BigGlobeNetwork.LOGGER.debug("Syncing ClientState to " + player);
		SettingsSyncS2CPacketHandler.INSTANCE.send(player);
		TimeSpeedS2CPacketHandler.INSTANCE.send(player);
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ClientWorldEvents.UNLOAD.register((ClientWorld world) -> {
			BigGlobeMod.LOGGER.info("Resetting ClientState on disconnect.");
			settings = null;
			timeSpeed = 1.0D;
		});
	}
}