package builderb0y.bigglobe.entities;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import builderb0y.bigglobe.items.BallOfStringItem;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.versions.EntityVersions;

public class StringEntity extends Entity {

	public static final TrackedData<Integer>
		PREVIOUS_ID = DataTracker.registerData(StringEntity.class, TrackedDataHandlerRegistry.INTEGER),
		NEXT_ID     = DataTracker.registerData(StringEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public CachedEntity
		prevEntity = this.new CachedEntity(PREVIOUS_ID),
		nextEntity = this.new CachedEntity(NEXT_ID);

	public StringEntity(EntityType<?> type, World world) {
		super(type, world);
		this.setSilent(true);
	}

	public StringEntity(EntityType<?> type, World world, double x, double y, double z) {
		super(type, world);
		this.setPosition(x, y, z);
		this.setSilent(true);
	}

	@Override
	public void initDataTracker() {
		this.dataTracker.startTracking(PREVIOUS_ID, 0);
		this.dataTracker.startTracking(NEXT_ID, 0);
	}

	@Override
	public Box getVisibilityBoundingBox() {
		Entity next = this.getNextEntity();
		if (next != null) {
			return this.getBoundingBox().union(next.getBoundingBox());
		}
		else {
			return this.getBoundingBox();
		}
	}

	@Override
	public boolean canHit() {
		return this.getNextEntity() == null;
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (player.getStackInHand(hand).getItem() == BigGlobeItems.BALL_OF_STRING) {
			this.setNextEntity(player);
			return ActionResult.SUCCESS;
		}
		else {
			return ActionResult.PASS;
		}
	}

	@Override
	public void tick() {
		Entity prevEntity = this.prevEntity.update();
		Entity nextEntity = this.nextEntity.update();
		//System.out.println((this.world.isClient ? "CLIENT: " : "SERVER: ") + (prevEntity != null ? prevEntity.getId() : 0) + " <- " + this.getId() + " -> " + (nextEntity != null ? nextEntity.getId() : 0));
		super.tick();
		this.tickMovement(prevEntity, nextEntity);
		if (!EntityVersions.getWorld(this).isClient) {
			this.maybeSplit(prevEntity, nextEntity);
		}
	}

	public void tickMovement(Entity prevEntity, Entity nextEntity) {
		this.adjustToNeighbors(prevEntity, nextEntity);
		this.applyGravity();
	}

	public void applyGravity() {
		this.move(MovementType.SELF, new Vec3d(0.0D, -0.04D, 0.0D));
		if (this.onGround) {
			this.setVelocity(Vec3d.ZERO);
		}
	}

	public void adjustToNeighbors(Entity prevEntity, Entity nextEntity) {
		if (prevEntity != null && nextEntity != null) {
			Vec3d currentPos = this.getPos();
			Vec3d idealPos = this.tryToCenterSelfBetweenNeighbors(prevEntity, nextEntity);
			this.move(MovementType.SELF, idealPos.subtract(currentPos));
		}
	}

	public Vec3d tryToCenterSelfBetweenNeighbors(Entity prev, Entity next) {
		Vec3d
			selfPos = this.getPos(),
			prevPos = prev.getPos(),
			nextPos = next.getPos();
		double prevNextDistanceSquared = prevPos.squaredDistanceTo(nextPos);
		if (prevNextDistanceSquared >= 4.0D) {
			//no valid location, pick the next best option.
			return middle(prevPos, nextPos);
		}
		double prevDistanceSquared = selfPos.squaredDistanceTo(prevPos);
		double nextDistanceSquared = selfPos.squaredDistanceTo(nextPos);
		if (prevDistanceSquared <= 1.0D && nextDistanceSquared <= 1.0D) {
			return selfPos;
		}
		//could happen when teleporting one string to another with commands or something,
		//in which case later logic will NaN out if we don't handle this sanely.
		if (prevNextDistanceSquared == 0.0D) {
			return selfPos.subtract(prevPos).normalize().add(prevPos);
		}
		Vec3d best;
		if (prevDistanceSquared > nextDistanceSquared) {
			best = selfPos.subtract(prevPos).normalize().add(prevPos);
			if (best.squaredDistanceTo(nextPos) <= 1.0D) {
				return best;
			}
		}
		else {
			best = selfPos.subtract(nextPos).normalize().add(nextPos);
			if (best.squaredDistanceTo(prevPos) <= 1.0D) {
				return best;
			}
		}
		Vec3d middle = middle(prevPos, nextPos);
		Vec3d midRelative = selfPos.subtract(middle);
		Vec3d normal = nextPos.subtract(prevPos).normalize();
		double dot = midRelative.dotProduct(normal);
		Vec3d onPlane = midRelative.subtract(normal.multiply(dot));
		double edgeRadius = Math.sqrt(1.0D - prevNextDistanceSquared * 0.25D);
		return onPlane.multiply(edgeRadius / onPlane.length()).add(middle);
	}

	public static Vec3d middle(Vec3d first, Vec3d second) {
		return new Vec3d(
			(first.x + second.x) * 0.5D,
			(first.y + second.y) * 0.5D,
			(first.z + second.z) * 0.5D
		);
	}

	public void maybeSplit(Entity prevEntity, Entity nextEntity) {
		if (nextEntity instanceof PlayerEntity player) {
			double distanceSquared = this.getPos().squaredDistanceTo(
				player.getX(),
				MathHelper.clamp(
					this.getY(),
					player.getBoundingBox().minY,
					player.getBoundingBox().maxY
				),
				player.getZ()
			);
			if (distanceSquared > 4.0D) {
				if (tryTakeString(player, false)) {
					double newX = (this.getX() + player.getX()) * 0.5D;
					double newY = (this.getY() + (player.getBoundingBox().minY + player.getBoundingBox().maxY) * 0.5D) * 0.5D;
					double newZ = (this.getZ() + player.getZ()) * 0.5D;
					StringEntity newEntity = new StringEntity(BigGlobeEntityTypes.STRING, EntityVersions.getWorld(this), player.getX(), player.getY(), player.getZ());
					newEntity.move(MovementType.SELF, new Vec3d(newX - player.getX(), newY - player.getY(), newZ - player.getZ()));
					EntityVersions.getWorld(this).spawnEntity(newEntity);
					this.setNextEntity(newEntity);
					newEntity.setPrevEntity(this);
					newEntity.setNextEntity(player);
				}
				else {
					this.setNextEntity(null);
				}
			}
			else if (distanceSquared <= 1.0D) {
				if (tryTakeString(player, true)) {
					if (prevEntity instanceof StringEntity string) {
						string.setNextEntity(player);
					}
					this.setPrevEntity(null);
					this.setNextEntity(null);
					this.discard();
				}
			}
		}
	}

	public static boolean tryTakeString(PlayerEntity player, boolean add) {
		PlayerInventory inventory = player.getInventory();
		for (int slot = 0, size = inventory.size(); slot < size; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.getItem() == BigGlobeItems.BALL_OF_STRING) {
				if (add) {
					BallOfStringItem.addString(stack, 1);
					return true;
				}
				else {
					if (stack.getDamage() < stack.getMaxDamage()) {
						stack.setDamage(stack.getDamage() + 1);
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		UUID prev = getUUID(nbt, "prev");
		if (prev != null) this.prevEntity.uuid = prev;
		UUID next = getUUID(nbt, "next");
		if (next != null) this.nextEntity.uuid = next;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt = super.writeNbt(nbt);
		putUUID(nbt, "prev", this.prevEntity.uuid);
		putUUID(nbt, "next", this.nextEntity.uuid);
		return nbt;
	}

	public static void putUUID(NbtCompound compound, String key, UUID uuid) {
		if (uuid != null) {
			compound.putLongArray(key, new long[] { uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() });
		}
	}

	public static @Nullable UUID getUUID(NbtCompound compound, String key) {
		long[] element = compound.getLongArray(key);
		if (element.length == 2) {
			return new UUID(element[0], element[1]);
		}
		else {
			return null;
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {}

	@Override
	public Packet<?> createSpawnPacket() {
		return new EntitySpawnS2CPacket(this);
	}

	public @Nullable Entity getPrevEntity() {
		return this.prevEntity.entity;
	}

	public @Nullable Entity getNextEntity() {
		return this.nextEntity.entity;
	}

	public void setPrevEntity(@Nullable Entity entity) {
		this.prevEntity.setEntity(entity);
	}

	public void setNextEntity(@Nullable Entity entity) {
		this.nextEntity.setEntity(entity);
	}

	public class CachedEntity {

		public final TrackedData<Integer> trackedID;
		public @Nullable Entity entity;
		public UUID uuid;

		public CachedEntity(TrackedData<Integer> trackedID) {
			this.trackedID = trackedID;
		}

		public Entity update() {
			World world = EntityVersions.getWorld(StringEntity.this);
			Entity entity;
			if (world.isClient) {
				Integer id = StringEntity.this.dataTracker.get(this.trackedID);
				entity = id == 0 ? null : world.getEntityById(id);
			}
			else {
				UUID uuid = this.uuid;
				entity = uuid == null ? null : ((ServerWorld)world).getEntity(uuid);
			}
			if (entity != null) {
				if (entity.squaredDistanceTo(StringEntity.this) > 256.0D) {
					this.uuid = null;
					entity = null;
				}
				else if (entity.isRemoved()) {
					entity = null;
				}
			}
			if (!world.isClient) {
				StringEntity.this.dataTracker.set(this.trackedID, entity != null ? entity.getId() : 0);
			}
			return this.entity = entity;
		}

		public void setEntity(Entity entity) {
			if (!EntityVersions.getWorld(StringEntity.this).isClient) {
				if (entity != null) {
					this.uuid = entity.getUuid();
					StringEntity.this.dataTracker.set(this.trackedID, entity.getId());
				}
				else {
					this.uuid = null;
					StringEntity.this.dataTracker.set(this.trackedID, 0);
				}
			}
		}
	}
}