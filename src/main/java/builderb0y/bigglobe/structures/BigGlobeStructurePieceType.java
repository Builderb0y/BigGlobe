package builderb0y.bigglobe.structures;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;

@FunctionalInterface
public interface BigGlobeStructurePieceType extends StructurePieceType.Simple {

	public abstract StructurePiece load(StructurePieceType type, NbtCompound nbt);

	@Override
	public default StructurePiece load(NbtCompound nbt) {
		return this.load(this, nbt);
	}
}