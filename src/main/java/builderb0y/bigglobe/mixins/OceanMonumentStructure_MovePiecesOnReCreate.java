package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.OceanMonumentStructure;

/**
{@link OceanMonumentStructure#modifyPiecesOnRead(ChunkPos, long, StructurePiecesList)}
re-creates the structure start from scratch, at its original position,
which is a problem if a script structure overrider changed its position.
in this case, where the structure is loaded, unloaded, and reloaded again
before getting a chance to place any blocks, its position will be reset.
normally what I would do is only disable that method in big globe worlds,
for the sole reason that I don't know what purpose it serves in vanilla worlds
and I don't want to accidentally break something, but it turns out in this case
there is no reference to a world or a ChunkGenerator anywhere in the parameters
of this method or the method that calls it. so I can't selectively only disable
it in big globe worlds.

UPDATE: I figured out what the purpose of this is in vanilla worlds:
ocean monuments don't save their pieces to NBT data. like at all.
they must be re-created from scratch or else they will not exist.
so... yeah. the new plan is to move the pieces to the old Y level.
*/
@Mixin(OceanMonumentStructure.class)
public class OceanMonumentStructure_MovePiecesOnReCreate {

	@Inject(method = "modifyPiecesOnRead", at = @At("RETURN"))
	private static void bigglobe_movePiecesOnReCreate(ChunkPos pos, long worldSeed, StructurePiecesList pieces, CallbackInfoReturnable<StructurePiecesList> callback) {
		//should never be empty, but it never hurts to double check this.
		if (!pieces.pieces().isEmpty() && !callback.getReturnValue().pieces().isEmpty()) {
			StructurePiece oldPiece = pieces.pieces().get(0);
			StructurePiece newPiece = callback.getReturnValue().pieces().get(0);
			newPiece.translate(0, oldPiece.getBoundingBox().getMinY() - newPiece.getBoundingBox().getMinY(), 0);
		}
	}
}