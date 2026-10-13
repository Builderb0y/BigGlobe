package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(ShipwreckPieces.ShipwreckPiece.class)
public abstract class ShipwreckGeneratorPiece_UseGeneratorHeight extends TemplateStructurePiece {

	public ShipwreckGeneratorPiece_UseGeneratorHeight(
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
	prevents shipwrecks from spawning on top of skylands.
	*/
	@ModifyArg(method = "postProcess", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/structures/ShipwreckPieces$ShipwreckPiece;adjustPositionHeight(I)V"))
	private int bigglobe_useOriginalPosition(int original, @Local(argsOnly = true) ChunkGenerator generator) {
		if (generator instanceof BigGlobeScriptedChunkGenerator) {
			return this.boundingBox.minY();
		}
		return original;
	}
}