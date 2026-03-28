package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.overriders.StructureOverrider;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.IglooPieces;

@Mixin(IglooPieces.IglooPiece.class)
public abstract class IglooGeneratorPiece_DontMoveInBigGlobeWorlds extends TemplateStructurePiece {

	public IglooGeneratorPiece_DontMoveInBigGlobeWorlds() {
		super(null, 0, null, null, null, null, null);
	}

	/**
	igloos spawn at Y90, always. when generate() is called, they move down to the correct position.

	why this is important: data/bigglobe/worldgen/configured_feature/overworld/overriders/height/igloo.json
	tries to query their bounding box before they move,
	and gets the wrong bounding box. this causes them to be placed on a hill or in a pit.
	to get around this, I would normally just move the structure via
	{@link StructureOverrider#move(StructureStartWrapper, int)},
	but they still move when generating, so now they are underground or floating in the sky.
	so, I now need to prevent them from moving on their own when I already moved them myself.
	*/
	@Redirect(method = "postProcess", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;"))
	private BlockPos bigglobe_dontAdd(BlockPos instance, int dx, int dy, int dz, WorldGenLevel world, StructureManager structureAccessor, ChunkGenerator chunkGenerator) {
		if (chunkGenerator instanceof BigGlobeScriptedChunkGenerator) {
			return instance;
		}
		else {
			return instance.offset(dx, dy, dz);
		}
	}
}