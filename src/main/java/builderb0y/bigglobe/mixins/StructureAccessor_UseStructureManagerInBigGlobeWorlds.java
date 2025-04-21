package builderb0y.bigglobe.mixins;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.structures.StructureManager.FinalStructures;
import builderb0y.bigglobe.structures.StructureManager.StructureGenerationParams;

@Mixin(StructureAccessor.class)
public abstract class StructureAccessor_UseStructureManagerInBigGlobeWorlds {

	@Shadow @Final private WorldAccess world;

	@Inject(method = "getStructureStarts(Lnet/minecraft/util/math/ChunkPos;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
	private void bigglobe_getIntersectingStarts(ChunkPos pos, Predicate<Structure> predicate, CallbackInfoReturnable<List<StructureStart>> callback) {
		if (this.world instanceof ServerWorldAccess serverWorldAccess && serverWorldAccess.toServerWorld().getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			FinalStructures structures = generator.structureManager.getIntersectingStructures(
				this.bigglobe_makeParams(serverWorldAccess.toServerWorld(), generator, pos)
			);
			if (structures.isEmpty() || predicate == Predicates.<Structure>alwaysTrue()) {
				callback.setReturnValue(Collections.unmodifiableList(structures));
			}
			else {
				callback.setReturnValue(
					structures
					.stream()
					.filter((StructureStart start) -> predicate.test(start.getStructure()))
					.toList()
				);
			}
		}
	}

	@Inject(method = "getStructureStarts(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/world/gen/structure/Structure;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
	private void bigglobe_getIntersectingStarts(ChunkSectionPos sectionPos, Structure structure, CallbackInfoReturnable<List<StructureStart>> callback) {
		if (this.world instanceof ServerWorldAccess serverWorldAccess && serverWorldAccess.toServerWorld().getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			FinalStructures structures = generator.structureManager.getIntersectingStructures(
				this.bigglobe_makeParams(serverWorldAccess.toServerWorld(), generator, sectionPos.toChunkPos())
			);
			if (structures.isEmpty()) {
				callback.setReturnValue(Collections.unmodifiableList(structures));
			}
			else {
				callback.setReturnValue(
					structures
					.stream()
					.filter((StructureStart start) -> start.getStructure() == structure)
					.toList()
				);
			}
		}
	}

	@Unique
	private StructureGenerationParams bigglobe_makeParams(ServerWorld world, BigGlobeScriptedChunkGenerator generator, ChunkPos chunkPos) {
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		Hints hints = ColumnUsage.GENERIC.maybeDhHints(distantHorizons);
		return new StructureGenerationParams(
			generator,
			generator.newColumnLookup(world, hints),
			world,
			chunkPos
		);
	}
}