package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.OceanRuinPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(OceanRuinPieces.OceanRuinPiece.class)
public abstract class OceanRuinGeneratorPiece_UseGeneratorHeight extends TemplateStructurePiece {

	public OceanRuinGeneratorPiece_UseGeneratorHeight(
		StructurePieceType type,
		int length,
		StructureTemplateManager structureTemplateManager,
		Identifier id,
		String template,
		StructurePlaceSettings placementData,
		BlockPos pos
	) {
		super(type, length, structureTemplateManager, id, template, placementData, pos);
	}

	/**
	prevents ocean ruins from spawning on top of skylands.
	*/
	@WrapOperation(method = "postProcess", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
	private int bigglobe_useGeneratorHeight(WorldGenLevel world, Types type, int x, int z, Operation<Integer> original, @Local(argsOnly = true) ChunkGenerator generator) {
		if (generator instanceof BigGlobeScriptedChunkGenerator) {
			return this.boundingBox.minY();
		}
		return original.call(world, type, x, z);
	}
}