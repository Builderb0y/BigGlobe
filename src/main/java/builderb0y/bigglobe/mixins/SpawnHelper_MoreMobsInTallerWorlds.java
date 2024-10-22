package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;

/**
vanilla logic picks a single random Y level to spawn at,
but in worlds as tall as big globe, there's a lot more opportunities
for the chosen Y level to be far away from any player.
this mixin changes the logic to choose more positions in taller worlds.

since this is a change to vanilla behavior that affects vanilla worlds,
this mixin is DISABLED BY DEFAULT, and must be manually enabled in
.minecraft/config/bigglobe/mixins.properties to have any effect.
*/
@Mixin(SpawnHelper.class)
public class SpawnHelper_MoreMobsInTallerWorlds {

	/**
	@author Builderb0y
	@reason see javadocs for class.
	*/
	@Overwrite
	public static void spawnEntitiesInChunk(
		SpawnGroup group,
		ServerWorld world,
		WorldChunk chunk,
		SpawnHelper.Checker checker,
		SpawnHelper.Runner runner
	) {
		for (int baseY = world.getBottomY(), topY = world.getTopY(); baseY < topY; baseY += 128) {
			int rng = world.random.nextInt();
			int x = chunk.getPos().getStartX() | (rng & 15);
			int z = chunk.getPos().getStartZ() | ((rng >>> 4) & 15);
			int y = baseY | ((rng >>> 8) & 127);
			if (y <= chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z) + 1) {
				SpawnHelper.spawnEntitiesInChunk(group, world, chunk, new BlockPos(x, y, z), checker, runner);
			}
		}
	}
}