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
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class RandomScriptEnvironment {

	public static final MethodInfo
		CONSTRUCTOR = MethodInfo.findConstructor(Permuter.class, long.class),
		PERMUTE_INT = MethodInfo.findMethod(Permuter.class, "permute", long.class, long.class, int.class).pure(),
		NEXT_INT_1  = MethodInfo.findMethod(RandomGenerator.class, "nextInt", int.class, int.class),
		NEXT_BOOLEAN = MethodInfo.findMethod(RandomGenerator.class, "nextBoolean", boolean.class),
		NEXT_CHANCED_BOOLEAN_F = MethodInfo.findMethod(Permuter.class, "nextChancedBoolean", boolean.class, RandomGenerator.class, float.class),
		NEXT_CHANCED_BOOLEAN_D = MethodInfo.findMethod(Permuter.class, "nextChancedBoolean", boolean.class, RandomGenerator.class, double.class);

	public static MutableScriptEnvironment create(InsnTree loader) {
		return (
			new MutableScriptEnvironment()
			.addType("Random", RandomGenerator.class)
			.addVariable("random", loader)
			.addQualifiedFunction(type(RandomGenerator.class), "new", (parser, name, arguments) -> {
				if (arguments.length == 0) throw new ScriptParsingException("Random.new() requires a seed.", parser.input);
				InsnTree seed = arguments[0].cast(parser, TypeInfos.LONG, CastMode.IMPLICIT_THROW);
				boolean needCasting = seed != arguments[0];
				for (int index = 1, length = arguments.length; index < length; index++) {
					InsnTree next = arguments[index].cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW);
					needCasting |= next != arguments[index];
					seed = invokeStatic(PERMUTE_INT, seed, next);
				}
				return new CastResult(newInstance(CONSTRUCTOR, seed), needCasting);
			})
			.addMethodInvoke(RandomGenerator.class, "nextBoolean")
			.addMethodRenamedInvokeStaticSpecific("nextBoolean", Permuter.class, "nextChancedBoolean", boolean.class, RandomGenerator.class, float.class)
			.addMethodRenamedInvokeStaticSpecific("nextBoolean", Permuter.class, "nextChancedBoolean", boolean.class, RandomGenerator.class, double.class)
			.addMethodMultiInvokes(RandomGenerator.class, "nextInt", "nextLong", "nextFloat", "nextDouble", "nextGaussian", "nextExponential")
			.addMethod(type(RandomGenerator.class), "switch", (parser, receiver, name, arguments) -> {
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
							constructor(
								ACC_PUBLIC,
								AssertionError.class,
								String.class
							),
							ldc("Random returned value out of range")
						)
					)
				);
				return new CastResult(
					switch_(
						parser,
						invokeInterface(
							loader,
							NEXT_INT_1,
							ldc(arguments.length)
						),
						cases
					),
					false
				);
			})
			.addMethodRenamedInvokeStaticSpecific("roundInt", Permuter.class, "roundRandomlyI", int.class, RandomGenerator.class, float.class)
			.addMethodRenamedInvokeStaticSpecific("roundInt", Permuter.class, "roundRandomlyI", int.class, RandomGenerator.class, double.class)
			.addMethodRenamedInvokeStaticSpecific("roundLong", Permuter.class, "roundRandomlyL", long.class, RandomGenerator.class, float.class)
			.addMethodRenamedInvokeStaticSpecific("roundLong", Permuter.class, "roundRandomlyL", long.class, RandomGenerator.class, double.class)
			.addMemberKeyword(type(RandomGenerator.class), "if", (parser, receiver, name) -> {
				return randomIf(parser, receiver, false);
			})
			.addMemberKeyword(type(RandomGenerator.class), "unless", (parser, receiver, name) -> {
				return randomIf(parser, receiver, true);
			})
		);
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
			conditionInsnTree = invokeInterface(receiver, NEXT_BOOLEAN);
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