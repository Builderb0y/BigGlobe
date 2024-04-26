package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.networking.packets.WaypointAddS2CPacket;
import builderb0y.bigglobe.networking.packets.WaypointRemoveS2CPacket;

public class ServerWaypointManager extends WaypointManager<ServerWaypointData> {

	public static final Type<ServerWaypointManager>
		TYPE = new Type<>(ServerWaypointManager::new, ServerWaypointManager::new, null);

	public ServerWaypointManager() {}

	public ServerWaypointManager(NbtCompound nbt) {
		this.readNbt(nbt);
	}

	public static @Nullable ServerWaypointManager get(ServerWorld world) {
		if (world.getRegistryKey() != HyperspaceConstants.WORLD_KEY) {
			world = world.getServer().getWorld(HyperspaceConstants.WORLD_KEY);
			if (world == null) return null;
		}
		return world.getPersistentStateManager().getOrCreate(ServerWaypointManager.TYPE, "bigglobe_hyperspace_waypoints");
	}

	@Override
	public boolean addWaypoint(ServerWaypointData waypoint, boolean executeCallbacks) {
		if (super.addWaypoint(waypoint, executeCallbacks)) {
			if (executeCallbacks) {
				MinecraftServer server = BigGlobeMod.currentServer;
				if (server != null) {
					if (waypoint.owner() != null) {
						ServerPlayerEntity player = server.getPlayerManager().getPlayer(waypoint.owner());
						if (player != null) {
							ClientWaypointManager clientManager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
							ClientWaypointData clientWaypoint;
							if (player.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
								clientWaypoint = waypoint.relativize(clientManager.entrance.position());
							}
							else {
								clientWaypoint = waypoint.absolutize();
							}
							clientManager.addWaypoint(clientWaypoint, false);
							WaypointAddS2CPacket.INSTANCE.send(player, clientWaypoint);
						}
					}
					else {
						ServerWorld world = server.getWorld(waypoint.world());
						if (world != null) {
							for (ServerPlayerEntity player : world.getPlayers()) {
								ClientWaypointManager clientManager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
								ClientWaypointData clientWaypoint = waypoint.absolutize();
								clientManager.addWaypoint(clientWaypoint, false);
								WaypointAddS2CPacket.INSTANCE.send(player, clientWaypoint);
							}
						}
						else {
							BigGlobeMod.LOGGER.warn("Attempt to add waypoint to non-existent world: " + waypoint);
						}
						world = server.getWorld(HyperspaceConstants.WORLD_KEY);
						if (world != null) {
							for (ServerPlayerEntity player : world.getPlayers()) {
								ClientWaypointManager clientManager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
								ClientWaypointData clientWaypoint = waypoint.relativize(clientManager.entrance.position());
								clientManager.addWaypoint(clientWaypoint, false);
								WaypointAddS2CPacket.INSTANCE.send(player, clientWaypoint);
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
	public ServerWaypointData removeWaypoint(UUID owner, UUID uuid, boolean executeCallbacks) {
		ServerWaypointData removed = super.removeWaypoint(owner, uuid, executeCallbacks);
		if (removed != null && executeCallbacks) {
			MinecraftServer server = BigGlobeMod.currentServer;
			if (server != null) {
				if (owner != null) {
					ServerPlayerEntity player = server.getPlayerManager().getPlayer(owner);
					if (player != null) {
						ClientWaypointManager clientManager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
						clientManager.removeWaypoint(owner, uuid, false);
						WaypointRemoveS2CPacket.INSTANCE.send(player, true, uuid);
					}
				}
				else {
					ServerWorld world = server.getWorld(removed.world());
					if (world != null) {
						for (ServerPlayerEntity player : world.getPlayers()) {
							ClientWaypointManager clientManager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
							clientManager.removeWaypoint(owner, uuid, false);
							WaypointRemoveS2CPacket.INSTANCE.send(player, false, uuid);
						}
					}
					else {
						BigGlobeMod.LOGGER.warn("Attempt to remove waypoint from non-existent world: " + removed);
					}
					world = server.getWorld(HyperspaceConstants.WORLD_KEY);
					if (world != null) {
						for (ServerPlayerEntity player : world.getPlayers()) {
							ClientWaypointManager clientManager = ((WaypointTracker)(player)).bigglobe_getWaypointManager();
							clientManager.removeWaypoint(owner, uuid, false);
							WaypointRemoveS2CPacket.INSTANCE.send(player, false, uuid);
						}
					}
				}
			}
		}
		return removed;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtList waypoints = new NbtList();
		this.getAllWaypoints().map(ServerWaypointData::toNBT).forEach(waypoints::add);
		nbt.put("waypoints", waypoints);
		return nbt;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void readNbt(NbtCompound nbt) {
		for (NbtCompound waypointNBT : (Iterable<NbtCompound>)(Iterable)(nbt.getList("waypoints", NbtElement.COMPOUND_TYPE))) {
			ServerWaypointData waypoint = ServerWaypointData.fromNBT(waypointNBT);
			if (waypoint != null) this.addWaypoint(waypoint, false);
		}
	}
}