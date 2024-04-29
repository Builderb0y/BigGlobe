package builderb0y.bigglobe.versions;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import builderb0y.bigglobe.math.BigGlobeMath;

#if MC_VERSION <= MC_1_19_4
import builderb0y.bigglobe.mixins.Entity_PortalCooldownSetter;
#endif

public class EntityVersions {

	public static World getWorld(Entity entity) {
		return entity.getWorld();
	}

	public static ServerWorld getServerWorld(ServerPlayerEntity player) {
		#if MC_VERSION < MC_1_20_0
			return player.getWorld();
		#else
			return player.getServerWorld();
		#endif
	}

	public static boolean isOnGround(Entity entity) {
		return entity.isOnGround();
	}

	public static ItemStack getAmmunition(PlayerEntity player, ItemStack weapon) {
		#if MC_VERSION <= MC_1_19_2
			return player.getArrowType(weapon);
		#else
			return player.getProjectileType(weapon);
		#endif
	}

	public static double getReachDistance(PlayerEntity player) {
		return 8.0D;
	}

	public static double getReachDistanceSquared(PlayerEntity player) {
		return BigGlobeMath.squareD(getReachDistance(player));
	}

	public static void setPortalCooldown(Entity entity, int cooldown) {
		#if MC_VERSION > MC_1_19_4
			entity.setPortalCooldown(cooldown);
		#else
			((Entity_PortalCooldownSetter)(entity)).bigglobe_setPortalCooldown(cooldown);
		#endif
	}
}