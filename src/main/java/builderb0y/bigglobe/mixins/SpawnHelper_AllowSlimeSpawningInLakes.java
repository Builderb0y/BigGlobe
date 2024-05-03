package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.structures.BigGlobeStructureTags;
import builderb0y.bigglobe.versions.EntityVersions;

@Mixin(SpawnHelper.class)
public class SpawnHelper_AllowSlimeSpawningInLakes {

	@Inject(
		method = "canSpawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/world/biome/SpawnSettings$SpawnEntry;Lnet/minecraft/util/math/BlockPos$Mutable;D)Z",
		at = @At(
			value = "INVOKE",
			#if MC_VERSION >= MC_1_20_5
			target = "Lnet/minecraft/entity/SpawnRestriction;isSpawnPosAllowed(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)Z"
			#else
			target = "Lnet/minecraft/entity/SpawnRestriction;getLocation(Lnet/minecraft/entity/EntityType;)Lnet/minecraft/entity/SpawnRestriction$Location;"
			#endif
		),
		cancellable = true
	)
	private static void bigglobe_canSpawn(
		ServerWorld world,
		SpawnGroup group,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		SpawnEntry spawnEntry,
		BlockPos.Mutable pos,
		double squaredDistance,
		CallbackInfoReturnable<Boolean> callback
	) {
		if (
			spawnEntry.type == EntityType.SLIME
			&& world.isAir(pos)
			&& world.getBlockState(pos.down()) == BlockStates.WATER
			&& (
				world
				.getStructureAccessor()
				.getStructureContaining(pos, BigGlobeStructureTags.SLIMES_SPAWN_ON_WATER)
				.hasChildren()
			)
			//vanilla logic.
			&& world.random.nextFloat() < world.getMoonSize()
			&& world.getLightLevel(pos) <= world.random.nextInt(8)
		) {
			callback.setReturnValue(
				//also vanilla logic.
				world.isSpaceEmpty(
					EntityVersions.getBoundingBox(
						spawnEntry.type,
						pos.getX() + 0.5,
						pos.getY(),
						pos.getZ() + 0.5
					)
				)
			);
		}
	}
}