package builderb0y.bigglobe.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.autocodec.annotations.VerifyNullable;
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

	public final PlayerEntity player;
	public @Nullable PackedWorldPos entrance;
	public int collapseProgress = -1;

	public PlayerWaypointManager(PlayerEntity player) {
		this.player = player;
	}

	public static @Nullable PlayerWaypointManager get(PlayerEntity player) {
		return ((WaypointTracker)(player)).bigglobe_getWaypointManager();
	}

	public static PlayerWaypointManager create(PlayerEntity player) {
		if (player.getClass() == ServerPlayerEntity.class) { //exclude fake players.
			return new ServerPlayerWaypointManager((ServerPlayerEntity)(player));
		}
		else if (EntityVersions.getWorld(player).isClient()) {
			return forPlayerClient(player);
		}
		else {
			return null;
		}
	}

	@Environment(EnvType.CLIENT)
	public static PlayerWaypointManager forPlayerClient(PlayerEntity player) {
		if (player.getClass() == ClientPlayerEntity.class) {
			return new ClientPlayerWaypointManager((ClientPlayerEntity)(player));
		}
		else {
			return null;
		}
	}

	public void tick() {
		if (EntityVersions.getWorld(this.player).getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
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

	#if MC_VERSION < MC_1_21_5

		@Override
		public NbtCompound writeNbt(NbtCompound nbt #if MC_VERSION >= MC_1_20_5 , RegistryWrapper.WrapperLookup registryLookup #endif) {
			throw new UnsupportedOperationException();
		}

	#endif
}