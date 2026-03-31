package builderb0y.bigglobe.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.versions.EntityVersions;

/**
manages waypoints visible to a specific player at a specific time.
the waypoints visible to a player can change when waypoints are
added or removed from the server, or when the player changes dimensions.
for example, the player will be able to see all public waypoints,
and all of their own private waypoints while in hyperspace,
but in other dimensions their view of waypoints will be filtered to
not include waypoints that are in a different dimension than them.

this class also keeps track of an "entrance" position.
when the player is in hyperspace, this is the position
of the waypoint they entered hyperspace from.
when the player is in any other dimension, this position is null.
*/
public abstract class PlayerWaypointManager extends WaypointManager<PlayerWaypointData> {

	public static final int COLLAPSE_DURATION_TICKS = 20 * 20;

	public final Player player;
	public @Nullable PackedWorldPos entrance;
	public int collapseProgress = -1;

	public PlayerWaypointManager(Player player) {
		this.player = player;
	}

	public static @Nullable PlayerWaypointManager get(Player player) {
		return ((WaypointTracker)(player)).bigglobe_getWaypointManager();
	}

	public static PlayerWaypointManager create(Player player) {
		if (player.getClass() == ServerPlayer.class) { //exclude fake players.
			return new ServerPlayerWaypointManager((ServerPlayer)(player));
		}
		else if (EntityVersions.getWorld(player).isClientSide()) {
			return forPlayerClient(player);
		}
		else {
			return null;
		}
	}

	@Environment(EnvType.CLIENT)
	public static PlayerWaypointManager forPlayerClient(Player player) {
		if (player.getClass() == LocalPlayer.class) {
			return new ClientPlayerWaypointManager((LocalPlayer)(player));
		}
		else {
			return null;
		}
	}

	public void tick() {
		if (EntityVersions.getWorld(this.player).dimension() == HyperspaceConstants.WORLD_KEY) {
			if (this.getAllWaypoints().isEmpty()) {
				if (this.collapseProgress < COLLAPSE_DURATION_TICKS) this.collapseProgress++;
			}
			else {
				if (this.collapseProgress >= 0) this.collapseProgress--;
			}
		}
		else {
			this.collapseProgress = -1;
		}
	}
}