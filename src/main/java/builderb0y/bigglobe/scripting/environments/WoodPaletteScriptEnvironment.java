package builderb0y.bigglobe.scripting.environments;

import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.common.Case;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.scripting.wrappers.BlockStateWrapper;
import builderb0y.bigglobe.scripting.wrappers.entries.WoodPaletteEntry;
import builderb0y.bigglobe.scripting.wrappers.tags.WoodPaletteTag;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MemberKeywordHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.special.NamedValues.NamedValue;
import builderb0y.scripting.parsing.special.PrefixedNamedValues;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WoodPaletteScriptEnvironment {

	public static final MutableScriptEnvironment BASE = (
		new MutableScriptEnvironment()
		.addType("WoodPalette", WoodPaletteEntry.class)
		.addType("WoodPaletteTag", WoodPaletteTag.class)
		.addCastConstant(WoodPaletteEntry.CONSTANT_FACTORY, true)
		.configure(WoodPaletteTag.PARSER)
		.addMethodInvokeSpecific(WoodPaletteTag.class, "random", WoodPaletteEntry.class, RandomGenerator.class)
		.addMethodInvokeSpecific(WoodPaletteTag.class, "random", WoodPaletteEntry.class, long.class)
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
							WoodPaletteEntry.INFO.getBlocks,
							getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()))
						);
					}
				)
			);
		}
	}

	public static Consumer<MutableScriptEnvironment> create(@Nullable InsnTree loadRandom) {
		return (MutableScriptEnvironment environment) -> {
			environment.addAll(BASE);
			for (WoodPaletteType type : WoodPaletteType.VALUES) {
				String baseName = Case.CAMEL_CASE.apply(type.lowerCaseName);
				InsnTree loadType = getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()));
				if (loadRandom != null) {
					environment.addField(
						type(WoodPaletteEntry.class),
						baseName + "Block",
						new FieldHandler.Named(
							"palette." + baseName + "Block",
							(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
								return mode.makeInvoker(
									parser,
									receiver,
									WoodPaletteEntry.INFO.getRandomBlock,
									loadRandom,
									loadType
								);
							}
						)
					);
				}
				environment.addMethod(
					type(WoodPaletteEntry.class),
					baseName + "Block",
					new MethodHandler.Named(
						"palette." + baseName + "Block(optional Random random or long seed)",
						(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
							return switch (arguments.length) {
								case 0 -> {
									if (loadRandom != null) {
										yield new CastResult(
											mode.makeInvoker(
												parser,
												receiver,
												WoodPaletteEntry.INFO.getRandomBlock,
												loadRandom,
												loadType
											),
											false
										);
									}
									else {
										throw new ScriptParsingException("Implicit random is not available. Specify your own random or seed.", parser.input);
									}
								}
								case 1 -> {
									if (arguments[0].getTypeInfo().equals(TypeInfos.LONG)) {
										yield new CastResult(mode.makeInvoker(parser, receiver, WoodPaletteEntry.INFO.getSeededBlock, arguments[0], loadType), false);
									}
									else if (arguments[0].getTypeInfo().extendsOrImplements(type(RandomGenerator.class))) {
										yield new CastResult(mode.makeInvoker(parser, receiver, WoodPaletteEntry.INFO.getRandomBlock, arguments[0], loadType), false);
									}
									else {
										throw new ScriptParsingException("Expected long or Random, got " + arguments[0].getTypeInfo(), parser.input);
									}
								}
								default -> {
									throw new ScriptParsingException("Expected 0 or 1 arguments, got " + arguments.length, parser.input);
								}
							};
						}
					)
				);
				environment.addMemberKeyword(
					type(WoodPaletteEntry.class),
					baseName + "State",
					new MemberKeywordHandler.Named(
						"palette." + baseName + "State(optional Random random or long seed, property1: value1, property2: value2, ...)",
						(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
							return mode.apply(receiver, (InsnTree actualReceiver) -> {
								PrefixedNamedValues namedValues = PrefixedNamedValues.parse(parser, null, TypeInfos.COMPARABLE, null);
								InsnTree loadRandomOrSeed = namedValues.prefix();
								if (loadRandomOrSeed == null) {
									loadRandomOrSeed = loadRandom;
									if (loadRandomOrSeed == null) {
										throw new ScriptParsingException("Implicit random is not available. Specify your own random or seed.", parser.input);
									}
								}
								InsnTree tree;
								if (loadRandomOrSeed.getTypeInfo().equals(TypeInfos.LONG)) {
									tree = invokeInstance(actualReceiver, WoodPaletteEntry.INFO.getSeededState, loadRandomOrSeed, loadType);
								}
								else if (loadRandomOrSeed.getTypeInfo().extendsOrImplements(type(RandomGenerator.class))) {
									tree = invokeInstance(actualReceiver, WoodPaletteEntry.INFO.getRandomState, loadRandomOrSeed, loadType);
								}
								else {
									throw new ScriptParsingException("Expected long or Random, got " + loadRandomOrSeed.getTypeInfo(), parser.input);
								}
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