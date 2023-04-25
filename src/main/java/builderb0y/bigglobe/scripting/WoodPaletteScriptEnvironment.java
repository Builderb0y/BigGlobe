package builderb0y.bigglobe.scripting;

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
			.addQualifiedFunctionInvokeStatics(WoodPaletteEntry.class, "randomForBiome", "allForBiome")
		);
		environment.addQualifiedFunction(type(WoodPaletteEntry.class), "randomForBiome", new FunctionHandler.Named("randomForBiome(Biome)", (parser, name, arguments) -> {
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
		}));
		for (WoodPaletteType type : WoodPaletteType.VALUES) {
			environment.addField(type(WoodPaletteEntry.class), type.lowerCaseName, (parser, receiver, name) -> {
				return invokeVirtual(
					receiver,
					MethodInfo.getMethod(WoodPaletteEntry.class, "getBlock"),
					getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()))
				);
			});
			environment.addMemberKeyword(type(WoodPaletteEntry.class), type.lowerCaseName, (parser, receiver, name) -> {
				parser.beginCodeBlock();
				InsnTree tree = invokeVirtual(
					receiver,
					MethodInfo.getMethod(WoodPaletteEntry.class, "getState"),
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