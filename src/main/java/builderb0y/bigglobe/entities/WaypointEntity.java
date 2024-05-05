package builderb0y.bigglobe.entities;

import java.util.random.RandomGenerator;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.hyperspace.ServerWaypointData;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
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
					waypoint.damage(player.getDamageSources().playerAttack(player), 1.0F);
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
			this.orbits = new Orbit[32];
			Permuter permuter = new Permuter(Permuter.stafford(System.currentTimeMillis() ^ System.nanoTime()));
			double circularHue = permuter.nextDouble();
			double linearHue = permuter.nextDouble();
			for (int index = 0; index < 32; index++) {
				this.orbits[index] = (
					(index & 1) == 0
					? new CircularOrbit(permuter, circularHue)
					: new   LinearOrbit(permuter,   linearHue)
				);
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
	public boolean canUsePortals() {
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

	@Override
	public boolean isInvulnerableTo(DamageSource damageSource) {
		return !this.isVulnerableTo(damageSource);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
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

	public static abstract class Orbit {

		public int color;

		public Orbit(RandomGenerator random, double hue) {
			Vector3d color = CloudColor.smoothHue(hue);
			double saturation = random.nextDouble();
			double brightness = random.nextDouble() * 0.25D + 0.75D;
			color.x = Interpolator.mixLinear(color.x, 1.0D, saturation) * brightness;
			color.y = Interpolator.mixLinear(color.y, 1.0D, saturation) * brightness;
			color.z = Interpolator.mixLinear(color.z, 1.0D, saturation) * brightness;
			this.color = CloudColor.packARGB(color);
		}

		public abstract void tick();

		public abstract Vector3f getPosition(Vector3f out, int history);
	}

	public static class LinearOrbit extends Orbit {

		public float x, y, z;
		public float currentAltitude, speed;

		public LinearOrbit(RandomGenerator random, double hue) {
			super(random, hue);
			float radius = random.nextFloat() + 0.5F;
			Vector3f scratch = new Vector3f();
			Vectors.setOnSphere(scratch, random, radius);
			this.x = scratch.x;
			this.y = scratch.y;
			this.z = scratch.z;

			this.currentAltitude = random.nextFloat((float)(BigGlobeMath.TAU));
			this.speed = 0.0625F / BigGlobeMath.squareF(radius);
		}

		@Override
		public void tick() {
			this.currentAltitude = BigGlobeMath.modulus_BP(this.currentAltitude + this.speed, (float)(BigGlobeMath.TAU));
		}

		@Override
		public Vector3f getPosition(Vector3f out, int history) {
			float angle = this.currentAltitude - history * this.speed * 0.5F;
			float sin = (float)(Math.sin(angle));
			return out.set(this.x * sin, this.y * sin, this.z * sin);
		}
	}

	public static class CircularOrbit extends Orbit {

		public float x1, y1, z1, x2, y2, z2;
		public float currentAngle, speed;

		public CircularOrbit(RandomGenerator random, double hue) {
			super(random, hue);
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

			float radius = random.nextFloat() + 0.5F;
			this.x1 *= radius;
			this.y1 *= radius;
			this.z1 *= radius;
			this.x2 *= radius;
			this.y2 *= radius;
			this.z2 *= radius;

			this.currentAngle = random.nextFloat((float)(BigGlobeMath.TAU));
			this.speed = 0.0625F / BigGlobeMath.squareF(radius);
		}

		@Override
		public void tick() {
			this.currentAngle = BigGlobeMath.modulus_BP(this.currentAngle + this.speed, (float)(BigGlobeMath.TAU));
		}

		@Override
		public Vector3f getPosition(Vector3f out, int history) {
			float angle = this.currentAngle - history * this.speed * 0.5F;
			float sin = (float)(Math.sin(angle));
			float cos = (float)(Math.cos(angle));
			return out.set(
				this.x1 * cos + this.x2 * sin,
				this.y1 * cos + this.y2 * sin,
				this.z1 * cos + this.z2 * sin
			);
		}
	}
}