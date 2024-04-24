package builderb0y.bigglobe.hyperspace;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public abstract class WaypointManager<D extends WaypointManager.WaypointData> extends PersistentState {

	public Map<@Nullable UUID, WaypointList<D>> owners = new HashMap<>(16);

	public boolean addWaypoint(D waypoint) {
		if (this.forUUID(waypoint.owner(), true).add(waypoint)) {
			this.markDirty();
			return true;
		}
		else {
			return false;
		}
	}

	public boolean removeWaypoint(D waypoint) {
		WaypointList<D> list = this.forUUID(waypoint.owner(), false);
		if (list != null) {
			if (list.remove(waypoint)) {
				this.markDirty();
				return true;
			}
			else {
				return false;
			}
		}
		else {
			BigGlobeMod.LOGGER.warn("Attempt to remove " + waypoint + " from player who doesn't have any waypoints.");
			return false;
		}
	}

	public WaypointList<D> forUUID(@Nullable UUID uuid, boolean create) {
		if (create) {
			return this.owners.computeIfAbsent(uuid, WaypointList::new);
		}
		else {
			return this.owners.get(uuid);
		}
	}

	public WaypointList<D> forPlayer(PlayerEntity player, boolean create) {
		if (create) {
			return this.owners.computeIfAbsent(player.getGameProfile().getId(), WaypointList::new);
		}
		else {
			return this.owners.get(player.getGameProfile().getId());
		}
	}

	public Stream<D> getRelevantWaypoints(UUID playerUUID) {
		WaypointList<D> global = this.owners.get(null);
		if (playerUUID == null) {
			return global != null ? global.waypoints.values().stream() : Stream.empty();
		}
		else {
			WaypointList<D> owned = this.owners.get(playerUUID);
			if (global != null) {
				if (owned != null) {
					return Stream.concat(global.waypoints.values().stream(), owned.waypoints.values().stream());
				}
				else {
					return global.waypoints.values().stream();
				}
			}
			else {
				if (owned != null) {
					return owned.waypoints.values().stream();
				}
				else {
					return Stream.empty();
				}
			}
		}
	}

	public static class ServerWaypointManager extends WaypointManager<ServerWaypointData> {

		public static final PersistentState.Type<ServerWaypointManager>
			TYPE = new PersistentState.Type<>(ServerWaypointManager::new, ServerWaypointManager::new, null);

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
		public NbtCompound writeNbt(NbtCompound nbt) {
			NbtList waypoints = new NbtList();
			this.owners.values().stream().flatMap((WaypointList<ServerWaypointData> list) -> list.waypoints.values().stream()).map(ServerWaypointData::toNBT).forEach(waypoints::add);
			nbt.put("waypoints", waypoints);
			return nbt;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void readNbt(NbtCompound nbt) {
			this.owners.clear();
			for (NbtCompound waypointNBT : (Iterable<NbtCompound>)(Iterable)(nbt.getList("waypoints", NbtElement.COMPOUND_TYPE))) {
				ServerWaypointData waypoint = ServerWaypointData.fromNBT(waypointNBT);
				if (waypoint != null) this.addWaypoint(waypoint);
			}
		}

		public void toByteBuffer(PacketByteBuf buffer, ServerPlayerEntity player, Vec3d entrance) {
			Object2IntMap<RegistryKey<World>> worlds = new Object2IntOpenHashMap<>(4);
			for (ServerWorld world : BigGlobeMod.getCurrentServer().getWorlds()) {
				worlds.computeIfAbsent(world.getRegistryKey(), (RegistryKey<World> key) -> worlds.size());
			}
			buffer.writeVarInt(worlds.size());
			for (Object2IntMap.Entry<RegistryKey<World>> entry : worlds.object2IntEntrySet()) {
				String worldName = entry.getKey().getValue().toString();
				buffer.writeVarInt(entry.getIntValue()).writeVarInt(worldName.length()).writeCharSequence(worldName, StandardCharsets.ISO_8859_1);
			}
			this.listToByteBuffer(buffer, player, entrance, worlds, this.forUUID(null, false));
			this.listToByteBuffer(buffer, player, entrance, worlds, this.forUUID(player.getGameProfile().getId(), false));
		}

		public void listToByteBuffer(
			PacketByteBuf buffer,
			ServerPlayerEntity player,
			Vec3d entrance,
			Object2IntMap<RegistryKey<World>> worlds,
			WaypointList<ServerWaypointData> list
		) {
			if (list != null) {
				buffer.writeVarInt(list.waypoints.size());
				for (Iterator<ServerWaypointData> iterator = list.waypoints.values().iterator(); iterator.hasNext(); ) {
					ServerWaypointData data = iterator.next();
					ClientWaypointData relativized = data.relativize(player, entrance);
					if (relativized != null) {
						relativized.toByteBuffer(buffer, worlds);
					}
					else {
						BigGlobeMod.LOGGER.warn("Found waypoint for non-existent world: " + data + "; removing.");
						iterator.remove();
						this.markDirty();
					}
				}
			}
			else {
				buffer.writeVarInt(0);
			}
		}
	}

	public static class ClientWaypointManager extends WaypointManager<ClientWaypointData> {

		public static ClientWaypointManager INSTANCE = new ClientWaypointManager();

		@Environment(EnvType.CLIENT)
		public static void init() {
			ClientChunkEvents.CHUNK_LOAD.register((ClientWorld world, WorldChunk chunk) -> {
				if (BigGlobeEntityTypes.WAYPOINT != null && world.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
					INSTANCE.onChunkLoaded(world, chunk);
				}
			});
		}

		public void onChunkLoaded(ClientWorld world, WorldChunk chunk) {
			this
			.getRelevantWaypoints(MinecraftClient.getInstance().player.getGameProfile().getId())
			.filter((ClientWaypointData clientData) -> (
				clientData.clientPosition.x() >= chunk.getPos().getStartX() &&
				clientData.clientPosition.x() <  chunk.getPos().getStartX() + 16 &&
				clientData.clientPosition.z() >= chunk.getPos().getStartZ() &&
				clientData.clientPosition.z() <  chunk.getPos().getStartZ() + 16
			))
			.forEach((ClientWaypointData clientData) -> {
				WaypointEntity entity = BigGlobeEntityTypes.WAYPOINT.create(world);
				if (entity != null) {
					entity.setPosition(clientData.clientPosition.x(), clientData.clientPosition.y(), clientData.clientPosition.z());
					entity.setHealth(WaypointEntity.MAX_HEALTH);
					entity.isFake = true;
					entity.data = clientData.destination;
					world.addEntity(entity);
				}
			});
		}

		@Environment(EnvType.CLIENT)
		public static ClientWaypointManager fromByteBuffer(PacketByteBuf buffer) {
			int worldCount = buffer.readVarInt();
			Int2ObjectMap<RegistryKey<World>> worlds = new Int2ObjectOpenHashMap<>(worldCount);
			for (int worldIndex = 0; worldIndex < worldCount; worldIndex++) {
				int id = buffer.readVarInt();
				int length = buffer.readVarInt();
				RegistryKey<World> key = RegistryKey.of(RegistryKeyVersions.world(), new Identifier(buffer.readCharSequence(length, StandardCharsets.ISO_8859_1).toString()));
				worlds.put(id, key);
			}
			int publicCount = buffer.readVarInt();
			WaypointList<ClientWaypointData> publicList = new WaypointList<>(null);
			for (int publicIndex = 0; publicIndex < publicCount; publicIndex++) {
				publicList.add(ClientWaypointData.fromByteBuffer(buffer, worlds, null));
			}
			int privateCount = buffer.readVarInt();
			UUID playerUUID = playerUUID();
			WaypointList<ClientWaypointData> privateList = new WaypointList<>(playerUUID);
			for (int privateIndex = 0; privateIndex < privateCount; privateIndex++) {
				privateList.add(ClientWaypointData.fromByteBuffer(buffer, worlds, playerUUID));
			}
			ClientWaypointManager manager = new ClientWaypointManager();
			manager.owners.put(null, publicList);
			manager.owners.put(playerUUID, privateList);
			return manager;
		}

		@Environment(EnvType.CLIENT)
		public static void setInstance(ClientWaypointManager instance) {
			INSTANCE = instance;
			ClientWorld world = MinecraftClient.getInstance().world;
			if (world != null && world.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
				for (Entity entity : world.getEntities()) {
					if (entity instanceof WaypointEntity waypoint && waypoint.isFake) {
						waypoint.discard();
					}
				}
				AtomicReferenceArray<WorldChunk> chunks = world.getChunkManager().chunks.chunks;
				for (int index = 0, length = chunks.length(); index < length; index++) {
					WorldChunk chunk = chunks.getPlain(index);
					if (chunk != null) {
						instance.onChunkLoaded(world, chunk);
					}
				}
			}
		}

		@Override
		public NbtCompound writeNbt(NbtCompound nbt) {
			throw new UnsupportedOperationException();
		}
	}

	public static class WaypointList<D extends WaypointData> {

		public @Nullable UUID owner;
		public Map<UUID, D> waypoints;

		public WaypointList(@Nullable UUID owner) {
			this.owner = owner;
			this.waypoints = new HashMap<>(16);
		}

		public boolean add(D waypoint) {
			if (!Objects.equals(this.owner, waypoint.owner())) {
				throw new IllegalArgumentException("Attempt to add " + waypoint + " to wrong WaypointList owned by " + this.owner);
			}
			if (this.waypoints.putIfAbsent(waypoint.uuid(), waypoint) == null) {
				return true;
			}
			else {
				BigGlobeMod.LOGGER.warn("Attempt to add duplicate waypoint: " + waypoint);
				return false;
			}
		}

		public boolean remove(D waypoint) {
			if (this.waypoints.remove(waypoint.uuid()) != null) {
				return true;
			}
			else {
				BigGlobeMod.LOGGER.warn("Attempt to remove non-existent waypoint: " + waypoint);
				return false;
			}
		}
	}

	public static interface WaypointData {

		public abstract UUID uuid();

		public abstract UUID owner();
	}

	public static record ServerWaypointData(
		RegistryKey<World> world,
		Vec3d position,
		UUID uuid,
		@Nullable UUID owner
	)
	implements WaypointData {

		public ClientWaypointData relativize(ServerPlayerEntity player, Vec3d entrance) {
			ServerWorld oldWorld = player.getServer().getWorld(this.world);
			if (oldWorld == null) return null;
			double x = this.position.x - entrance.x;
			double z = this.position.z - entrance.z;
			if (x != 0.0D && z != 0.0D) {
				double scalar = 1.0D / Math.sqrt(Math.sqrt(BigGlobeMath.squareD(x, z)));
				x *= scalar;
				z *= scalar;
			}
			double fractionY = Interpolator.unmixLinear(oldWorld.getBottomY(), oldWorld.getTopY() - 2, this.position.y);
			double hyperspaceY = fractionY * (HyperspaceConstants.DIMENSION_HEIGHT - 2);
			return new ClientWaypointData(this, new Vector3f((float)(x), (float)(hyperspaceY), (float)(z)));
		}

		public NbtCompound toNBT() {
			NbtCompound nbt = new NbtCompound();
			nbt.putString("world", this.world.getValue().toString());

			NbtList position = new NbtList();
			position.add(NbtDouble.of(this.position.x));
			position.add(NbtDouble.of(this.position.y));
			position.add(NbtDouble.of(this.position.z));
			nbt.put("pos", position);

			nbt.putUuid("uuid", this.uuid);
			if (this.owner != null) nbt.putUuid("owner", this.owner);
			return nbt;
		}

		public static @Nullable WaypointManager.ServerWaypointData fromNBT(NbtCompound nbt) {
			RegistryKey<World> world;
			{
				String worldName = nbt.getString("world");
				if (worldName.isEmpty()) {
					BigGlobeMod.LOGGER.warn("Attempt to load waypoint with no world: " + nbt);
					return null;
				}
				Identifier worldIdentifier;
				try {
					worldIdentifier = new Identifier(worldName);
				}
				catch (InvalidIdentifierException exception) {
					BigGlobeMod.LOGGER.warn("Attempt to load waypoint with invalid world: " + nbt, exception);
					return null;
				}
				world = RegistryKey.of(RegistryKeyVersions.world(), worldIdentifier);
			}

			Vec3d position;
			{
				NbtList positionNBT = nbt.getList("pos", NbtElement.DOUBLE_TYPE);
				if (positionNBT.size() == 3) {
					position = new Vec3d(positionNBT.getDouble(0), positionNBT.getDouble(1), positionNBT.getDouble(2));
				}
				else {
					BigGlobeMod.LOGGER.warn("Attempt to load waypoint with invalid position: " + nbt);
					return null;
				}
			}

			UUID uuid;
			try {
				uuid = nbt.getUuid("uuid");
			}
			catch (IllegalArgumentException exception) {
				BigGlobeMod.LOGGER.warn("Attempt to load waypoint with invalid UUID: " + nbt, exception);
				return null;
			}

			UUID owner;
			{
				NbtElement ownerNBT = nbt.get("owner");
				if (ownerNBT != null) try {
					owner = NbtHelper.toUuid(ownerNBT);
				}
				catch (IllegalArgumentException exception) {
					BigGlobeMod.LOGGER.warn("Attempt to load waypoint with invalid owner: " + nbt, exception);
					return null;
				}
				else {
					owner = null;
				}
			}

			return new ServerWaypointData(world, position, uuid, owner);
		}

		public void toByteBuffer(PacketByteBuf buffer, Object2IntMap<RegistryKey<World>> worldIDs) {
			buffer
			.writeVarInt(worldIDs.getInt(this.world))
			.writeDouble(this.position.x)
			.writeDouble(this.position.y)
			.writeDouble(this.position.z)
			.writeUuid(this.uuid);
		}

		public static ServerWaypointData fromByteBuffer(PacketByteBuf buffer, Int2ObjectMap<RegistryKey<World>> worldIDs, UUID owner) {
			RegistryKey<World> world = worldIDs.get(buffer.readVarInt());
			double x = buffer.readDouble();
			double y = buffer.readDouble();
			double z = buffer.readDouble();
			UUID uuid = buffer.readUuid();
			return new ServerWaypointData(world, new Vec3d(x, y, z), uuid, owner);
		}
	}

	public static record ClientWaypointData(
		ServerWaypointData destination,
		Vector3fc clientPosition
	)
	implements WaypointData {

		@Override
		public UUID uuid() {
			return this.destination.uuid;
		}

		@Override
		public UUID owner() {
			return this.destination.owner;
		}

		public void toByteBuffer(PacketByteBuf buffer, Object2IntMap<RegistryKey<World>> worldIDs) {
			this.destination.toByteBuffer(buffer, worldIDs);
			buffer
			.writeFloat(this.clientPosition.x())
			.writeFloat(this.clientPosition.y())
			.writeFloat(this.clientPosition.z());
		}

		public static ClientWaypointData fromByteBuffer(PacketByteBuf buffer, Int2ObjectMap<RegistryKey<World>> worldIDs, UUID owner) {
			ServerWaypointData destination = ServerWaypointData.fromByteBuffer(buffer, worldIDs, owner);
			float clientX = buffer.readFloat();
			float clientY = buffer.readFloat();
			float clientZ = buffer.readFloat();
			return new ClientWaypointData(destination, new Vector3f(clientX, clientY, clientZ));
		}
	}

	public static UUID playerUUID() {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			return thePlayerUUID();
		}
		else {
			throw new IllegalStateException("Can't get the player UUID on server");
		}
	}

	@Environment(EnvType.CLIENT)
	public static UUID thePlayerUUID() {
		return MinecraftClient.getInstance().getGameProfile().getId();
	}
}