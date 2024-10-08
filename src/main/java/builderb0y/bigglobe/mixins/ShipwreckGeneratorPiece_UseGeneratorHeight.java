package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(ShipwreckGenerator.Piece.class)
public abstract class ShipwreckGeneratorPiece_UseGeneratorHeight extends SimpleStructurePiece {

	public ShipwreckGeneratorPiece_UseGeneratorHeight(
		StructurePieceType type,
		int length,
		StructureTemplateManager structureTemplateManager,
		Identifier id,
		String template,
		StructurePlacementData placementData,
		BlockPos pos
	) {
		super(type, length, structureTemplateManager, id, template, placementData, pos);
	}

	/** prevents shipwrecks from spawning on top of skylands. */
	@Redirect(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/StructureWorldAccess;getTopY(Lnet/minecraft/world/Heightmap$Type;II)I"))
	private int bigglobe_useGeneratorHeight(StructureWorldAccess receiver, Heightmap.Type type, int x, int z, StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator generator) {
		if (generator instanceof BigGlobeScriptedChunkGenerator) {
			return generator.getHeight(x, z, type, world, ((ServerChunkManager)(world.getChunkManager())).getNoiseConfig());
		}
		return receiver.getTopY(type, x, z);
	}
}