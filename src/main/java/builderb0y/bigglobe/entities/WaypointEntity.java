package builderb0y.bigglobe.entities;

import java.util.random.RandomGenerator;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.hyperspace.ServerWaypointData;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.networking.packets.UseWaypointPacket;
import builderb0y.bigglobe.networking.packets.WaypointRemoveC2SPacket;
import builderb0y.bigglobe.networking.packets.WaypointRenameC2SPacket;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.Vectors;
import builderb0y.bigglobe.versions.ItemStackVersions;

public class WaypointEntity extends Entity {

	public static final float MAX_HEALTH = 5.0F;
	static {
		AttackEntityCallback.EVENT.register((PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) -> {
			if (
				entity instanceof WaypointEntity waypoint &&
				waypoint.isFake &&
				waypoint.data != null
			) {
				if (!player.isSpectator()) {
					waypoint.implDamage(player.getDamageSources().playerAttack(player), 1.0F);
					if (!(waypoint.health > 0.0F)) {
						WaypointRemoveC2SPacket.INSTANCE.send(waypoint.data.id());
					}
				}
				return ActionResult.FAIL;
			}
			else {
				return ActionResult.PASS;
			}
		});
		UseEntityCallback.EVENT.register((PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) -> {
			if (
				entity instanceof WaypointEntity waypoint &&
				waypoint.isFake &&
				waypoint.data != null &&
				player.getStackInHand(hand).getItem() == Items.NAME_TAG
			) {
				if (!player.isSpectator()) {
					WaypointRenameC2SPacket.INSTANCE.send(waypoint.data.id(), hand);
				}
				return ActionResult.FAIL;
			}
			else {
				return ActionResult.PASS;
			}
		});
	}

	public @Nullable ServerWaypointData data;
	/** true if this entity is client-side only and does not exist on the server. */
	public boolean isFake;
	public float health;
	public Orbit[] orbits;

	public WaypointEntity(EntityType<?> type, World world) {
		super(type, world);
		if (world.isClient) {
			this.orbits = new Orbit[16];
			Permuter permuter = new Permuter(Permuter.stafford(System.currentTimeMillis() ^ System.nanoTime()));
			float circularHue = permuter.nextFloat();
			for (int index = 0; index < this.orbits.length; index++) {
				this.orbits[index] = new Orbit(permuter, circularHue);
			}
		}
	}

	@Override
	public boolean canHit() {
		return true;
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public boolean canUsePortals(#if MC_VERSION >= MC_1_21_0 boolean allowVehicles #endif) {
		return false;
	}

	@Nullable
	@Override
	public ItemStack getPickBlockStack() {
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
	public void onPlayerCollision(PlayerEntity player) {
		super.onPlayerCollision(player);
		if (player.hasPortalCooldown()) {
			player.setPortalCooldown(20);
			return;
		}
		if (
			this.isFake &&
			this.data != null &&
			player.getEyePos().squaredDistanceTo(
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
			damageSource.getSource() instanceof PlayerEntity player && (
				this.data == null ||
				this.data.owner() == null ||
				this.data.owner().equals(player.getGameProfile().getId())
			)
		);
	}

	public boolean isInvulnerableTo(DamageSource damageSource) {
		return !this.isVulnerableTo(damageSource);
	}

	@Override
	public boolean damage(#if MC_VERSION >= MC_1_21_2 ServerWorld world, #endif DamageSource source, float amount) {
		return this.implDamage(source, amount);
	}

	public boolean implDamage(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		}
		if (this.isFake != this.getWorld().isClient) {
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
		if (this.getWorld().isClient) {
			for (Orbit orbit : this.orbits) {
				orbit.tick();
			}
		}
		this.health = Math.min(this.health + 0.05F, MAX_HEALTH);
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.IGNORE;
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
	public boolean canAvoidTraps() {
		return true;
	}

	@Override
	public void initDataTracker(#if MC_VERSION >= MC_1_20_5 DataTracker.Builder builder #endif) {
		//not synced.
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		//not savable.
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		//not savable.
	}

	public static class Orbit {

		public float r, g, b;
		public float x1, y1, z1, x2, y2, z2;
		public float currentAngle, radius, speed;

		public Orbit(RandomGenerator random, float baseHue) {
			Vector3f scratch = new Vector3f(CloudColor.smoothHue(baseHue + random.nextFloat(0.25F)));
			this.r = scratch.x;
			this.g = scratch.y;
			this.b = scratch.z;

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

			this.radius = random.nextFloat() + 0.5F;
			this.currentAngle = random.nextFloat((float)(BigGlobeMath.TAU));
			this.speed = 0.25F / BigGlobeMath.squareF(this.radius);
		}

		public void tick() {
			this.currentAngle = BigGlobeMath.modulus_BP(this.currentAngle + this.speed, (float)(BigGlobeMath.TAU));
		}

		public void getPositionAndVelocity(Vector3f pos, Vector3f velocity, int history) {
			float angle = this.currentAngle - history * 0.125F;
			float sin = (float)(Math.sin(angle));
			float cos = (float)(Math.cos(angle));
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