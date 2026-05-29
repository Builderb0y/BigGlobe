package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.ChunkGenerator;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.structures.BigGlobeStructureTags;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.SpawnEntryVersions;

@Mixin(NaturalSpawner.class)
public class SpawnHelper_AllowSlimeSpawningInLakes {

	@Inject(
		method = "isValidSpawnPostitionForType(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;Lnet/minecraft/core/BlockPos$MutableBlockPos;D)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/SpawnPlacements;isSpawnPositionOk(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
		),
		cancellable = true
	)
	private static void bigglobe_canSpawn(
		ServerLevel world,
		MobCategory group,
		StructureManager structureAccessor,
		ChunkGenerator chunkGenerator,
		SpawnerData spawnEntry,
		BlockPos.MutableBlockPos pos,
		double squaredDistance,
		CallbackInfoReturnable<Boolean> callback
	) {
		if (
			SpawnEntryVersions.type(spawnEntry) == EntityType.SLIME
			&& world.isEmptyBlock(pos)
			&& world.getBlockState(pos.below()) == BlockStates.WATER
			&& (
				world
				.structureManager()
				.getStructureWithPieceAt(pos, BigGlobeStructureTags.SLIMES_SPAWN_ON_WATER)
				.isValid()
			)
			//vanilla logic.
			&& world.getRandom().nextFloat() < world.getMoonBrightness(pos)
			&& world.getMaxLocalRawBrightness(pos) <= world.getRandom().nextInt(8)
		) {
			callback.setReturnValue(
				//also vanilla logic.
				world.noCollision(
					EntityVersions.getBoundingBox(
						SpawnEntryVersions.type(spawnEntry),
						pos.getX() + 0.5,
						pos.getY(),
						pos.getZ() + 0.5
					)
				)
			);
		}
	}
}