package builderb0y.bigglobe.hyperspace;

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;

public class ClientWaypointManager extends WaypointManager<ClientWaypointData> {

	public @Nullable ServerWaypointData entrance;
	public Map<ChunkPos, Set<ClientWaypointData>> byChunk = new HashMap<>(16);

	public ClientWaypointManager(@Nullable ServerWaypointData entrance) {
		this.entrance = entrance;
	}

	public static ClientWaypointManager create(ServerWaypointManager serverManager, PlayerEntity player, @Nullable ServerWaypointData entrance) {
		ClientWaypointManager clientManager = new ClientWaypointManager(entrance);
		convert(serverManager, clientManager, player, null, entrance);
		convert(serverManager, clientManager, player, player.getGameProfile().getId(), entrance);
		return clientManager;
	}

	public static void convert(
		ServerWaypointManager serverManager,
		ClientWaypointManager clientManager,
		PlayerEntity player,
		UUID owner,
		ServerWaypointData entrance
	) {
		WaypointList<ServerWaypointData> serverList = serverManager.owners.get(owner);
		if (serverList != null) {
			RegistryKey<World> worldTarget = player.getWorld().getRegistryKey();
			for (ServerWaypointData waypoint : serverList.waypoints.values()) {
				if (worldTarget == HyperspaceConstants.WORLD_KEY || worldTarget == waypoint.world()) {
					clientManager.addWaypoint(waypoint.toClientData(entrance.position()), false);
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static void init() {
		ClientChunkEvents.CHUNK_LOAD.register((ClientWorld world, WorldChunk chunk) -> {
			((WaypointTracker)(MinecraftClient.getInstance().player)).bigglobe_getWaypointManager().onChunkLoaded(world, chunk);
		});
	}

	public Set<ClientWaypointData> forChunk(ChunkPos pos, boolean create) {
		if (create) {
			return this.byChunk.computeIfAbsent(pos, (ChunkPos ignored) -> new ObjectOpenCustomHashSet<>(4, WaypointData.UUID_STRATEGY));
		}
		else {
			return this.byChunk.get(pos);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean addWaypoint(ClientWaypointData waypoint, boolean executeCallbacks) {
		if (super.addWaypoint(waypoint, executeCallbacks)) {
			ChunkPos chunkPos = waypoint.chunkPos();
			if (this.forChunk(chunkPos, true).add(waypoint)) {
				if (executeCallbacks) {
					ClientWorld world;
					WaypointEntity entity;
					if (
						BigGlobeEntityTypes.WAYPOINT != null &&
						(world = MinecraftClient.getInstance().world) != null &&
						(entity = BigGlobeEntityTypes.WAYPOINT.create(world)) != null
					) {
						entity.setPosition(waypoint.clientPosition().x(), waypoint.clientPosition().y() - 1.0D, waypoint.clientPosition().z());
						entity.setHealth(WaypointEntity.MAX_HEALTH);
						entity.isFake = true;
						entity.data = waypoint.destination();
						world.addEntity(entity);
					}
				}
			}
			else {
				BigGlobeMod.LOGGER.warn(waypoint + " already exists in chunk " + chunkPos);
			}

			return true;
		}
		else {
			return false;
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ClientWaypointData removeWaypoint(UUID owner, UUID uuid, boolean executeCallbacks) {
		ClientWaypointData waypoint = super.removeWaypoint(owner, uuid, executeCallbacks);
		if (waypoint != null) {
			Set<ClientWaypointData> set = this.byChunk.get(waypoint.chunkPos());
			if (set != null && set.remove(waypoint)) {
				if (executeCallbacks) {
					ClientWorld world = MinecraftClient.getInstance().world;
					if (world != null) {
						List<WaypointEntity> found = world.getEntitiesByClass(
							WaypointEntity.class,
							new Box(
								waypoint.clientPosition().x() - 1.0D,
								waypoint.clientPosition().y() - 1.0D,
								waypoint.clientPosition().z() - 1.0D,
								waypoint.clientPosition().x() + 1.0D,
								waypoint.clientPosition().y() + 1.0D,
								waypoint.clientPosition().z() + 1.0D
							),
							(WaypointEntity entity) -> entity.isFake && entity.data != null && entity.data.uuid().equals(waypoint.uuid())
						);
						switch (found.size()) {
							case 0 -> BigGlobeMod.LOGGER.warn("Did not find any waypoints in client world with UUID " + waypoint.uuid());
							case 1 -> found.get(0).discard();
							default -> {
								BigGlobeMod.LOGGER.warn("Found more than one waypoint in client world with UUID " + waypoint.uuid());
								found.forEach(Entity::discard);
							}
						}
					}
				}
			}
			else {
				BigGlobeMod.LOGGER.warn("Attempt to remove waypoint from a chunk where it is not present: " + waypoint);
			}
		}
		return waypoint;
	}

	@Environment(EnvType.CLIENT)
	public void onChunkLoaded(ClientWorld world, WorldChunk chunk) {
		if (BigGlobeEntityTypes.WAYPOINT != null) {
			Set<ClientWaypointData> waypoints = this.forChunk(chunk.getPos(), false);
			if (waypoints != null && !waypoints.isEmpty()) {
				for (ClientWaypointData waypoint : waypoints) {
					if (waypoint.clientPosition().y() - 1.0D >= chunk.getBottomY() && waypoint.clientPosition().y() - 1.0D < chunk.getTopY()) {
						WaypointEntity entity = BigGlobeEntityTypes.WAYPOINT.create(world);
						if (entity != null) {
							entity.setPosition(waypoint.clientPosition().x(), waypoint.clientPosition().y() - 1.0D, waypoint.clientPosition().z());
							entity.setHealth(WaypointEntity.MAX_HEALTH);
							entity.isFake = true;
							entity.data = waypoint.destination();
							world.addEntity(entity);
						}
					}
				}
			}
		}
	}

	public static void setOnServer(ServerPlayerEntity player, ClientWaypointManager manager) {
		((WaypointTracker)(player)).bigglobe_setWaypointManager(manager);
	}

	@Environment(EnvType.CLIENT)
	public static void setOnClient(ClientPlayerEntity player, ClientWaypointManager manager) {
		((WaypointTracker)(player)).bigglobe_setWaypointManager(manager);
		ClientWorld world = player.clientWorld;
		for (Entity entity : world.getEntities()) {
			if (entity instanceof WaypointEntity waypoint && waypoint.isFake) {
				waypoint.discard();
			}
		}
		AtomicReferenceArray<WorldChunk> chunks = world.getChunkManager().chunks.chunks;
		for (int index = 0, length = chunks.length(); index < length; index++) {
			WorldChunk chunk = chunks.getPlain(index);
			if (chunk != null) {
				manager.onChunkLoaded(world, chunk);
			}
		}
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		throw new UnsupportedOperationException();
	}
}