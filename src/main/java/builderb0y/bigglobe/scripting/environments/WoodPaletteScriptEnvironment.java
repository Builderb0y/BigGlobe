package builderb0y.bigglobe.scripting.environments;

import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import builderb0y.autocodec.common.Case;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.scripting.wrappers.BlockStateWrapper;
import builderb0y.bigglobe.scripting.wrappers.WoodPaletteEntry;
import builderb0y.bigglobe.scripting.wrappers.WoodPaletteTagKey;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MemberKeywordHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.special.NamedValues;
import builderb0y.scripting.parsing.special.NamedValues.NamedValue;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WoodPaletteScriptEnvironment {

	public static final MutableScriptEnvironment BASE = (
		new MutableScriptEnvironment()
		.addType("WoodPalette", WoodPaletteEntry.class)
		.addType("WoodPaletteTag", WoodPaletteTagKey.class)
		.addCastConstant(WoodPaletteEntry.CONSTANT_FACTORY, true)
		.addCastConstant(WoodPaletteTagKey.CONSTANT_FACTORY, true)
		.addMethodInvokeSpecific(WoodPaletteTagKey.class, "random", WoodPaletteEntry.class, RandomGenerator.class)
		.addMethodInvokeSpecific(WoodPaletteTagKey.class, "random", WoodPaletteEntry.class, long.class)
		.addFieldInvoke(WoodPaletteEntry.class, "features")
	);

	static {
		for (WoodPaletteType type : WoodPaletteType.VALUES) {
			String baseName = Case.CAMEL_CASE.apply(type.lowerCaseName);
			BASE.addField(
				type(WoodPaletteEntry.class),
				baseName + "Blocks",
				new FieldHandler.Named(
					"palette." + baseName + "Blocks",
					(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
						return mode.makeInvoker(
							parser,
							receiver,
							MethodInfo.getMethod(WoodPaletteEntry.class, "getBlocks"),
							getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()))
						);
					}
				)
			);
		}
	}

	public static Consumer<MutableScriptEnvironment> create(InsnTree loadRandom) {
		return (MutableScriptEnvironment environment) -> {
			environment.addAll(BASE);
			for (WoodPaletteType type : WoodPaletteType.VALUES) {
				String baseName = Case.CAMEL_CASE.apply(type.lowerCaseName);
				environment.addField(
					type(WoodPaletteEntry.class),
					baseName + "Block",
					new FieldHandler.Named(
						"palette." + baseName + "Block",
						(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
							return mode.makeInvoker(
								parser,
								receiver,
								MethodInfo.getMethod(WoodPaletteEntry.class, "getBlock"),
								loadRandom,
								getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()))
							);
						}
					)
				);
				environment.addMemberKeyword(
					type(WoodPaletteEntry.class),
					baseName + "State",
					new MemberKeywordHandler.Named(
						"palette." + baseName + "State(property1: value1, property2: value2, ...)",
						(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
							return mode.apply(receiver, (InsnTree actualReceiver) -> {
								InsnTree tree = invokeInstance(
									actualReceiver,
									MethodInfo.getMethod(WoodPaletteEntry.class, "getState"),
									loadRandom,
									getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()))
								);
								NamedValues namedValues = NamedValues.parse(parser, TypeInfos.COMPARABLE, null);
								for (NamedValue value : namedValues.values()) {
									tree = invokeStatic(BlockStateWrapper.WITH, tree, ldc(value.name()), value.value());
								}
								return namedValues.maybeWrap(tree);
							});
						}
					)
				);
			}
		};
	}
}