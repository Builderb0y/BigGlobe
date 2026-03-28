package builderb0y.bigglobe.scripting.environments;

import java.util.random.RandomGenerator;

import builderb0y.bigglobe.scripting.wrappers.StructurePieceWrapper;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.scripting.wrappers.entries.StructureEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.StructurePieceTypeEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.StructurePlacementScriptEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.StructureTypeEntry;
import builderb0y.bigglobe.scripting.wrappers.tags.StructurePieceTypeTag;
import builderb0y.bigglobe.scripting.wrappers.tags.StructurePlacementScriptTag;
import builderb0y.bigglobe.scripting.wrappers.tags.StructureTag;
import builderb0y.bigglobe.scripting.wrappers.tags.StructureTypeTag;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure;
import builderb0y.scripting.environments.MutableScriptEnvironment;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class StructureScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()

			.addType("StructureStart", StructureStartWrapper.TYPE)
			.addType("StructurePiece", StructurePieceWrapper.TYPE)
			.addType("Structure", StructureEntry.TYPE)
			.addType("StructureTag", StructureTag.TYPE)
			.addType("StructureType", StructureTypeEntry.TYPE)
			.addType("StructureTypeTag", StructureTypeTag.TYPE)
			.addType("StructurePieceType", StructurePieceTypeEntry.TYPE)
			.addType("StructurePieceTypeTag", StructurePieceTypeTag.TYPE)
			.addType("ScriptStructurePiece", type(ScriptedStructure.Piece.class))
			.addType("StructurePlacementScript", type(StructurePlacementScriptEntry.class))

			.addFieldInvokes(StructureStartWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "midX", "midY", "midZ", "sizeX", "sizeY", "sizeZ", "structure", "pieces")
			.addFieldInvokeStatics(StructurePieceWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "midX", "midY", "midZ", "sizeX", "sizeY", "sizeZ", "rotation", "mirror", "type", "hasPreferredTerrainHeight", "preferredTerrainHeight")
			.addMethodInvokeSpecific(StructurePieceTypeTag.class, "random", StructurePieceTypeEntry.class, RandomGenerator.class)
			.addMethodInvokeSpecific(StructurePieceTypeTag.class, "random", StructurePieceTypeEntry.class, long.class)
			.addFieldInvokes(StructureEntry.class, "type", "generationStep", "validBiomes", "terrainAdaptation")
			.addMethodInvokeSpecific(StructureTag.class, "random", StructureEntry.class, RandomGenerator.class)
			.addMethodInvokeSpecific(StructureTag.class, "random", StructureEntry.class, long.class)
			.addMethodInvokeSpecific(StructureTypeTag.class, "random", StructureTypeEntry.class, RandomGenerator.class)
			.addMethodInvokeSpecific(StructureTypeTag.class, "random", StructureTypeEntry.class, long.class)
			.addFieldInvokes(ScriptedStructure.Piece.class, "symmetry", "offsetX", "offsetZ", "placement")

			.addCastConstant(StructureEntry.CONSTANT_FACTORY, true)
			.addCastConstant(StructureTypeEntry.CONSTANT_FACTORY, true)
			.addCastConstant(StructurePieceTypeEntry.CONSTANT_FACTORY, true)
			.addCastConstant(StructurePlacementScriptEntry.CONSTANT_FACTORY, true)
			.configure(StructureTag.PARSER)
			.configure(StructureTypeTag.PARSER)
			.configure(StructurePieceTypeTag.PARSER)
			.configure(StructurePlacementScriptTag.PARSER)
	);
}