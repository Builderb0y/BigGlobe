package builderb0y.bigglobe.structures;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;

@FunctionalInterface
public interface BigGlobeStructurePieceType extends StructurePieceType {

	public abstract StructurePiece load(StructurePieceType type, StructureContext context, NbtCompound nbt);

	@Override
	public default StructurePiece load(StructureContext context, NbtCompound nbt) {
		return this.load(this, context, nbt);
	}
}