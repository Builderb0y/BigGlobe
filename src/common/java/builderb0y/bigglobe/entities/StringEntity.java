package builderb0y.bigglobe.entities;

import java.util.*;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.data.LongListData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.items.BallOfStringItem;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.DataHelper;
import builderb0y.bigglobe.versions.EntityVersions;

public class StringEntity extends Entity {

	public static final ResourceKey<LootTable> LOOT_TABLE_KEY = ResourceKey.create(Registries.LOOT_TABLE, BigGlobeMod.modID("entities/string"));

	public static final EntityDataAccessor<Integer>
		PREVIOUS_ID = SynchedEntityData.defineId(StringEntity.class, EntityDataSerializers.INT),
		NEXT_ID = SynchedEntityData.defineId(StringEntity.class, EntityDataSerializers.INT);

	//need to tick string entities in a very specific order.
	//minecraft doesn't guarantee that order, so I'm guaranteeing it manually.
	public static final WeakHashMap<Level, ArrayList<StringEntity>> TO_TICK = new WeakHashMap<>();

	static {
		ServerTickEvents.END_WORLD_TICK.register(StringEntity::onWorldTickEnd);
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			initClient();
		}
	}

	//apparently lambda methods don't get removed when their enclosing method is of
	//the wrong side, and apparently some JVMs preload lambda method parameter types.
	//so, I have to use an anonymous class instead of a lambda to prevent crashes.
	@SuppressWarnings("Convert2Lambda")
	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ClientTickEvents.END_CLIENT_TICK.register(new EndTick() {

			@Override
			public void onEndTick(Minecraft client) {
				if (client.level != null) {
					onWorldTickEnd(client.level);
				}
			}
		});
	}

	public static void onWorldTickEnd(Level world) {
		ArrayList<StringEntity> entities = TO_TICK.get(world);
		if (entities != null && !entities.isEmpty()) {
			entities.forEach(StringEntity::tickAll);
			entities.clear();
		}
	}

	public CachedEntity
		prevEntity = this.new CachedEntity(PREVIOUS_ID),
		nextEntity = this.new CachedEntity(NEXT_ID);

	public StringEntity(EntityType<?> type, Level world) {
		super(type, world);
		this.setSilent(true);
	}

	public StringEntity(EntityType<?> type, Level world, double x, double y, double z) {
		super(type, world);
		this.setPos(x, y, z);
		this.setSilent(true);
	}

	@Override
	public void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(PREVIOUS_ID, 0).define(NEXT_ID, 0);
	}

	public AABB getVisibilityBoundingBox() {
		Entity next = this.getNextEntity();
		if (next != null) {
			return this.getBoundingBox().minmax(next.getBoundingBox());
		}
		else {
			return this.getBoundingBox();
		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
		if (this.isInvulnerableToBase(source)) {
			return false;
		}
		this.dropString(world, source);
		this.remove(RemovalReason.KILLED);
		return true;
	}

	public void dropString(ServerLevel world, DamageSource damageSource) {
		world
			.getServer()

			.reloadableRegistries()
			.getLootTable(LOOT_TABLE_KEY)

			.getRandomItems(

				new LootParams.Builder(world)

					.withParameter(LootContextParams.THIS_ENTITY, this)
					.withParameter(LootContextParams.ORIGIN, EntityVersions.getPos(this))
					.withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)

					.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, damageSource.getEntity())
					.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, damageSource.getDirectEntity())

					.create(LootContextParamSets.ENTITY),
				(ItemStack stack) -> this.spawnAtLocation(world, stack)
			);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).getItem() == BigGlobeItems.BALL_OF_STRING && this.getNextEntity() == null) {
			this.setNextEntity(player);
			return InteractionResult.SUCCESS;
		}
		else {
			return InteractionResult.PASS;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!(this.getNextEntity() instanceof StringEntity)) {
			//last entity in the line.
			TO_TICK.computeIfAbsent(EntityVersions.getWorld(this), $ -> new ArrayList<>(4)).add(this);
		}
	}

	public void tickAll() {
		class Iter {

			public StringEntity current;

			public Iter(StringEntity current) {
				this.current = current;
			}

			public boolean next() {
				if (this.current.getNextEntity() instanceof StringEntity string) {
					this.current = string;
					return true;
				}
				return false;
			}

			public boolean prev() {
				if (this.current.getPrevEntity() instanceof StringEntity string && string.getPrevEntity() != null) {
					this.current = string;
					return true;
				}
				return false;
			}
		}

		if (this.getPrevEntity() instanceof StringEntity) {
			Iter iter = new Iter(this);
			//tug entities towards player first.
			do iter.current.moveTowards(iter.current.getNextEntity(), true);
			while (iter.prev());
			//then tug towards the start of the line.
			do iter.current.moveTowards(iter.current.getPrevEntity(), false);
			while (iter.next());
		}

		if (!EntityVersions.getWorld(this).isClientSide()) {
			this.maybeSplit();
		}
	}

	@Override
	public void baseTick() {
		this.prevEntity.update();
		this.nextEntity.update();
		super.baseTick();
		this.applyGravitationalVelocity();
	}

	public void applyGravitationalVelocity() {
		this.push(0.0D, -0.015625D, 0.0D);
		this.move(MoverType.SELF, this.getDeltaMovement());
		if (EntityVersions.isOnGround(this)) {
			this.setDeltaMovement(Vec3.ZERO);
		}
	}

	public void moveTowards(Entity other, boolean slow) {
		if (other == null) return;
		Vec3 projection = project(EntityVersions.getPos(this), EntityVersions.getPos(other), slow);
		if (projection != null) {
			if (other instanceof StringEntity string) {
				//make 2 adjacent strings "pull" on each other.
				Vec3 shared = new Vec3(0.0D, (this.getDeltaMovement().y + string.getDeltaMovement().y) * 0.5D, 0.0D);
				this.setDeltaMovement(shared);
				string.setDeltaMovement(shared);
			}
			else {
				this.setDeltaMovement(Vec3.ZERO);
			}
			this.moveLeniently(projection);
		}
	}

	public static @Nullable Vec3 project(Vec3 from, Vec3 onto, boolean slow) {
		double
			dx = onto.x() - from.x(),
			dy = onto.y() - from.y(),
			dz = onto.z() - from.z(),
			scalar = dx * dx + dy * dy + dz * dz;
		if (scalar > 1.0D) {
			scalar = Math.sqrt(scalar);
			//0.125 blocks per move operation seems to be the
			//max safe value for slabs, stairs, and corners.
			scalar = (slow ? Math.min(scalar - 1.0D, 0.125D) : (scalar - 1.0D)) / scalar;
			dx *= scalar;
			dy *= scalar;
			dz *= scalar;
			return new Vec3(dx + from.x, dy + from.y, dz + from.z);
		}
		else {
			return null;
		}
	}

	public void moveLeniently(Vec3 to) {
		double scalar = EntityVersions.getPos(this).distanceToSqr(to);
		if (!(scalar > 1.0E-7D)) return;
		this.setPos(to);
		this.moveOutOfBlocks();
	}

	public void moveOutOfBlocks() {
		record PositionedVoxelShape(BlockPos pos, VoxelShape shape) {

			public PositionedVoxelShape {
				pos = pos.immutable();
			}
		}

		for (
			BlockCollisions<PositionedVoxelShape> iterator = (
				new BlockCollisions<>(
					EntityVersions.getWorld(this),
					this,
					this.getBoundingBox(),
					false,
					PositionedVoxelShape::new
				)
			);
			iterator.hasNext();
		) {
			PositionedVoxelShape next = iterator.next();
			BlockPos pos = next.pos();
			VoxelShape collision = next.shape();
			collision.forAllBoxes(
				(
					double collisionMinX,
					double collisionMinY,
					double collisionMinZ,
					double collisionMaxX,
					double collisionMaxY,
					double collisionMaxZ
				)
					-> {
					AABB bounds = this.getBoundingBox();
					if (!bounds.intersects(collisionMinX, collisionMinY, collisionMinZ, collisionMaxX, collisionMaxY, collisionMaxZ)) return;

					double[] overlaps = new double[6];
					overlaps[Directions.POSITIVE_X.ordinal()] = collisionMaxX - bounds.minX;
					overlaps[Directions.POSITIVE_Y.ordinal()] = collisionMaxY - bounds.minY;
					overlaps[Directions.POSITIVE_Z.ordinal()] = collisionMaxZ - bounds.minZ;
					overlaps[Directions.NEGATIVE_X.ordinal()] = bounds.maxX - collisionMinX;
					overlaps[Directions.NEGATIVE_Y.ordinal()] = bounds.maxY - collisionMinY;
					overlaps[Directions.NEGATIVE_Z.ordinal()] = bounds.maxZ - collisionMinZ;

					Direction[] directions = Directions.ALL.clone();
					//sort by distance we need to move to escape the block in this direction.
					Arrays.sort(directions, Comparator.comparing((Direction direction) -> overlaps[direction.ordinal()]));

					Vec3 oldPos = EntityVersions.getPos(this);
					//try to escape the block by moving the least distance first.
					for (Direction direction : directions) {
						double amount = overlaps[direction.ordinal()];
						Vec3 newPos = oldPos.add(direction.getStepX() * amount, direction.getStepY() * amount, direction.getStepZ() * amount);
						this.setPos(newPos);
						AABB newBounds = this.getBoundingBox();
						BlockPos offsetPos = pos.relative(direction);
						//check if moving on this axis would put us inside another block or not.
						if (
							Shapes.joinIsNotEmpty(
								EntityVersions
									.getWorld(this)
									.getBlockState(offsetPos)
									.getCollisionShape(EntityVersions.getWorld(this), offsetPos, CollisionContext.of(this)),

								Shapes.create(
									newBounds.minX - offsetPos.getX(),
									newBounds.minY - offsetPos.getY(),
									newBounds.minZ - offsetPos.getZ(),
									newBounds.maxX - offsetPos.getX(),
									newBounds.maxY - offsetPos.getY(),
									newBounds.maxZ - offsetPos.getZ()
								),

								BooleanOp.AND
							)
						) {
							//inside another block. revert to previous position
							//and try a different axis if one is available.
							this.setPos(oldPos);
						}
						else {
							//successfully found a place where we can exist without colliding.
							//no need to try any more axes.
							break;
						}
					}
				}
			);
		}
	}

	public void maybeSplit() {
		Entity prevEntity = this.getPrevEntity();
		Entity nextEntity = this.getNextEntity();
		if (nextEntity instanceof Player player) {
			double distanceSquared = EntityVersions.getPos(this).distanceToSqr(
				player.getX(),
				Mth.clamp(
					this.getY(),
					player.getBoundingBox().minY - 1.0D,
					player.getBoundingBox().maxY
				),
				player.getZ()
			);
			if (distanceSquared > 4.0D) {
				if (tryTakeString(player, false)) {
					Vec3 target = project(
						player.getBoundingBox().getCenter(),
						EntityVersions.getPos(this),
						false
					);
					//should always be the case since distance was > 4 to begin with,
					//but just in case something weird happens,
					//it's better to handle this sanely.
					if (target != null) {
						StringEntity newEntity = new StringEntity(
							BigGlobeEntityTypes.STRING,
							EntityVersions.getWorld(this),
							target.x,
							target.y,
							target.z
						);
						newEntity.moveOutOfBlocks();
						this.setNextEntity(newEntity);
						newEntity.setPrevEntity(this);
						newEntity.setNextEntity(player);
						EntityVersions.getWorld(this).addFreshEntity(newEntity);
					}
				}
				else {
					this.setNextEntity(null);
				}
			}
			else if (distanceSquared < 0.99D) {
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

	public static boolean tryTakeString(Player player, boolean add) {
		Inventory inventory = player.getInventory();
		for (int slot = 0, size = inventory.getContainerSize(); slot < size; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.getItem() == BigGlobeItems.BALL_OF_STRING) {
				if (add) {
					BallOfStringItem.addString(stack, 1);
					return true;
				}
				else {
					if (stack.getDamageValue() < stack.getMaxDamage()) {
						stack.setDamageValue(stack.getDamageValue() + 1);
						return true;
					}
				}
			}
		}
		return false;
	}

	public static final Codec<UUID> UUID = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(
		new NamedCoder<>("UUID (as a long array)") {

			@Override
			@OverrideOnly
			public <T_Encoded> @Nullable UUID decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
				if (context.isEmpty()) return null;
				LongListData list = context.forceAsLongList();
				if (list.size() != 2) throw new DecodeException(() -> "Expected UUID to be 2 longs, but it was " + list.size() + " longs");
				return new UUID(list.getLong(0), list.getLong(1));
			}

			@Override
			@OverrideOnly
			public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, UUID> context) throws EncodeException {
				UUID uuid = context.object;
				if (uuid == null) return EmptyData.INSTANCE;
				return LongListData.wrap(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
			}
		}
	);

	public static final DataHelper<StringEntity> DATA_HELPER = (
		new DataHelper<>(StringEntity.class)
			.begin("prev").codec(UUID).pathAccessor("prevEntity.uuid", false).add()
			.begin("next").codec(UUID).pathAccessor("nextEntity.uuid", false).add()
	);

	@Override
	public void readAdditionalSaveData(ValueInput data) {
		DATA_HELPER.read(this, data);
	}

	@Override
	public void addAdditionalSaveData(ValueOutput data) {
		DATA_HELPER.write(this, data);
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

		public final EntityDataAccessor<Integer> trackedID;
		public @Nullable Entity entity;
		public UUID uuid;

		public CachedEntity(EntityDataAccessor<Integer> trackedID) {
			this.trackedID = trackedID;
		}

		public Entity update() {
			Level world = EntityVersions.getWorld(StringEntity.this);
			Entity entity;
			if (world.isClientSide()) {
				Integer id = StringEntity.this.entityData.get(this.trackedID);
				entity = id == 0 ? null : world.getEntity(id);
			}
			else {
				UUID uuid = this.uuid;
				entity = uuid == null ? null : ((ServerLevel)(world)).getEntity(uuid);
			}
			if (entity != null) {
				if (entity.distanceToSqr(StringEntity.this) > 256.0D) {
					this.uuid = null;
					entity = null;
				}
				else if (entity.isRemoved()) {
					entity = null;
				}
			}
			if (!world.isClientSide()) {
				StringEntity.this.entityData.set(this.trackedID, entity != null ? entity.getId() : 0);
			}
			return this.entity = entity;
		}

		public void setEntity(Entity entity) {
			if (!EntityVersions.getWorld(StringEntity.this).isClientSide()) {
				if (entity != null) {
					this.uuid = entity.getUUID();
					StringEntity.this.entityData.set(this.trackedID, entity.getId());
				}
				else {
					this.uuid = null;
					StringEntity.this.entityData.set(this.trackedID, 0);
				}
			}
		}
	}
}