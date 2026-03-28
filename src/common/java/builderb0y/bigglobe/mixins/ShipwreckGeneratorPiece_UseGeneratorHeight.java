package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
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
	@Redirect(method = "postProcess", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
	private int bigglobe_useGeneratorHeight(WorldGenLevel receiver, Heightmap.Types type, int x, int z, WorldGenLevel world, StructureManager structureAccessor, ChunkGenerator generator) {
		if (generator instanceof BigGlobeScriptedChunkGenerator) {
			return generator.getBaseHeight(x, z, type, world, ((ServerChunkCache)(world.getChunkSource())).randomState());
		}
		return receiver.getHeight(type, x, z);
	}
}