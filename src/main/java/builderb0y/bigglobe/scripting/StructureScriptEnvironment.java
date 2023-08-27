package builderb0y.bigglobe.scripting;

import java.util.random.RandomGenerator;

import builderb0y.bigglobe.scripting.wrappers.*;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure;
import builderb0y.scripting.environments.MutableScriptEnvironment;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class StructureScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()

		.addType("StructureStart",           StructureStartWrapper   .TYPE)
		.addType("StructurePiece",           StructurePieceWrapper   .TYPE)
		.addType("Structure",                StructureEntry          .TYPE)
		.addType("StructureTag",             StructureTagKey         .TYPE)
		.addType("StructureType",            StructureTypeEntry      .TYPE)
		.addType("StructureTypeTag",         StructureTypeTagKey     .TYPE)
		.addType("StructurePieceType",       StructurePieceTypeEntry .TYPE)
		.addType("StructurePieceTypeTag",    StructurePieceTypeTagKey.TYPE)
		.addType("ScriptStructurePiece",     type(ScriptedStructure.Piece.class))
		.addType("StructurePlacementScript", type(StructurePlacementScriptEntry.class))
		.addFieldInvokes(StructureStartWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "midX", "midY", "midZ", "sizeX", "sizeY", "sizeZ", "structure", "pieces")
		.addFieldInvokeStatics(StructurePieceWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "midX", "midY", "midZ", "sizeX", "sizeY", "sizeZ", "type", "hasPreferredTerrainHeight", "preferredTerrainHeight")
		.addMethodInvokeSpecific(StructurePieceTypeTagKey.class, "random", StructurePieceTypeEntry.class, RandomGenerator.class)
		.addMethodInvokeSpecific(StructurePieceTypeTagKey.class, "random", StructurePieceTypeEntry.class, long.class)
		.addMethodInvokeSpecific(StructureEntry.class, "isIn", boolean.class, StructureTagKey.class)
		.addFieldInvokes(StructureEntry.class, "type", "generationStep")
		.addMethodInvokeSpecific(StructureTagKey.class, "random", StructureEntry.class, RandomGenerator.class)
		.addMethodInvokeSpecific(StructureTagKey.class, "random", StructureEntry.class, long.class)
		.addMethodInvokeSpecific(StructureTypeEntry.class, "isIn", boolean.class, StructureTypeTagKey.class)
		.addMethodInvokeSpecific(StructureTypeTagKey.class, "random", StructureTypeEntry.class, RandomGenerator.class)
		.addMethodInvokeSpecific(StructureTypeTagKey.class, "random", StructureTypeEntry.class, long.class)
		.addFieldInvokes(ScriptedStructure.Piece.class, "symmetry", "offsetX", "offsetZ", "placement")

		.addCastConstant(StructureEntry               .CONSTANT_FACTORY, true)
		.addCastConstant(StructureTagKey              .CONSTANT_FACTORY, true)
		.addCastConstant(StructureTypeEntry           .CONSTANT_FACTORY, true)
		.addCastConstant(StructureTypeTagKey          .CONSTANT_FACTORY, true)
		.addCastConstant(StructurePieceTypeEntry      .CONSTANT_FACTORY, true)
		.addCastConstant(StructurePieceTypeTagKey     .CONSTANT_FACTORY, true)
		.addCastConstant(StructurePlacementScriptEntry.CONSTANT_FACTORY, true)
	);
}