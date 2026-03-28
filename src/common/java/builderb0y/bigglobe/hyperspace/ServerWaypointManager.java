package builderb0y.bigglobe.hyperspace;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedDataType;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseFixer;
import builderb0y.autocodec.annotations.UseImprinter;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.imprinters.AutoImprinter.NamedImprinter;
import builderb0y.autocodec.imprinters.ImprintContext;
import builderb0y.autocodec.imprinters.ImprintException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.GameProfileVersions;

/**
manages all the waypoints on a server,
including all public waypoints, and all
private waypoints created by every player.
*/
@AddPseudoField("waypoints")
@UseFixer(name = "INSTANCE", in = HyperspaceStorageVersions.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
@UseImprinter(name = "new", in = ServerWaypointManager.Imprinter.class, usage = MemberUsage.METHOD_IS_FACTORY)
public class ServerWaypointManager extends WaypointManager<ServerWaypointData> {

	public static final SavedDataType<ServerWaypointManager>
		TYPE = new SavedDataType<>("bigglobe_hyperspace_waypoints", ServerWaypointManager::new, BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ServerWaypointManager.class), null);

	public int nextID;

	public ServerWaypointManager() {
	}

	public Collection<ServerWaypointData> waypoints() {
		return this.getAllWaypoints();
	}

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
			manager.nextID = context.forceGetMember("nextID").forceAsInt();
			ImprintException rootException = null;
			for (ImprintContext<T_Encoded, ServerWaypointManager> waypoint : context.forceGetMember("waypoints").listIterable())
				try {
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

	public static @Nullable ServerWaypointManager get(ServerLevel world) {
		if (world.dimension() != HyperspaceConstants.WORLD_KEY) {
			world = world.getServer().getLevel(HyperspaceConstants.WORLD_KEY);
			if (world == null) return null;
		}

		return world.getDataStorage().computeIfAbsent(ServerWaypointManager.TYPE);
	}

	public int nextID() {
		int prevID = this.nextID;
		int nextID = prevID + 1;
		if (nextID == 0) throw new IllegalStateException("Ran out of IDs for waypoints.");
		this.nextID = nextID;
		return prevID;
	}

	public Stream<ServerWaypointData> getVisibleWaypoints(Player player) {
		return this.getVisibleWaypoints(GameProfileVersions.getUUID(player.getGameProfile()), EntityVersions.getWorld(player).dimension());
	}

	public Stream<ServerWaypointData> getVisibleWaypoints(UUID playerUUID, ResourceKey<Level> playerWorld) {
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
						ServerPlayer player = server.getPlayerList().getPlayer(waypoint.owner());
						if (player != null) {
							PlayerWaypointManager playerManager = PlayerWaypointManager.get(player);
							if (playerManager != null) {
								PlayerWaypointData serverWaypoint;
								if (EntityVersions.getWorld(player).dimension() == HyperspaceConstants.WORLD_KEY) {
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
						ServerLevel world = server.getLevel(waypoint.pos().world());
						if (world != null) {
							for (ServerPlayer player : world.players()) {
								PlayerWaypointManager manager = PlayerWaypointManager.get(player);
								if (manager != null) {
									manager.addWaypoint(waypoint.absolutize(), true);
								}
							}
						}
						else {
							BigGlobeMod.LOGGER.warn("Attempt to add waypoint to non-existent world: " + waypoint);
						}
						world = server.getLevel(HyperspaceConstants.WORLD_KEY);
						if (world != null) {
							for (ServerPlayer player : world.players()) {
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
					ServerPlayer player = server.getPlayerList().getPlayer(removed.owner());
					if (player != null) {
						PlayerWaypointManager manager = PlayerWaypointManager.get(player);
						if (manager != null) {
							manager.removeWaypoint(id, true);
						}
					}
				}
				else {
					ServerLevel world = server.getLevel(removed.pos().world());
					if (world != null) {
						for (ServerPlayer player : world.players()) {
							PlayerWaypointManager manager = PlayerWaypointManager.get(player);
							if (manager != null) {
								manager.removeWaypoint(id, true);
							}
						}
					}
					else {
						BigGlobeMod.LOGGER.warn("Attempt to remove waypoint from non-existent world: " + removed);
					}
					world = server.getLevel(HyperspaceConstants.WORLD_KEY);
					if (world != null) {
						for (ServerPlayer player : world.players()) {
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