package builderb0y.bigglobe.scripting;

import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.scripting.wrappers.BlockStateWrapper;
import builderb0y.bigglobe.scripting.wrappers.WoodPaletteEntry;
import builderb0y.bigglobe.scripting.wrappers.WoodPaletteTagKey;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WoodPaletteScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
		.addType("WoodPalette", WoodPaletteEntry.class)
		.addType("WoodPaletteTag", WoodPaletteTagKey.class)
		.addCastConstant(WoodPaletteEntry.CONSTANT_FACTORY, "WoodPalette", true)
		.addCastConstant(WoodPaletteTagKey.CONSTANT_FACTORY, "WoodPaletteTag", true)
	);

	static {
		for (WoodPaletteType type : WoodPaletteType.VALUES) {
			INSTANCE.addField(type(WoodPaletteEntry.class), type.lowerCaseName, (parser, receiver, name) -> {
				return invokeVirtual(
					receiver,
					MethodInfo.getMethod(WoodPaletteEntry.class, "getBlock"),
					getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()))
				);
			});
			INSTANCE.addMemberKeyword(type(WoodPaletteEntry.class), type.lowerCaseName, (parser, receiver, name) -> {
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
	}
}