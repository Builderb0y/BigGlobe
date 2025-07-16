package builderb0y.bigglobe.versions;

import java.util.Collections;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import builderb0y.bigglobe.math.BigGlobeMath;

#if MC_VERSION < MC_1_21_0
	import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
#endif

public class EntityVersions {

	public static World getWorld(Entity entity) {
		return entity.getWorld();
	}

	public static ServerWorld getServerWorld(ServerPlayerEntity player) {
		#if MC_VERSION >= MC_1_21_6
			return player.getWorld();
		#else
			return player.getServerWorld();
		#endif
	}

	public static boolean isOnGround(Entity entity) {
		return entity.isOnGround();
	}

	public static ItemStack getAmmunition(PlayerEntity player, ItemStack weapon) {
		return player.getProjectileType(weapon);
	}

	public static double getBlockReachDistance(PlayerEntity player) {
		#if MC_VERSION >= MC_1_21_2
			return player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
		#elif MC_VERSION >= MC_1_20_5
			return player.getAttributeValue(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE);
		#else
			return 8.0D;
		#endif
	}

	public static double getBlockReachDistanceSquared(PlayerEntity player) {
		return BigGlobeMath.squareD(getBlockReachDistance(player));
	}

	public static double getEntityReachDistance(PlayerEntity player) {
		#if MC_VERSION >= MC_1_21_2
			return player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
		#elif MC_VERSION >= MC_1_20_5
			return player.getAttributeValue(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE);
		#else
			return 8.0D;
		#endif
	}

	public static double getEntityReachDistanceSquared(PlayerEntity player) {
		return BigGlobeMath.squareD(getEntityReachDistance(player));
	}

	public static void setPortalCooldown(Entity entity, int cooldown) {
		entity.setPortalCooldown(cooldown);
	}

	public static Box getBoundingBox(EntityType<?> type, double x, double y, double z) {
		return type. #if MC_VERSION >= MC_1_20_5 getSpawnBox #else createSimpleBoundingBox #endif (x, y, z);
	}

	public static ServerPlayerEntity teleport(ServerPlayerEntity player, ServerWorld destinationWorld, Vec3d position, Vec3d velocity, float yaw, float pitch) {
		#if MC_VERSION >= MC_1_21_2
			return player.teleportTo(
				new TeleportTarget(
					destinationWorld,
					position,
					velocity,
					yaw,
					pitch,
					false,
					false,
					Collections.emptySet(),
					TeleportTarget.NO_OP
				)
			);
		#elif MC_VERSION >= MC_1_21_0
			return (ServerPlayerEntity)(
				player.teleportTo(
					new TeleportTarget(
						destinationWorld,
						position,
						velocity,
						yaw,
						pitch,
						false,
						TeleportTarget.NO_OP
					)
				)
			);
		#else
			return FabricDimensions.teleport(
				player,
				destinationWorld,
				new TeleportTarget(
					position,
					player.getVelocity(),
					player.getYaw(),
					player.getPitch()
				)
			);
		#endif
	}

	public static RegistryKey<World> getRespawnDimension(ServerPlayerEntity player) {
		#if MC_VERSION >= MC_1_21_5
			return player.getRespawn() != null ? player.getRespawn().dimension() : null;
		#else
			return player.getSpawnPointDimension();
		#endif
	}

	public static BlockPos getRespawnPosition(ServerPlayerEntity player) {
		#if MC_VERSION >= MC_1_21_5
			return player.getRespawn() != null ? player.getRespawn().pos() : null;
		#else
			return player.getSpawnPointPosition();
		#endif
	}

	public static boolean isRespawnForced(ServerPlayerEntity player) {
		#if MC_VERSION >= MC_1_21_5
			return player.getRespawn() != null && player.getRespawn().forced();
		#else
			return player.isSpawnForced();
		#endif
	}

	public static float getRespawnAngle(ServerPlayerEntity player) {
		#if MC_VERSION >= MC_1_21_5
			return player.getRespawn() != null ? player.getRespawn().angle() : 0.0F;
		#else
			return player.getSpawnAngle();
		#endif
	}

	public static double prevX(Entity entity) {
		return entity.#if MC_VERSION >= MC_1_21_5 lastX #else prevX #endif;
	}

	public static double prevY(Entity entity) {
		return entity.#if MC_VERSION >= MC_1_21_5 lastY #else prevY #endif;
	}

	public static double prevZ(Entity entity) {
		return entity.#if MC_VERSION >= MC_1_21_5 lastZ #else prevZ #endif;
	}
}