package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import builderb0y.bigglobe.versions.HeightLimitViewVersions;

/**
vanilla logic picks a single random Y level to spawn at,
but in worlds as tall as big globe, there's a lot more opportunities
for the chosen Y level to be far away from any player.
this mixin changes the logic to choose more positions in taller worlds.

since this is a change to vanilla behavior that affects vanilla worlds,
this mixin is DISABLED BY DEFAULT, and must be manually enabled in
.minecraft/config/bigglobe/mixins.properties to have any effect.
*/
@Mixin(NaturalSpawner.class)
public class SpawnHelper_MoreMobsInTallerWorlds {

	/**
	@author Builderb0y
	@reason see javadocs for class.
	*/
	@Overwrite
	public static void spawnCategoryForChunk(
		MobCategory group,
		ServerLevel world,
		LevelChunk chunk,
		NaturalSpawner.SpawnPredicate checker,
		NaturalSpawner.AfterSpawnCallback runner
	) {
		for (int baseY = HeightLimitViewVersions.getMinY(world), topY = HeightLimitViewVersions.getMaxY(world); baseY < topY; baseY += 128) {
			int rng = world.getRandom().nextInt();
			int x = chunk.getPos().getMinBlockX() | (rng & 15);
			int z = chunk.getPos().getMinBlockZ() | ((rng >>> 4) & 15);
			int y = baseY | ((rng >>> 8) & 127);
			if (y <= chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1) {
				NaturalSpawner.spawnCategoryForPosition(group, world, chunk, new BlockPos(x, y, z), checker, runner);
			}
		}
	}
}