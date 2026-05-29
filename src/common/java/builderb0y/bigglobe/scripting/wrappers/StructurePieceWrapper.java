package builderb0y.bigglobe.scripting.wrappers;

import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;

import builderb0y.bigglobe.util.Directions;
import builderb0y.scripting.bytecode.TypeInfo;

public class StructurePieceWrapper {

	public static final TypeInfo TYPE = TypeInfo.of(StructurePiece.class);

	public static int minX(StructurePiece piece) {
		return piece.getBoundingBox().minX();
	}

	public static int minY(StructurePiece piece) {
		return piece.getBoundingBox().minY();
	}

	public static int minZ(StructurePiece piece) {
		return piece.getBoundingBox().minZ();
	}

	public static int maxX(StructurePiece piece) {
		return piece.getBoundingBox().maxX();
	}

	public static int maxY(StructurePiece piece) {
		return piece.getBoundingBox().maxY();
	}

	public static int maxZ(StructurePiece piece) {
		return piece.getBoundingBox().maxZ();
	}

	public static int midX(StructurePiece piece) {
		return (piece.getBoundingBox().minX() + piece.getBoundingBox().maxX() + 1) >> 1;
	}

	public static int midY(StructurePiece piece) {
		return (piece.getBoundingBox().minY() + piece.getBoundingBox().maxY() + 1) >> 1;
	}

	public static int midZ(StructurePiece piece) {
		return (piece.getBoundingBox().minZ() + piece.getBoundingBox().maxZ() + 1) >> 1;
	}

	public static int sizeX(StructurePiece piece) {
		return piece.getBoundingBox().maxX() - piece.getBoundingBox().minX() + 1;
	}

	public static int sizeY(StructurePiece piece) {
		return piece.getBoundingBox().maxY() - piece.getBoundingBox().minY() + 1;
	}

	public static int sizeZ(StructurePiece piece) {
		return piece.getBoundingBox().maxZ() - piece.getBoundingBox().minZ() + 1;
	}

	public static int rotation(StructurePiece piece) {
		return Directions.reverseScriptRotation(piece.getRotation());
	}

	public static String mirror(StructurePiece piece) {
		return Directions.reverseScriptMirror(piece.getMirror());
	}

	public static StructurePieceType type(StructurePiece piece) {
		return piece.getType();
	}

	public static boolean hasPreferredTerrainHeight(StructurePiece piece) {
		return piece instanceof PoolElementStructurePiece pool && pool.getElement().getProjection() == Projection.RIGID;
	}

	public static int preferredTerrainHeight(StructurePiece piece) {
		int y = piece.getBoundingBox().minY();
		return piece instanceof PoolElementStructurePiece pool ? pool.getGroundLevelDelta() + y : y;
	}
}