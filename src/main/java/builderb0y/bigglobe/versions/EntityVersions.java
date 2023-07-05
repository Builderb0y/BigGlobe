package builderb0y.bigglobe.versions;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class EntityVersions {

	public static World getWorld(Entity entity) {
		return entity.getWorld();
	}

	public static ServerWorld getServerWorld(ServerPlayerEntity player) {
		return player.getServerWorld();
	}

	public static boolean isOnGround(Entity entity) {
		return entity.isOnGround();
	}
}