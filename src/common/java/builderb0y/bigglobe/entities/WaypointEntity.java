package builderb0y.bigglobe.entities;

import java.util.Arrays;
import java.util.random.RandomGenerator;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.hyperspace.ServerWaypointData;
import builderb0y.bigglobe.items.AuraBottleItem;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.FastMath;
import builderb0y.bigglobe.networking.packets.UseWaypointPacket;
import builderb0y.bigglobe.networking.packets.WaypointRemoveC2SPacket;
import builderb0y.bigglobe.networking.packets.WaypointRenameC2SPacket;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.Vectors;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.GameProfileVersions;
import builderb0y.bigglobe.versions.ItemStackVersions;

public class WaypointEntity extends Entity {

	public static final float MAX_HEALTH = 5.0F;

	static {
		AttackEntityCallback.EVENT.register((Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) -> {
			if (
				entity instanceof WaypointEntity waypoint &&
				waypoint.isFake &&
				waypoint.data != null
			) {
				if (!player.isSpectator()) {
					waypoint.implDamage(player.damageSources().playerAttack(player), 1.0F);
					if (!(waypoint.health > 0.0F)) {
						WaypointRemoveC2SPacket.INSTANCE.send(waypoint.data.id());
					}
				}
				return InteractionResult.FAIL;
			}
			else {
				return InteractionResult.PASS;
			}
		});
		UseEntityCallback.EVENT.register((Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) -> {
			if (
				entity instanceof WaypointEntity waypoint &&
				waypoint.isFake &&
				waypoint.data != null && (
					player.getItemInHand(hand).getItem() == Items.NAME_TAG ||
					player.getItemInHand(hand).getItem() instanceof AuraBottleItem
				)
			) {
				if (!player.isSpectator()) {
					WaypointRenameC2SPacket.INSTANCE.send(waypoint.data.id(), hand);
					player.swing(hand, false);
				}
				return InteractionResult.FAIL;
			}
			else {
				return InteractionResult.PASS;
			}
		});
	}

	public @Nullable ServerWaypointData data;
	/**
	true if this entity is client-side only and does not exist on the server.
	*/
	public boolean isFake;
	public float health;
	public CloudColor color = CloudColor.BLANK;
	public Orbit[] orbits;

	public WaypointEntity(EntityType<?> type, Level world) {
		super(type, world);
		if (world.isClientSide()) {
			this.orbits = new Orbit[16];
			Permuter permuter = new Permuter(Permuter.stafford(System.currentTimeMillis() ^ System.nanoTime()));
			for (int index = 0; index < this.orbits.length; index++) {
				this.orbits[index] = new Orbit(permuter, ((float)(index)) / ((float)(this.orbits.length)) + 0.5F);
			}
		}
	}

	public void setColor(CloudColor color) {
		this.color = color;
		if (this.orbits != null) {
			Permuter permuter = new Permuter(Permuter.stafford(System.currentTimeMillis() ^ System.nanoTime()));
			switch (color) {
				case BLANK -> {
					double initial = permuter.nextDouble();
					for (Orbit orbit : this.orbits) {
						double variation = permuter.nextDouble(-0.0625D, +0.0625D);
						orbit.setColor(initial + variation);
					}
				}
				case RAINBOW -> {
					for (Orbit orbit : this.orbits) {
						orbit.setColor(permuter.nextDouble());
					}
				}
				default -> {
					for (Orbit orbit : this.orbits) {
						double variation = permuter.nextDouble(-0.0625D, +0.0625D);
						orbit.setColor(color.hueFraction + variation);
					}
				}
			}
		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public boolean canUsePortal(boolean allowVehicles) {
		return false;
	}

	@Nullable
	@Override
	public ItemStack getPickResult() {
		if (this.data != null) {
			Item item = this.data.owner() != null ? BigGlobeItems.PRIVATE_WAYPOINT : BigGlobeItems.PUBLIC_WAYPOINT;
			if (item != null) {
				ItemStack stack = new ItemStack(item);
				if (this.data.name() != null) {
					ItemStackVersions.setCustomName(stack, this.data.name());
				}
				return stack;
			}
		}
		return null;
	}

	@Override
	public void playerTouch(Player player) {
		super.playerTouch(player);
		if (player.isOnPortalCooldown()) {
			player.setPortalCooldown(20);
			return;
		}
		if (
			this.isFake &&
			this.data != null &&
			player.getEyePosition().distanceToSqr(
				this.getX(),
				this.getY() + 1.0D,
				this.getZ()
			)
			<= 0.25D * this.health / MAX_HEALTH
		) {
			UseWaypointPacket.INSTANCE.send(this.data.id());
		}
	}

	public boolean isVulnerableTo(DamageSource damageSource) {
		return (
			damageSource.getDirectEntity() instanceof Player player && (
				this.data == null ||
				this.data.owner() == null ||
				this.data.owner().equals(GameProfileVersions.getUUID(player.getGameProfile()))
			)
		);
	}

	public boolean isInvulnerableTo(DamageSource damageSource) {
		return !this.isVulnerableTo(damageSource);
	}

	@Override
	public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
		return this.implDamage(source, amount);
	}

	public boolean implDamage(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		}
		if (this.isFake != EntityVersions.getWorld(this).isClientSide()) {
			return true;
		}
		float newHealth = this.health - amount;
		if (!(newHealth > 0.0F)) {
			this.health = 0.0F;
			this.remove(RemovalReason.KILLED);
		}
		else {
			this.health = newHealth;
		}
		return true;
	}

	@Override
	public void tick() {
		if (EntityVersions.getWorld(this).isClientSide()) {
			for (Orbit orbit : this.orbits) {
				orbit.tick();
			}
		}
		this.health = Math.min(this.health + 0.05F, MAX_HEALTH);
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	@Override
	public boolean canAddPassenger(Entity passenger) {
		return false;
	}

	@Override
	public boolean couldAcceptPassenger() {
		return false;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public void defineSynchedData(SynchedEntityData.Builder builder) {
		//not synced.
	}

	@Override
	public void readAdditionalSaveData(ValueInput view) {
		//not savable.
	}

	@Override
	public void addAdditionalSaveData(ValueOutput view) {
		//not savable.
	}

	public static class Orbit {

		public float r, g, b;
		public float x1, y1, z1, x2, y2, z2;
		public float currentAngle, radius, speed;

		public static Orbit copy(Orbit from, @Nullable Orbit to) {
			if (to == null) to = new Orbit();
			to.r = from.r;
			to.g = from.g;
			to.b = from.b;
			to.x1 = from.x1;
			to.y1 = from.y1;
			to.z1 = from.z1;
			to.x2 = from.x2;
			to.y2 = from.y2;
			to.z2 = from.z2;
			to.currentAngle = from.currentAngle;
			to.radius = from.radius;
			to.speed = from.speed;
			return to;
		}

		public static Orbit[] copy(Orbit[] from, @Nullable Orbit @Nullable [] to) {
			int length = from.length;
			if (to == null) {
				to = new Orbit[length];
			}
			else if (to.length != length) {
				to = Arrays.copyOf(to, length);
			}
			for (int index = 0; index < length; index++) {
				to[index] = copy(from[index], to[index]);
			}
			return to;
		}

		public Orbit() {}

		public Orbit(RandomGenerator random, float radius) {
			Vector3f scratch = new Vector3f();
			Vectors.setOnSphere(scratch, random, 1.0F);
			this.x1 = scratch.x;
			this.y1 = scratch.y;
			this.z1 = scratch.z;

			Vectors.setOnSphere(scratch, random, 1.0F);
			this.x2 = scratch.x;
			this.y2 = scratch.y;
			this.z2 = scratch.z;

			float dot = this.x1 * this.x2 + this.y1 * this.y2 + this.z1 * this.z2;
			this.x2 -= this.x1 * dot;
			this.y2 -= this.y1 * dot;
			this.z2 -= this.z1 * dot;

			scratch.set(this.x2, this.y2, this.z2).normalize();
			this.x2 = scratch.x;
			this.y2 = scratch.y;
			this.z2 = scratch.z;

			this.radius = radius;
			this.currentAngle = random.nextFloat((float)(BigGlobeMath.TAU));
			this.speed = 0.25F / BigGlobeMath.squareF(this.radius);
		}

		public void setColor(double hue) {
			Vector3d color = CloudColor.smoothHue(hue);
			this.r = (float)(color.x);
			this.g = (float)(color.y);
			this.b = (float)(color.z);
		}

		public void tick() {
			this.currentAngle = BigGlobeMath.modulus_BP(this.currentAngle + this.speed, (float)(BigGlobeMath.TAU));
		}

		public void getPositionAndVelocity(Vector3f pos, Vector3f velocity, float history, float partialTicks) {
			float angle = this.currentAngle + partialTicks * this.speed - history * 2.0F;
			float sin = (float)(FastMath.Trig.fastSin(angle));
			float cos = (float)(FastMath.Trig.fastCos(angle));
			pos.set(
				(this.x1 * cos + this.x2 * sin) * this.radius,
				(this.y1 * cos + this.y2 * sin) * this.radius,
				(this.z1 * cos + this.z2 * sin) * this.radius
			);
			velocity.set(
				this.x1 * sin - this.x2 * cos,
				this.y1 * sin - this.y2 * cos,
				this.z1 * sin - this.z2 * cos
			);
		}
	}
}