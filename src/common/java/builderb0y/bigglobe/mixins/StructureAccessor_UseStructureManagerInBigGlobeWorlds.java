package builderb0y.bigglobe.mixins;

import java.util.List;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.DelegatingStructure;
import builderb0y.bigglobe.structures.management.StructureLocator;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor.ManyStructuresOneBox;
import builderb0y.bigglobe.util.Streamable;
import builderb0y.bigglobe.util.WorldUtil;

@Mixin(StructureManager.class)
public abstract class StructureAccessor_UseStructureManagerInBigGlobeWorlds {

	@Shadow
	@Final
	private LevelAccessor level;

	@Inject(method = "startsForStructure(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
	private void bigglobe_getIntersectingStarts(ChunkPos pos, Predicate<Structure> predicate, CallbackInfoReturnable<List<StructureStart>> callback) {
		if (this.level instanceof ServerLevelAccessor serverWorldAccess && serverWorldAccess.getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			Streamable<Holder<Structure>> structures = generator.structureLocator().allStructures();
			callback.setReturnValue(
				generator.structureLocator().getStructuresIntersecting(
					this.bigglobe_makeParams(
						serverWorldAccess.getLevel(),
						generator,
						WorldUtil.chunkBox(pos, serverWorldAccess),
						() -> structures.stream().filter((Holder<Structure> structure) -> predicate.test(structure.value()))
					)
				)
				.map(StructureStartWrapper::start)
				.toList()
			);
		}
	}

	@Inject(method = "startsForStructure(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
	private void bigglobe_getIntersectingStarts(SectionPos sectionPos, Structure structure, CallbackInfoReturnable<List<StructureStart>> callback) {
		if (this.level instanceof ServerLevelAccessor serverWorldAccess && serverWorldAccess.getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			Holder<Structure> entry = StructureLocator.toHolder(serverWorldAccess, structure);
			callback.setReturnValue(
				generator.structureLocator().getStructuresIntersecting(
					this.bigglobe_makeParams(
						serverWorldAccess.getLevel(),
						generator,
						WorldUtil.chunkBox(sectionPos.chunk(), serverWorldAccess),
						Streamable.singleton(entry)
					)
				)
				.map(StructureStartWrapper::start)
				.toList()
			);
		}
	}

	@ModifyArg(
		method = {
			"getStructureWithPieceAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/tags/TagKey;)Lnet/minecraft/world/level/levelgen/structure/StructureStart;",
			"getStructureWithPieceAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/HolderSet;)Lnet/minecraft/world/level/levelgen/structure/StructureStart;"
		},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/StructureManager;getStructureWithPieceAt(Lnet/minecraft/core/BlockPos;Ljava/util/function/Predicate;)Lnet/minecraft/world/level/levelgen/structure/StructureStart;"
		)
	)
	private Predicate<Holder<Structure>> bigglobe_handleDelegates(Predicate<Holder<Structure>> original) {
		return (Holder<Structure> structure) -> {
			while (structure.value() instanceof DelegatingStructure delegating) {
				structure = delegating.delegate();
			}
			return original.test(structure);
		};
	}

	@Unique
	private StructureLocator.Params bigglobe_makeParams(ServerLevel world, BigGlobeScriptedChunkGenerator generator, BoundingBox area, Streamable<Holder<Structure>> structures) {
		return new StructureLocator.Params(
			generator,
			generator.configuredColumnFactory(
				world,
				ColumnUsage.GENERIC.maybeDhHints()
			),
			world,
			new ManyStructuresOneBox(
				structures,
				area
			)
		);
	}
}