package builderb0y.bigglobe.hyperspace;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.versions.EntityVersions;

public class HyperspaceFlight {

	static {
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(HyperspaceFlight::onPlayerChangedDimension);
	}

	public static void onPlayerTick(Player player) {
		if (EntityVersions.getWorld(player).dimension() == HyperspaceConstants.WORLD_KEY) {
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
		}
	}

	public static void onPlayerChangedDimension(ServerPlayer player, ServerLevel oldWorld, ServerLevel newWorld) {
		if (newWorld.dimension() != HyperspaceConstants.WORLD_KEY) {
			if (oldWorld.dimension() == HyperspaceConstants.WORLD_KEY) {
				player.gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
				//only place where this is called from will send
				//a PlayerAbilitiesS2CPacket shortly afterward.
				//however, I've gotten at least one report of this not working.
				//the packet is small, so sending a duplicate copy of it shouldn't hurt anything.
				player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
			}
			PlayerWaypointManager manager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
			if (manager != null) manager.entrance = null;
		}
	}

	public static void init() {
	}
}