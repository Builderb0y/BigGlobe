package builderb0y.bigglobe.scripting;

import java.util.random.RandomGenerator;

import builderb0y.bigglobe.scripting.wrappers.*;
import builderb0y.scripting.environments.MutableScriptEnvironment;

public class StructureScriptEnvironment extends MutableScriptEnvironment {

	public static final StructureScriptEnvironment INSTANCE = new StructureScriptEnvironment();

	public StructureScriptEnvironment() {
		this

		.addType("StructureStart",     StructureStartWrapper    .TYPE)
		.addType("StructurePiece",     StructurePieceWrapper    .TYPE)
		.addType("Structure",          StructureEntry           .TYPE)
		.addType("StructureTag",       StructureTagKey          .TYPE)
		.addType("StructureType",      StructureTypeEntry       .TYPE)
		.addType("StructureTypeTag",   StructureTypeTagKey      .TYPE)
		.addType("StructurePieceType", StructurePieceTypeWrapper.TYPE)
		.addFieldInvokes(StructureStartWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "midX", "midY", "midZ", "sizeX", "sizeY", "sizeZ", "structure", "pieces")
		.addFieldInvokeStatics(StructurePieceWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "midX", "midY", "midZ", "sizeX", "sizeY", "sizeZ", "type", "hasPreferredTerrainHeight", "preferredTerrainHeight")
		.addMethodInvoke(StructureEntry.class, "isIn")
		.addFieldInvokes(StructureEntry.class, "type", "generationStep")
		.addMethodInvokeSpecific(StructureTagKey.class, "random", StructureEntry.class, RandomGenerator.class)
		.addMethodInvoke(StructureTypeEntry.class, "isIn")
		.addMethodInvokeSpecific(StructureTypeTagKey.class, "random", StructureTypeEntry.class, RandomGenerator.class)

		.addCastConstant(StructureEntry           .CONSTANT_FACTORY, "Structure",          true)
		.addCastConstant(StructureTagKey          .CONSTANT_FACTORY, "StructureTag",       true)
		.addCastConstant(StructureTypeEntry       .CONSTANT_FACTORY, "StructureType",      true)
		.addCastConstant(StructureTypeTagKey      .CONSTANT_FACTORY, "StructureTypeTag",   true)
		.addCastConstant(StructurePieceTypeWrapper.CONSTANT_FACTORY, "StructurePieceType", true)

		;
	}
}