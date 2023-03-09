package builderb0y.bigglobe.scripting.wrappers;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.pool.StructurePool.Projection;

import builderb0y.scripting.bytecode.TypeInfo;

public class StructurePieceWrapper {

	public static final TypeInfo TYPE = TypeInfo.of(StructurePiece.class);

	public static int minX(StructurePiece piece) {
		return piece.getBoundingBox().getMinX();
	}

	public static int minY(StructurePiece piece) {
		return piece.getBoundingBox().getMinY();
	}

	public static int minZ(StructurePiece piece) {
		return piece.getBoundingBox().getMinZ();
	}

	public static int maxX(StructurePiece piece) {
		return piece.getBoundingBox().getMaxX();
	}

	public static int maxY(StructurePiece piece) {
		return piece.getBoundingBox().getMaxY();
	}

	public static int maxZ(StructurePiece piece) {
		return piece.getBoundingBox().getMaxZ();
	}

	public static StructurePieceType type(StructurePiece piece) {
		return piece.getType();
	}

	public static boolean hasPreferredTerrainHeight(StructurePiece piece) {
		return piece instanceof PoolStructurePiece pool && pool.getPoolElement().getProjection() == Projection.RIGID;
	}

	public static int preferredTerrainHeight(StructurePiece piece) {
		int y = piece.getBoundingBox().getMinY();
		return piece instanceof PoolStructurePiece pool ? pool.getGroundLevelDelta() + y : y;
	}
}