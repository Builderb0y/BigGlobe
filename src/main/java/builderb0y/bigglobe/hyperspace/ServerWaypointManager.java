package builderb0y.bigglobe.hyperspace;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
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
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseFixer;
import builderb0y.autocodec.annotations.UseImprinter;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.imprinters.AutoImprinter.NamedImprinter;
import builderb0y.autocodec.imprinters.ImprintContext;
import builderb0y.autocodec.imprinters.ImprintException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

/**
manages all the waypoints on a server,
including all public waypoints, and all
private waypoints created by every player.
*/
@UseFixer(name = "INSTANCE", in = HyperspaceStorageVersions.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
@UseImprinter(name = "new", in = ServerWaypointManager.Imprinter.class, usage = MemberUsage.METHOD_IS_FACTORY)
public class ServerWaypointManager extends WaypointManager<ServerWaypointData> {

	#if MC_VERSION >= MC_1_21_5

		public static final PersistentStateType<ServerWaypointManager>
			TYPE = new PersistentStateType<>("bigglobe_hyperspace_waypoints", ServerWaypointManager::new, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ServerWaypointManager.class), null);

	#elif MC_VERSION >= MC_1_20_2

		public static final Type<ServerWaypointManager>
			TYPE = new Type<>(ServerWaypointManager::new, ServerWaypointManager::new, null);

	#endif

	public int nextID;

	public ServerWaypointManager() {}

	public static class Imprinter extends NamedImprinter<ServerWaypointManager> {

		public final AutoCoder<ServerWaypointData> waypointCoder;

		public Imprinter(FactoryContext<ServerWaypointManager> context) {
			super("ServerWaypointManager.Imprinter");
			this.waypointCoder = context.type(ReifiedType.from(ServerWaypointData.class)).forceCreateCoder();
		}

		@Override
		@OverrideOnly
		public <T_Encoded> void imprint(@NotNull ImprintContext<T_Encoded, ServerWaypointManager> context) throws ImprintException {
			ServerWaypointManager manager = context.object;
			manager.nextID = context.getMember("nextID").forceAsInt();
			ImprintException rootException = null;
			for (ImprintContext<T_Encoded, ServerWaypointManager> waypoint : context.getMember("waypoints").listIterable()) try {
				manager.addWaypoint(waypoint.decodeWith(this.waypointCoder), false);
			}
			catch (DecodeException exception) {
				if (rootException == null) rootException = new ImprintException(() -> "Some waypoints failed to be deserialized, see below.");
				rootException.addSuppressed(exception);
			}
			if (rootException != null) throw rootException;
		}

		@Override
		public @Nullable Stream<@NotNull String> getKeys() {
			return Stream.of("version", "waypoints", "nextID");
		}
	}

	public static @Nullable ServerWaypointManager get(ServerWorld world) {
		if (world.getRegistryKey() != HyperspaceConstants.WORLD_KEY) {
			world = world.getServer().getWorld(HyperspaceConstants.WORLD_KEY);
			if (world == null) return null;
		}
		#if MC_VERSION >= MC_1_21_5
			return world.getPersistentStateManager().getOrCreate(ServerWaypointManager.TYPE);
		#elif MC_VERSION >= MC_1_20_2
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
						ServerWorld world = server.getWorld(waypoint.pos().world());
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
					ServerWorld world = server.getWorld(removed.pos().world());
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