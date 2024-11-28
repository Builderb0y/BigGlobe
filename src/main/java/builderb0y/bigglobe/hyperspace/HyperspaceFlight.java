package builderb0y.bigglobe.hyperspace;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;

public class HyperspaceFlight {

	public static void onPlayerTick(PlayerEntity player) {
		if (player.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
			player.getAbilities().allowFlying = true;
			player.getAbilities().flying = true;
		}
	}

	public static void onPlayerChangedDimension(ServerPlayerEntity player, ServerWorld oldWorld, ServerWorld newWorld) {
		if (newWorld.getRegistryKey() != HyperspaceConstants.WORLD_KEY) {
			if (oldWorld.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
				player.interactionManager.getGameMode().setAbilities(player.getAbilities());
				//only place where this is called from will send
				//a PlayerAbilitiesS2CPacket shortly afterward.
			}
			PlayerWaypointManager manager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
			if (manager != null) manager.entrance = null;
		}
	}

	static {
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(HyperspaceFlight::onPlayerChangedDimension);
	}
}