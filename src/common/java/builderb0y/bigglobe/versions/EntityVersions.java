package builderb0y.bigglobe.versions;

import java.util.Collections;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import builderb0y.bigglobe.math.BigGlobeMath;

public class EntityVersions {

	public static Level getWorld(Entity entity) {

		return entity.level();
	}

	public static ServerLevel getServerWorld(ServerPlayer player) {

		return player.level();
	}

	@Environment(EnvType.CLIENT)
	public static ClientLevel getClientWorld(LocalPlayer player) {

		return (ClientLevel)(player.level());
	}

	public static MinecraftServer getServer(ServerPlayer player) {

		return player.level().getServer();
	}

	public static Vec3 getPos(Entity entity) {

		return entity.position();
	}

	public static boolean isOnGround(Entity entity) {
		return entity.onGround();
	}

	public static ItemStack getAmmunition(Player player, ItemStack weapon) {
		return player.getProjectile(weapon);
	}

	public static double getBlockReachDistance(Player player) {

		return player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
	}

	public static double getBlockReachDistanceSquared(Player player) {
		return BigGlobeMath.squareD(getBlockReachDistance(player));
	}

	public static double getEntityReachDistance(Player player) {

		return player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
	}

	public static double getEntityReachDistanceSquared(Player player) {
		return BigGlobeMath.squareD(getEntityReachDistance(player));
	}

	public static void setPortalCooldown(Entity entity, int cooldown) {
		entity.setPortalCooldown(cooldown);
	}

	public static AABB getBoundingBox(EntityType<?> type, double x, double y, double z) {

		return type.getSpawnAABB(x, y, z);
	}

	public static ServerPlayer teleport(ServerPlayer player, ServerLevel destinationWorld, Vec3 position, Vec3 velocity, float yaw, float pitch) {

		return player.teleport(
			new TeleportTransition(
				destinationWorld,
				position,
				velocity,
				yaw,
				pitch,
				false,
				false,
				Collections.emptySet(),
				TeleportTransition.DO_NOTHING
			)
		);
	}

	public static ResourceKey<Level> getRespawnDimension(ServerPlayer player) {

		return player.getRespawnConfig() != null ? player.getRespawnConfig().respawnData().dimension() : null;
	}

	public static BlockPos getRespawnPosition(ServerPlayer player) {

		return player.getRespawnConfig() != null ? player.getRespawnConfig().respawnData().pos() : null;
	}

	public static boolean isRespawnForced(ServerPlayer player) {

		return player.getRespawnConfig() != null && player.getRespawnConfig().forced();
	}

	public static float getRespawnAngle(ServerPlayer player) {

		return player.getRespawnConfig() != null ? player.getRespawnConfig().respawnData().yaw() : 0.0F;
	}

	public static double prevX(Entity entity) {
		return entity.xo;
	}

	public static double prevY(Entity entity) {
		return entity.yo;
	}

	public static double prevZ(Entity entity) {
		return entity.zo;
	}

	public static GameType getGameMode(ServerPlayer player) {

		return player.gameMode();
	}
}