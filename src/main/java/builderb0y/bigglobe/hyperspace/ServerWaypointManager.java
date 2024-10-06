package builderb0y.bigglobe.hyperspace;

import java.util.UUID;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;

/**
manages all the waypoints on a server,
including all public waypoints, and all
private waypoints created by every player.
*/
public class ServerWaypointManager extends WaypointManager<ServerWaypointData> {

	#if MC_VERSION >= MC_1_20_2
		public static final Type<ServerWaypointManager>
			TYPE = new Type<>(ServerWaypointManager::new, ServerWaypointManager::new, null);
	#endif

	public int nextID;

	public ServerWaypointManager() {}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ServerWaypointManager(NbtCompound nbt #if MC_VERSION >= MC_1_20_5 , RegistryWrapper.WrapperLookup lookup #endif) {
		HyperspaceStorageVersions.update(nbt);
		this.nextID = nbt.getInt("nextID");
		for (NbtCompound waypointNBT : (Iterable<NbtCompound>)(Iterable)(nbt.getList("waypoints", NbtElement.COMPOUND_TYPE))) {
			ServerWaypointData waypoint = ServerWaypointData.fromNBT(waypointNBT);
			if (waypoint != null) this.addWaypoint(waypoint, false);
		}
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt #if MC_VERSION >= MC_1_20_5 , RegistryWrapper.WrapperLookup lookup #endif) {
		nbt.putByte("version", (byte)(HyperspaceStorageVersions.CURRENT_VERSION));
		NbtList waypoints = new NbtList();
		for (ServerWaypointData waypoint : this.getAllWaypoints()) {
			waypoints.add(waypoint.toNBT());
		}
		nbt.put("waypoints", waypoints);
		nbt.putInt("nextID", this.nextID);
		return nbt;
	}

	public static @Nullable ServerWaypointManager get(ServerWorld world) {
		if (world.getRegistryKey() != HyperspaceConstants.WORLD_KEY) {
			world = world.getServer().getWorld(HyperspaceConstants.WORLD_KEY);
			if (world == null) return null;
		}
		#if MC_VERSION >= MC_1_20_2
			return world.getPersistentStateManager().getOrCreate(ServerWaypointManager.TYPE, "bigglobe_hyperspace_waypoints");
		#else
			return world.getPersistentStateManager().getOrCreate(ServerWaypointManager::new, ServerWaypointManager::new, "bigglobe_hyperspace_waypoints");
		#endif
	}

	public int nextID() {
		int prevID = this.nextID;
		int nextID = prevID + 1;
		if (nextID == 0) throw new IllegalStateException("Ran out of IDs for waypoints.");
		this.nextID = nextID;
		return prevID;
	}

	public Stream<ServerWaypointData> getVisibleWaypoints(PlayerEntity player) {
		return this.getVisibleWaypoints(player.getGameProfile().getId(), player.getWorld().getRegistryKey());
	}

	public Stream<ServerWaypointData> getVisibleWaypoints(UUID playerUUID, RegistryKey<World> playerWorld) {
		Stream<ServerWaypointData> stream;
		WaypointLookup<ServerWaypointData> global = this.byOwner.get(null);
		if (playerUUID == null) {
			if (global != null && !global.isEmpty()) {
				stream = global.values().stream();
			}
			else {
				return Stream.empty();
			}
		}
		else {
			WaypointLookup<ServerWaypointData> owned = this.byOwner.get(playerUUID);
			if (global != null && !global.isEmpty()) {
				if (owned != null && !owned.isEmpty()) {
					stream = Stream.concat(global.values().stream(), owned.values().stream());
				}
				else {
					stream = global.values().stream();
				}
			}
			else {
				if (owned != null && !owned.isEmpty()) {
					stream = owned.values().stream();
				}
				else {
					return Stream.empty();
				}
			}
		}
		if (playerWorld != HyperspaceConstants.WORLD_KEY) {
			stream = stream.filter((ServerWaypointData data) -> data.destinationPosition().world() == playerWorld);
		}
		return stream;
	}

	@Override
	public boolean addWaypoint(ServerWaypointData waypoint, boolean sync) {
		if (super.addWaypoint(waypoint, sync)) {
			if (sync) {
				MinecraftServer server = BigGlobeMod.currentServer;
				if (server != null) {
					if (waypoint.owner() != null) {
						ServerPlayerEntity player = server.getPlayerManager().getPlayer(waypoint.owner());
						if (player != null) {
							PlayerWaypointManager playerManager = PlayerWaypointManager.get(player);
							if (playerManager != null) {
								PlayerWaypointData serverWaypoint;
								if (player.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
									serverWaypoint = waypoint.relativize(playerManager.entrance != null ? playerManager.entrance.pos() : PackedPos.ZERO);
								}
								else {
									serverWaypoint = waypoint.absolutize();
								}
								playerManager.addWaypoint(serverWaypoint, true);
							}
						}
					}
					else {
						ServerWorld world = server.getWorld(waypoint.position().world());
						if (world != null) {
							for (ServerPlayerEntity player : world.getPlayers()) {
								PlayerWaypointManager manager = PlayerWaypointManager.get(player);
								if (manager != null) {
									manager.addWaypoint(waypoint.absolutize(), true);
								}
							}
						}
						else {
							BigGlobeMod.LOGGER.warn("Attempt to add waypoint to non-existent world: " + waypoint);
						}
						world = server.getWorld(HyperspaceConstants.WORLD_KEY);
						if (world != null) {
							for (ServerPlayerEntity player : world.getPlayers()) {
								PlayerWaypointManager serverManager = PlayerWaypointManager.get(player);
								if (serverManager != null) {
									PlayerWaypointData serverWaypoint = waypoint.relativize(serverManager.entrance != null ? serverManager.entrance.pos() : PackedPos.ZERO);
									serverManager.addWaypoint(serverWaypoint, true);
								}
							}
						}
					}
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public ServerWaypointData removeWaypoint(int id, boolean sync) {
		ServerWaypointData removed = super.removeWaypoint(id, sync);
		if (removed != null && sync) {
			MinecraftServer server = BigGlobeMod.currentServer;
			if (server != null) {
				if (removed.owner() != null) {
					ServerPlayerEntity player = server.getPlayerManager().getPlayer(removed.owner());
					if (player != null) {
						PlayerWaypointManager manager = PlayerWaypointManager.get(player);
						if (manager != null) {
							manager.removeWaypoint(id, true);
						}
					}
				}
				else {
					ServerWorld world = server.getWorld(removed.position().world());
					if (world != null) {
						for (ServerPlayerEntity player : world.getPlayers()) {
							PlayerWaypointManager manager = PlayerWaypointManager.get(player);
							if (manager != null) {
								manager.removeWaypoint(id, true);
							}
						}
					}
					else {
						BigGlobeMod.LOGGER.warn("Attempt to remove waypoint from non-existent world: " + removed);
					}
					world = server.getWorld(HyperspaceConstants.WORLD_KEY);
					if (world != null) {
						for (ServerPlayerEntity player : world.getPlayers()) {
							PlayerWaypointManager manager = PlayerWaypointManager.get(player);
							if (manager != null) {
								manager.removeWaypoint(id, true);
							}
						}
					}
				}
			}
		}
		return removed;
	}
}