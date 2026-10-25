package builderb0y.bigglobe.structures;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

@FunctionalInterface
public interface BigGlobeStructurePieceType extends StructurePieceType {

	public abstract StructurePiece load(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt);

	@Override
	public default StructurePiece load(StructurePieceSerializationContext context, CompoundTag nbt) {
		return this.load(this, context, nbt);
	}
}