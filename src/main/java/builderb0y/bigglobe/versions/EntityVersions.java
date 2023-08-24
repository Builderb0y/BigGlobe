package builderb0y.bigglobe.versions;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

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
}