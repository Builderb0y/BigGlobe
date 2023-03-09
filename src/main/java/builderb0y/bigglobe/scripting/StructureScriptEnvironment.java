package builderb0y.bigglobe.scripting;

import java.util.random.RandomGenerator;

import builderb0y.bigglobe.scripting.wrappers.*;
import builderb0y.scripting.bytecode.CastingSupport.CastProvider;
import builderb0y.scripting.bytecode.CastingSupport.ConstantCaster;
import builderb0y.scripting.bytecode.CastingSupport.LookupCastProvider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

public class StructureScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
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
	);

	public static final CastProvider CAST_PROVIDER = (
		new LookupCastProvider()
		.append(TypeInfos.STRING, StructureEntry           .TYPE, true, new ConstantCaster(StructureEntry           .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, StructureTagKey          .TYPE, true, new ConstantCaster(StructureTagKey          .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, StructureTypeEntry       .TYPE, true, new ConstantCaster(StructureTypeEntry       .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, StructureTypeTagKey      .TYPE, true, new ConstantCaster(StructureTypeTagKey      .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, StructurePieceTypeWrapper.TYPE, true, new ConstantCaster(StructurePieceTypeWrapper.CONSTANT_FACTORY))
	);
}