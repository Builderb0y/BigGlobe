package builderb0y.bigglobe.scripting;

import java.util.random.RandomGenerator;

import net.minecraft.block.Block;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.Wrappers.*;
import builderb0y.scripting.bytecode.CastingSupport.CastProvider;
import builderb0y.scripting.bytecode.CastingSupport.ConstantCaster;
import builderb0y.scripting.bytecode.CastingSupport.LookupCastProvider;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment2;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MinecraftScriptEnvironment extends MutableScriptEnvironment2 {

	public static final CastProvider CAST_PROVIDER = (
		new LookupCastProvider()

		.append(TypeInfos.STRING, BlockTagKey            .TYPE, true, new ConstantCaster(BlockTagKey            .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, BlockWrapper           .TYPE, true, new ConstantCaster(BlockWrapper           .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, BlockStateWrapper      .TYPE, true, new ConstantCaster(BlockStateWrapper      .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, BiomeEntry             .TYPE, true, new ConstantCaster(BiomeEntry             .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, BiomeTagKey            .TYPE, true, new ConstantCaster(BiomeTagKey            .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, ConfiguredFeatureEntry .TYPE, true, new ConstantCaster(ConfiguredFeatureEntry .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, ConfiguredFeatureTagKey.TYPE, true, new ConstantCaster(ConfiguredFeatureTagKey.CONSTANT_FACTORY))
		.append(TypeInfos.STRING, StructureEntry         .TYPE, true, new ConstantCaster(StructureEntry         .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, StructureTagKey        .TYPE, true, new ConstantCaster(StructureTagKey        .CONSTANT_FACTORY))
	);

	public MinecraftScriptEnvironment(InsnTree loadWorld) {
		InsnTree loadRandom = InsnTrees.getField(loadWorld, field(ACC_PUBLIC | ACC_FINAL, WorldWrapper.class, "permuter", Permuter.class));

		this
		.addVariableRenamedInvoke(loadWorld, "worldSeed", method(ACC_PUBLIC | ACC_PURE, WorldWrapper.TYPE, "getSeed", TypeInfos.LONG))
		.addFunction("block",                BlockWrapper           .CONSTANT_FACTORY)
		.addFunction("blockState",           BlockStateWrapper      .CONSTANT_FACTORY)
		.addFunction("blockTag",             BlockTagKey            .CONSTANT_FACTORY)
		.addFunction("biome",                BiomeEntry             .CONSTANT_FACTORY)
		.addFunction("biomeTag",             BiomeTagKey            .CONSTANT_FACTORY)
		.addFunction("configuredFeature",    ConfiguredFeatureEntry .CONSTANT_FACTORY)
		.addFunction("configuredFeatureTag", ConfiguredFeatureTagKey.CONSTANT_FACTORY)
		.addFunctionInvokes(loadWorld, WorldWrapper.class, "getBlockState", "setBlockState", "placeBlockState", "fillBlockState", "placeFeature", "getBiome", "isYLevelValid", "getBlockData", "setBlockData", "mergeBlockData")
		.addType("Block",                BlockWrapper           .TYPE)
		.addType("BlockTag",             BlockTagKey            .TYPE)
		.addType("BlockState",           BlockStateWrapper      .TYPE)
		.addType("Biome",                BiomeEntry             .TYPE)
		.addType("BiomeTag",             BiomeTagKey            .TYPE)
		.addType("ConfiguredFeature",    ConfiguredFeatureEntry .TYPE)
		.addType("ConfiguredFeatureTag", ConfiguredFeatureTagKey.TYPE)
		.addType("StructureStart",       StructureStartWrapper  .TYPE)
		.addType("StructurePiece",       StructurePieceWrapper  .TYPE)
		.addType("Structure",            StructureEntry         .TYPE)
		.addType("StructureTag",         StructureTagKey        .TYPE)
		.addFieldInvokes(BiomeEntry.class, "temperature", "downfall", "precipitation")
		.addFieldInvokes(StructureStartWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "structure", "pieces")
		.addFieldInvokeStatics(StructurePieceWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ")
		.addMethodInvokeStatics(BlockWrapper.class, "getDefaultState", "isIn")
		.addMethodInvokeSpecific(BlockTagKey.class, "random", Block.class, RandomGenerator.class)
		.addMethod(BlockTagKey.TYPE, "random", randomFromWorld(loadRandom, BlockTagKey.class, Block.class))
		.addMethodInvokeStatics(BlockStateWrapper.class, "isIn", "getBlock", "isAir", "isReplaceable", "hasWater", "hasLava", "blocksLight", "hasCollision", "hasFullCubeCollision", "rotate", "mirror", "with")
		.addMethod(BlockStateWrapper.TYPE, "canPlaceAt", (parser, receiver, name, arguments) -> {
			InsnTree[] position = ScriptEnvironment.castArguments(parser, "canPlaceAt", types("III"), CastMode.IMPLICIT_THROW, arguments);
			return invokeStatic(MethodInfo.getMethod(BlockStateWrapper.class, "canPlaceAt"), loadWorld, receiver, position[0], position[1], position[2]);
		})
		.addMethodInvoke(BiomeEntry.class, "isIn")
		.addMethodInvokeSpecific(BiomeTagKey.class, "random", BiomeEntry.class, RandomGenerator.class)
		.addMethod(BiomeTagKey.TYPE, "random", randomFromWorld(loadRandom, BiomeTagKey.class, BiomeEntry.class))
		.addMethodInvoke(ConfiguredFeatureEntry.class, "isIn")
		.addMethodInvokeSpecific(ConfiguredFeatureTagKey.class, "random", ConfiguredFeatureEntry.class, RandomGenerator.class)
		.addMethod(ConfiguredFeatureTagKey.TYPE, "random", randomFromWorld(loadRandom, ConfiguredFeatureTagKey.class, ConfiguredFeatureEntry.class))
		.addMethodInvoke(StructureEntry.class, "isIn")
		.addMethodInvokeSpecific(StructureTagKey.class, "random", StructureEntry.class, RandomGenerator.class)
		.addMethod(StructureTagKey.TYPE, "random", randomFromWorld(loadRandom, StructureTagKey.class, StructureEntry.class))
		;
	}

	public static MethodHandler randomFromWorld(InsnTree loadRandom, Class<?> owner, Class<?> returnType) {
		MethodInfo randomFunction = MethodInfo.findMethod(owner, "random", returnType, RandomGenerator.class);
		return (parser, receiver, name, arguments) -> {
			if (arguments.length == 0) {
				return invokeVirtual(receiver, randomFunction, loadRandom);
			}
			return null;
		};
	}
}