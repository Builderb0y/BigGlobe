package builderb0y.bigglobe.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.poi.PointOfInterestStorage;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

/**
by default, big globe will expand bounding boxes of
structures to 16 blocks past their initial bounds.
this is made more complicated by the fact that
the bounding box is already expanded by 12 blocks
whenever the terrain adaptation isn't NONE.
regardless, big globe does this to give
overriders a bit more room to work with, to
create smoother slopes easing into the structure.

the problem is that my expansion isn't saved to disk,
so when the start is reloaded from disk,
it won't have an expanded bounding box anymore.
and *this* is an issue because if the start
unloaded while in an ungenerated chunk,
then when the chunk generates,
it won't have smooth slopes easing into the structure anymore.

solution 1: re-expand the bounding box elsewhere if the
ChunkGenerator is an instance of {@link BigGlobeScriptedChunkGenerator}.

solution 2: save the bounding box to disk.

I opted for solution 2, because it feels more stable,
and also because there isn't a lot of overlap between
"places I could put a post-deserialization hook" and
"places that have access to a ChunkGenerator in some way".
the only real candidate that I could find is in
{@link ChunkSerializer#deserialize(ServerWorld, PointOfInterestStorage, ChunkPos, NbtCompound)},
and this location is a bit awkward to work with.
*/
@Mixin(StructureStart.class)
public class StructureStart_SaveBoundingBox {

	@Shadow private volatile @Nullable BlockBox boundingBox;

	@Inject(method = "fromNbt", at = @At("RETURN"))
	private static void readBoundingBox(StructureContext context, NbtCompound nbt, long seed, CallbackInfoReturnable<StructureStart> callback) {
		StructureStart start = callback.getReturnValue();
		if (start != null && start != StructureStart.DEFAULT) {
			NbtElement boxNBT = nbt.get("bigglobe:bounding_box");
			if (boxNBT != null) {
				BlockBox box = BlockBox.CODEC.parse(NbtOps.INSTANCE, boxNBT).result().orElse(null);
				if (box != null) {
					((StructureStart_BoundingBoxSetter)(Object)(start)).bigglobe_setBoundingBox(box);
				}
			}
		}
	}

	@Inject(method = "toNbt", at = @At("RETURN"))
	private void writeBoundingBox(StructureContext context, ChunkPos chunkPos, CallbackInfoReturnable<NbtCompound> callback) {
		BlockBox box = this.boundingBox;
		if (box != null) {
			NbtElement boxNBT = BlockBox.CODEC.encodeStart(NbtOps.INSTANCE, box).result().orElse(null);
			if (boxNBT != null) {
				callback.getReturnValue().put("bigglobe:bounding_box", boxNBT);
			}
		}
	}
}