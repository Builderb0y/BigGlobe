package builderb0y.bigglobe.scripting;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.common.Case;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.scripting.wrappers.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.BlockStateWrapper;
import builderb0y.bigglobe.scripting.wrappers.WoodPaletteEntry;
import builderb0y.bigglobe.scripting.wrappers.WoodPaletteTagKey;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WoodPaletteScriptEnvironment {

	public static MutableScriptEnvironment create(InsnTree loadRandom) {
		MutableScriptEnvironment environment = (
			new MutableScriptEnvironment()
			.addType("WoodPalette", WoodPaletteEntry.class)
			.addType("WoodPaletteTag", WoodPaletteTagKey.class)
			.addCastConstant(WoodPaletteEntry.CONSTANT_FACTORY, true)
			.addCastConstant(WoodPaletteTagKey.CONSTANT_FACTORY, true)
			.addMethodInvokeSpecific(WoodPaletteTagKey.class, "random", WoodPaletteEntry.class, RandomGenerator.class)
			.addMethodInvokeSpecific(WoodPaletteTagKey.class, "random", WoodPaletteEntry.class, long.class)
			.addQualifiedFunctionInvokeStatics(WoodPaletteEntry.class, "randomForBiome", "allForBiome")
			.addQualifiedFunction(type(WoodPaletteEntry.class), "randomForBiome", new FunctionHandler.Named("randomForBiome(Biome)", (parser, name, arguments) -> {
				InsnTree biome = ScriptEnvironment.castArgument(parser, "randomForBiome", BiomeEntry.TYPE, CastMode.IMPLICIT_NULL, arguments);
				if (biome == null) return null;
				return new CastResult(
					invokeStatic(
						MethodInfo.getMethod(WoodPaletteEntry.class, "randomForBiome"),
						biome,
						loadRandom
					),
					biome != arguments[0]
				);
			}))
		);
		for (WoodPaletteType type : WoodPaletteType.VALUES) {
			String baseName = Case.CAMEL_CASE.apply(type.lowerCaseName);
			environment.addField(type(WoodPaletteEntry.class), baseName + "Blocks", (parser, receiver, name, mode) -> {
				return mode.makeInstanceGetter(
					parser,
					receiver,
					MethodInfo.getMethod(WoodPaletteEntry.class, "getBlocks"),
					getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()))
				);
			});
			environment.addField(type(WoodPaletteEntry.class), baseName + "Block", (parser, receiver, name, mode) -> {
				return mode.makeInstanceGetter(
					parser,
					receiver,
					MethodInfo.getMethod(WoodPaletteEntry.class, "getBlock"),
					loadRandom,
					getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()))
				);
			});
			environment.addMemberKeyword(type(WoodPaletteEntry.class), baseName + "State", (parser, receiver, name) -> {
				parser.beginCodeBlock();
				InsnTree tree = invokeInstance(
					receiver,
					MethodInfo.getMethod(WoodPaletteEntry.class, "getState"),
					loadRandom,
					getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()))
				);
				if (parser.input.peekAfterWhitespace() != ')') {
					do {
						String property = parser.input.expectIdentifierAfterWhitespace();
						parser.input.expectOperatorAfterWhitespace(":");
						InsnTree value = parser.nextScript().cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW);
						tree = invokeStatic(BlockStateWrapper.WITH, tree, ldc(property), value);
					}
					while (parser.input.hasOperatorAfterWhitespace(","));
				}
				parser.endCodeBlock();
				return tree;
			});
		}
		return environment;
	}
}