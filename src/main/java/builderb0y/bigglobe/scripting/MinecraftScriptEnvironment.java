package builderb0y.bigglobe.scripting;

import java.util.random.RandomGenerator;

import net.minecraft.block.Block;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.*;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MinecraftScriptEnvironment extends MutableScriptEnvironment {

	public MinecraftScriptEnvironment(InsnTree loadWorld) {
		InsnTree loadRandom = InsnTrees.getField(loadWorld, field(ACC_PUBLIC | ACC_FINAL, WorldWrapper.class, "permuter", Permuter.class));

		this
		.addVariableRenamedInvoke(loadWorld, "worldSeed", method(ACC_PUBLIC | ACC_PURE, WorldWrapper.TYPE, "getSeed", TypeInfos.LONG))
		.addFunctionInvokes(loadWorld, WorldWrapper.class, "getBlockState", "setBlockState", "placeBlockState", "fillBlockState", "placeFeature", "getBiome", "isYLevelValid", "getBlockData", "setBlockData", "mergeBlockData")
		.addType("Block",                BlockWrapper           .TYPE)
		.addType("BlockTag",             BlockTagKey            .TYPE)
		.addType("BlockState",           BlockStateWrapper      .TYPE)
		.addType("Biome",                BiomeEntry             .TYPE)
		.addType("BiomeTag",             BiomeTagKey            .TYPE)
		.addType("ConfiguredFeature",    ConfiguredFeatureEntry .TYPE)
		.addType("ConfiguredFeatureTag", ConfiguredFeatureTagKey.TYPE)
		.addFieldInvokes(BiomeEntry.class, "temperature", "downfall")
		.addMethodInvokeStatics(BlockWrapper.class, "getDefaultState", "isIn")
		.addMethodInvokeSpecific(BlockTagKey.class, "random", Block.class, RandomGenerator.class)
		.addMethod(BlockTagKey.TYPE, "random", randomFromWorld(loadRandom, BlockTagKey.class, Block.class))
		.addMethodInvokeStatics(BlockStateWrapper.class, "isIn", "getBlock", "isAir", "isReplaceable", "hasWater", "hasLava", "blocksLight", "hasCollision", "hasFullCubeCollision", "hasFullCubeOutline", "rotate", "mirror", "with")
		.addMethod(BlockStateWrapper.TYPE, "canPlaceAt", (parser, receiver, name, arguments) -> {
			InsnTree[] position = ScriptEnvironment.castArguments(parser, "canPlaceAt", types("III"), CastMode.IMPLICIT_NULL, arguments);
			return position == null ? null : new CastResult(invokeStatic(MethodInfo.getMethod(BlockStateWrapper.class, "canPlaceAt"), loadWorld, receiver, position[0], position[1], position[2]), position != arguments);
		})
		.addMethodInvoke(BiomeEntry.class, "isIn")
		.addMethodInvokeSpecific(BiomeTagKey.class, "random", BiomeEntry.class, RandomGenerator.class)
		.addMethod(BiomeTagKey.TYPE, "random", randomFromWorld(loadRandom, BiomeTagKey.class, BiomeEntry.class))
		.addMethodInvoke(ConfiguredFeatureEntry.class, "isIn")
		.addMethodInvokeSpecific(ConfiguredFeatureTagKey.class, "random", ConfiguredFeatureEntry.class, RandomGenerator.class)
		.addMethod(ConfiguredFeatureTagKey.TYPE, "random", randomFromWorld(loadRandom, ConfiguredFeatureTagKey.class, ConfiguredFeatureEntry.class))

		//casting

		.addCastConstant(BlockWrapper           .CONSTANT_FACTORY, "Block",                true)
		.addCastConstant(BlockStateWrapper      .CONSTANT_FACTORY, "BlockState",           true)
		.addCastConstant(BlockTagKey            .CONSTANT_FACTORY, "BlockTag",             true)
		.addCastConstant(BiomeEntry             .CONSTANT_FACTORY, "Biome",                true)
		.addCastConstant(BiomeTagKey            .CONSTANT_FACTORY, "BiomeTag",             true)
		.addCastConstant(ConfiguredFeatureEntry .CONSTANT_FACTORY, "ConfiguredFeature",    true)
		.addCastConstant(ConfiguredFeatureTagKey.CONSTANT_FACTORY, "ConfiguredFeatureTag", true)
		;
	}

	public static MethodHandler randomFromWorld(InsnTree loadRandom, Class<?> owner, Class<?> returnType) {
		MethodInfo randomFunction = MethodInfo.findMethod(owner, "random", returnType, RandomGenerator.class);
		return (parser, receiver, name, arguments) -> {
			if (arguments.length == 0) {
				return new CastResult(invokeVirtual(receiver, randomFunction, loadRandom), false);
			}
			return null;
		};
	}
}