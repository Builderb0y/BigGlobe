package builderb0y.bigglobe.scripting;

import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class RandomScriptEnvironment {

	public static final MethodInfo
		CONSTRUCTOR            = MethodInfo.findConstructor(Permuter       .class,                       long   .class                                     ),
		PERMUTE_INT            = MethodInfo.findMethod     (Permuter       .class, "permute",            long   .class,            long.class, int   .class).pure(),
		NEXT_INT_1             = MethodInfo.findMethod     (RandomGenerator.class, "nextInt",            int    .class,            int .class              ),
		NEXT_BOOLEAN           = MethodInfo.findMethod     (RandomGenerator.class, "nextBoolean",        boolean.class                                     ),
		NEXT_CHANCED_BOOLEAN_F = MethodInfo.findMethod     (Permuter       .class, "nextChancedBoolean", boolean.class, RandomGenerator.class, float .class),
		NEXT_CHANCED_BOOLEAN_D = MethodInfo.findMethod     (Permuter       .class, "nextChancedBoolean", boolean.class, RandomGenerator.class, double.class),
		ASSERT_FAIL            = MethodInfo.findConstructor(AssertionError .class,                       String .class                                     );

	public static MutableScriptEnvironment create(InsnTree loader) {
		return (
			new MutableScriptEnvironment()
			.addType("Random", RandomGenerator.class)
			.addVariable("random", loader)
			.addQualifiedFunction(type(RandomGenerator.class), "new", new FunctionHandler.Named("Random.new(long [, int...])", (parser, name, arguments) -> {
				if (arguments.length == 0) return null;
				CastResult seed = createSeed(parser, arguments);
				return new CastResult(newInstance(CONSTRUCTOR, seed.tree()), seed.requiredCasting());
			}))
			.addMethodInvoke(RandomGenerator.class, "nextBoolean")
			.addMethodRenamedInvokeStaticSpecific("nextBoolean", Permuter.class, "nextChancedBoolean", boolean.class, RandomGenerator.class, float.class)
			.addMethodRenamedInvokeStaticSpecific("nextBoolean", Permuter.class, "nextChancedBoolean", boolean.class, RandomGenerator.class, double.class)
			.addMethodMultiInvokes(RandomGenerator.class, "nextInt", "nextLong", "nextFloat", "nextDouble", "nextGaussian", "nextExponential")
			.addMethod(type(RandomGenerator.class), "switch", new MethodHandler.Named("random.switch(cases) ;nullable random not yet supported", (parser, receiver, name, mode, arguments) -> {
				if (arguments.length < 2) {
					throw new ScriptParsingException("switch() requires at least 2 arguments", parser.input);
				}
				Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
				for (int index = 0, length = arguments.length; index < length; index++) {
					cases.put(index, arguments[index]);
				}
				cases.defaultReturnValue(
					throw_(
						newInstance(
							ASSERT_FAIL,
							ldc("Random returned value out of range")
						)
					)
				);
				return new CastResult(
					(
						switch (mode) {
							case NORMAL -> MemberKeywordMode.NORMAL;
							case NULLABLE -> MemberKeywordMode.NULLABLE;
							case RECEIVER -> MemberKeywordMode.RECEIVER;
							case NULLABLE_RECEIVER -> MemberKeywordMode.NULLABLE_RECEIVER;
						}
					)
					.apply(loader, actualReceiver -> {
						return switch_(
							parser,
							invokeInstance(
								actualReceiver,
								NEXT_INT_1,
								ldc(arguments.length)
							),
							cases
						);
					}),
					false
				);
			}))
			.addMethodRenamedInvokeStaticSpecific("roundInt", Permuter.class, "roundRandomlyI", int.class, RandomGenerator.class, float.class)
			.addMethodRenamedInvokeStaticSpecific("roundInt", Permuter.class, "roundRandomlyI", int.class, RandomGenerator.class, double.class)
			.addMethodRenamedInvokeStaticSpecific("roundLong", Permuter.class, "roundRandomlyL", long.class, RandomGenerator.class, float.class)
			.addMethodRenamedInvokeStaticSpecific("roundLong", Permuter.class, "roundRandomlyL", long.class, RandomGenerator.class, double.class)
			.addMemberKeyword(type(RandomGenerator.class), "if", (parser, receiver, name, mode) -> {
				return wrapRandomIf(parser, receiver, false, mode);
			})
			.addMemberKeyword(type(RandomGenerator.class), "unless", (parser, receiver, name, mode) -> {
				return wrapRandomIf(parser, receiver, true, mode);
			})
		);
	}

	public static CastResult createSeed(ExpressionParser parser, InsnTree... arguments) {
		InsnTree seed = arguments[0].cast(parser, TypeInfos.LONG, CastMode.IMPLICIT_THROW);
		boolean needCasting = seed != arguments[0];
		for (int index = 1, length = arguments.length; index < length; index++) {
			InsnTree next = arguments[index].cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW);
			needCasting |= next != arguments[index];
			seed = invokeStatic(PERMUTE_INT, seed, next);
		}
		return new CastResult(seed, needCasting);
	}

	public static InsnTree wrapRandomIf(ExpressionParser parser, InsnTree receiver, boolean negate, MemberKeywordMode mode) throws ScriptParsingException {
		return mode.apply(receiver, actualReceiver -> randomIf(parser, actualReceiver, negate));
	}

	public static InsnTree randomIf(ExpressionParser parser, InsnTree receiver, boolean negate) throws ScriptParsingException {
		parser.beginCodeBlock();
		InsnTree conditionInsnTree, body;
		InsnTree firstPart = parser.nextScript();
		if (parser.input.hasOperatorAfterWhitespace(":")) { //random.if(a: b)
			Sort sort = firstPart.getTypeInfo().getSort();
			if (sort != Sort.FLOAT && sort != Sort.DOUBLE) {
				throw new ScriptParsingException("random." + (negate ? "unless" : "if") + "() chance should be float or double, but was " + firstPart.getTypeInfo(), parser.input);
			}
			body = parser.nextScript();
			conditionInsnTree = invokeStatic(
				sort == Sort.FLOAT ? NEXT_CHANCED_BOOLEAN_F : NEXT_CHANCED_BOOLEAN_D,
				receiver,
				firstPart
			);
		}
		else { //random.if(a)
			conditionInsnTree = invokeInstance(receiver, NEXT_BOOLEAN);
			body = firstPart;
		}
		parser.endCodeBlock();
		ConditionTree conditionTree = condition(parser, conditionInsnTree);
		if (negate) conditionTree = not(conditionTree);

		if (parser.input.hasIdentifierAfterWhitespace("else")) {
			return ifElse(parser, conditionTree, body, BuiltinScriptEnvironment.tryParenthesized(parser));
		}
		else {
			return ifThen(conditionTree, body);
		}
	}
}