package builderb0y.bigglobe.scripting.environments;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomSwitch;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.MemberKeywordHandler;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode.MemberKeywordFunction;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.special.IntervalSyntax;
import builderb0y.scripting.util.InfoHolder;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class RandomScriptEnvironment {

	public static final RandomGeneratorInfo RNG_INFO = new RandomGeneratorInfo();

	public static class RandomGeneratorInfo extends InfoHolder {

		@Disambiguate(name = "nextInt", returnType = int.class, paramTypes = {})
		public MethodInfo nextInt;
		@Disambiguate(name = "nextInt", returnType = int.class, paramTypes = { int.class })
		public MethodInfo nextIntBound;
		@Disambiguate(name = "nextInt", returnType = int.class, paramTypes = { int.class, int.class })
		public MethodInfo nextIntOriginBound;

		@Disambiguate(name = "nextLong", returnType = long.class, paramTypes = {})
		public MethodInfo nextLong;
		@Disambiguate(name = "nextLong", returnType = long.class, paramTypes = { long.class })
		public MethodInfo nextLongBound;
		@Disambiguate(name = "nextLong", returnType = long.class, paramTypes = { long.class, long.class })
		public MethodInfo nextLongOriginBound;

		@Disambiguate(name = "nextFloat", returnType = float.class, paramTypes = {})
		public MethodInfo nextFloat;
		@Disambiguate(name = "nextFloat", returnType = float.class, paramTypes = { float.class })
		public MethodInfo nextFloatBound;
		@Disambiguate(name = "nextFloat", returnType = float.class, paramTypes = { float.class, float.class })
		public MethodInfo nextFloatOriginBound;

		@Disambiguate(name = "nextDouble", returnType = double.class, paramTypes = {})
		public MethodInfo nextDouble;
		@Disambiguate(name = "nextDouble", returnType = double.class, paramTypes = { double.class })
		public MethodInfo nextDoubleBound;
		@Disambiguate(name = "nextDouble", returnType = double.class, paramTypes = { double.class, double.class })
		public MethodInfo nextDoubleOriginBound;

		@Disambiguate(name = "nextGaussian", returnType = double.class, paramTypes = {})
		public MethodInfo nextGaussian;
		@Disambiguate(name = "nextGaussian", returnType = double.class, paramTypes = { double.class, double.class })
		public MethodInfo nextGaussianMeanDev;
		public MethodInfo nextExponential;

		public MethodInfo nextBoolean;

		public RandomGeneratorInfo() {
			super(RandomGenerator.class);
		}
	}

	public static final PermuterInfo PERMUTER_INFO = new PermuterInfo();

	@SuppressWarnings("unused")
	public static class PermuterInfo extends InfoHolder {

		@Disambiguate(name = "new", returnType = void.class, paramTypes = { long.class })
		public MethodInfo constructor;

		@Disambiguate(name = "permute", returnType = long.class, paramTypes = { long.class, int.class })
		public MethodInfo permuteI;

		public MethodInfo nextUniformInt, toUniformInt, nextPositiveInt, toPositiveInt;
		@Disambiguate(name = "nextBoundedInt", returnType = int.class, paramTypes = { long.class, int.class })
		public MethodInfo nextIntBound;
		@Disambiguate(name = "nextBoundedInt", returnType = int.class, paramTypes = { long.class, int.class, int.class })
		public MethodInfo nextIntOriginBound;

		public MethodInfo nextUniformLong, toUniformLong, nextPositiveLong, toPositiveLong;
		@Disambiguate(name = "nextBoundedLong", returnType = long.class, paramTypes = { long.class, long.class })
		public MethodInfo nextLongBound;
		@Disambiguate(name = "nextBoundedLong", returnType = long.class, paramTypes = { long.class, long.class, long.class })
		public MethodInfo nextLongOriginBound;

		@Disambiguate(name = "nextUniformFloat", returnType = float.class, paramTypes = { long.class })
		public MethodInfo nextUniformFloat;
		public MethodInfo toUniformFloat, nextPositiveFloat, toPositiveFloat;
		@Disambiguate(name = "nextBoundedFloat", returnType = float.class, paramTypes = { long.class, float.class })
		public MethodInfo nextFloatBound;
		@Disambiguate(name = "nextBoundedFloat", returnType = float.class, paramTypes = { long.class, float.class, float.class })
		public MethodInfo nextFloatOriginBound;

		@Disambiguate(name = "nextUniformDouble", returnType = double.class, paramTypes = { long.class })
		public MethodInfo nextUniformDouble;
		public MethodInfo toUniformDouble, nextPositiveDouble, toPositiveDouble;
		@Disambiguate(name = "nextBoundedDouble", returnType = double.class, paramTypes = { long.class, double.class })
		public MethodInfo nextDoubleBound;
		@Disambiguate(name = "nextBoundedDouble", returnType = double.class, paramTypes = { long.class, double.class, double.class })
		public MethodInfo nextDoubleOriginBound;

		public MethodInfo nextBoolean, toBoolean;
		@Disambiguate(name = "nextChancedBoolean", returnType = boolean.class, paramTypes = { long.class, float.class })
		public MethodInfo nextChancedBooleanF;
		@Disambiguate(name = "nextChancedBoolean", returnType = boolean.class, paramTypes = { long.class, double.class })
		public MethodInfo nextChancedBooleanD;
		@Disambiguate(name = "toChancedBoolean", returnType = boolean.class, paramTypes = { long.class, float.class })
		public MethodInfo toChancedBooleanF;
		@Disambiguate(name = "toChancedBoolean", returnType = boolean.class, paramTypes = { long.class, double.class })
		public MethodInfo toChancedBooleanD;
		@Disambiguate(name = "nextChancedBoolean", returnType = boolean.class, paramTypes = { RandomGenerator.class, float.class })
		public MethodInfo rngNextChancedBooleanF;
		@Disambiguate(name = "nextChancedBoolean", returnType = boolean.class, paramTypes = { RandomGenerator.class, double.class })
		public MethodInfo rngNextChancedBooleanD;

		@Disambiguate(name = "roundRandomlyI", returnType = int.class, paramTypes = { long.class, float.class })
		public MethodInfo roundRandomlyIF;
		@Disambiguate(name = "roundRandomlyI", returnType = int.class, paramTypes = { long.class, double.class })
		public MethodInfo roundRandomlyID;
		@Disambiguate(name = "roundRandomlyL", returnType = long.class, paramTypes = { long.class, float.class })
		public MethodInfo roundRandomlyLF;
		@Disambiguate(name = "roundRandomlyL", returnType = long.class, paramTypes = { long.class, double.class })
		public MethodInfo roundRandomlyLD;

		@Disambiguate(name = "roundRandomlyI", returnType = int.class, paramTypes = { RandomGenerator.class, float.class })
		public MethodInfo rngRoundRandomlyIF;
		@Disambiguate(name = "roundRandomlyI", returnType = int.class, paramTypes = { RandomGenerator.class, double.class })
		public MethodInfo rngRoundRandomlyID;
		@Disambiguate(name = "roundRandomlyL", returnType = long.class, paramTypes = { RandomGenerator.class, float.class })
		public MethodInfo rngRoundRandomlyLF;
		@Disambiguate(name = "roundRandomlyL", returnType = long.class, paramTypes = { RandomGenerator.class, double.class })
		public MethodInfo rngRoundRandomlyLD;

		public PermuterInfo() {
			super(Permuter.class);
		}

		public InsnTree newInstance(InsnTree seed) {
			return InsnTrees.newInstance(this.constructor, seed);
		}

		public InsnTree permute(InsnTree seed, InsnTree intSalt) {
			return invokeStatic(this.permuteI, seed, intSalt);
		}

		public InsnTree nextUniformInt(InsnTree seed) {
			return invokeStatic(this.nextUniformInt, seed);
		}

		public InsnTree toUniformInt(InsnTree seed) {
			return invokeStatic(this.toUniformInt, seed);
		}

		public InsnTree nextPositiveInt(InsnTree seed) {
			return invokeStatic(this.nextPositiveInt, seed);
		}

		public InsnTree toPositiveInt(InsnTree seed) {
			return invokeStatic(this.toPositiveInt, seed);
		}

		public InsnTree nextUniformLong(InsnTree seed) {
			return invokeStatic(this.nextUniformLong, seed);
		}

		public InsnTree nextPositiveLong(InsnTree seed) {
			return invokeStatic(this.nextPositiveLong, seed);
		}

		public InsnTree nextUniformFloat(InsnTree seed) {
			return invokeStatic(this.nextUniformFloat, seed);
		}

		public InsnTree toUniformFloat(InsnTree seed) {
			return invokeStatic(this.toUniformFloat, seed);
		}

		public InsnTree nextPositiveFloat(InsnTree seed) {
			return invokeStatic(this.nextPositiveFloat, seed);
		}

		public InsnTree toPositiveFloat(InsnTree seed) {
			return invokeStatic(this.toPositiveFloat, seed);
		}

		public InsnTree nextUniformDouble(InsnTree seed) {
			return invokeStatic(this.nextUniformDouble, seed);
		}

		public InsnTree toUniformDouble(InsnTree seed) {
			return invokeStatic(this.toUniformDouble, seed);
		}

		public InsnTree nextPositiveDouble(InsnTree seed) {
			return invokeStatic(this.nextPositiveDouble, seed);
		}

		public InsnTree toPositiveDouble(InsnTree seed) {
			return invokeStatic(this.toPositiveDouble, seed);
		}

		public InsnTree nextBoolean(InsnTree seed) {
			return invokeStatic(this.nextBoolean, seed);
		}

		public InsnTree toBoolean(InsnTree seed) {
			return invokeStatic(this.toBoolean, seed);
		}

		public InsnTree nextChancedBooleanF(InsnTree seed, InsnTree chance) {
			return invokeStatic(this.nextChancedBooleanF, seed, chance);
		}

		public InsnTree nextChancedBooleanD(InsnTree seed, InsnTree chance) {
			return invokeStatic(this.nextChancedBooleanD, seed, chance);
		}

		public InsnTree toChancedBooleanF(InsnTree seed, InsnTree chance) {
			return invokeStatic(this.toChancedBooleanF, seed, chance);
		}

		public InsnTree toChancedBooleanD(InsnTree seed, InsnTree chance) {
			return invokeStatic(this.toChancedBooleanD, seed, chance);
		}

		public InsnTree roundRandomlyIF(InsnTree seed, InsnTree value) {
			return invokeStatic(this.roundRandomlyIF, seed, value);
		}

		public InsnTree roundRandomlyID(InsnTree seed, InsnTree value) {
			return invokeStatic(this.roundRandomlyID, seed, value);
		}

		public InsnTree roundRandomlyLF(InsnTree seed, InsnTree value) {
			return invokeStatic(this.roundRandomlyLF, seed, value);
		}

		public InsnTree roundRandomlyLD(InsnTree seed, InsnTree value) {
			return invokeStatic(this.roundRandomlyLD, seed, value);
		}
	}

	public static final MethodInfo ASSERT_FAIL = MethodInfo.findConstructor(AssertionError.class, String.class);

	public static CastResult createSeed(ExpressionParser parser, InsnTree... arguments) {
		InsnTree seed = arguments[0].cast(parser, TypeInfos.LONG, CastMode.IMPLICIT_THROW, false);
		boolean needCasting = seed != arguments[0];
		for (int index = 1, length = arguments.length; index < length; index++) {
			InsnTree next = arguments[index].cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW, false);
			needCasting |= next != arguments[index];
			seed = invokeStatic(PERMUTER_INFO.permuteI, seed, next);
		}
		return new CastResult(seed, needCasting);
	}

	public static InsnTree wrapRandomIf(ExpressionParser parser, InsnTree receiver, boolean negate, MemberKeywordMode mode) throws ScriptParsingException {
		return mode.apply(receiver, (InsnTree actualReceiver) -> randomIf(parser, actualReceiver, negate));
	}

	public static InsnTree randomIf(ExpressionParser parser, InsnTree receiver, boolean negate) throws ScriptParsingException {
		parser.beginCodeBlock();
		InsnTree conditionInsnTree, body;
		InsnTree firstPart = parser.nextScript();
		if (parser.input.hasOperatorAfterWhitespace(":")) { //random.if (a: b)
			Sort sort = firstPart.getTypeInfo().getSort();
			if (sort != Sort.FLOAT && sort != Sort.DOUBLE) {
				throw new ScriptParsingException("random." + (negate ? "unless" : "if") + "() chance should be float or double, but was " + firstPart.getTypeInfo(), parser.input);
			}
			body = parser.nextScript();
			conditionInsnTree = invokeStatic(
				sort == Sort.FLOAT ? PERMUTER_INFO.rngNextChancedBooleanF : PERMUTER_INFO.rngNextChancedBooleanD,
				receiver,
				firstPart
			);
		}
		else { //random.if (a)
			conditionInsnTree = invokeInstance(receiver, RNG_INFO.nextBoolean);
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

	public static MemberKeywordHandler randomSwitch() {
		return (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			parser.beginCodeBlock();
			Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
			MemberKeywordFunction selector;
			InsnTree first = parser.nextScript();
			if (parser.input.hasOperatorAfterWhitespace(",")) {
				cases.defaultReturnValue(
					throw_(
						newInstance(
							ASSERT_FAIL,
							ldc("Random returned value out of range")
						)
					)
				);
				cases.put(0, first);
				do cases.put(cases.size(), parser.nextScript());
				while (parser.input.hasOperatorAfterWhitespace(","));
				if (parser.endCodeBlock()) throw new ScriptParsingException("Can't declare variables *directly* inside a random switch.", parser.input);
				selector = (InsnTree actualReceiver) -> {
					InsnTree switchValue;
					if (actualReceiver.getTypeInfo().equals(type(RandomGenerator.class))) {
						switchValue = invokeInstance(
							actualReceiver,
							RNG_INFO.nextIntBound,
							ldc(cases.size())
						);
					}
					else if (actualReceiver.getTypeInfo().equals(TypeInfos.LONG)) {
						switchValue = invokeStatic(
							PERMUTER_INFO.nextIntBound,
							actualReceiver,
							ldc(cases.size())
						);
					}
					else {
						throw new InvalidOperandException("Expected receiver to be long or RandomGenerator, but it was " + actualReceiver);
					}
					return switch_(parser, switchValue, cases);
				};
			}
			else if (parser.input.hasOperatorAfterWhitespace(":")) {
				List<InsnTree> weights = new ArrayList<>();
				weights.add(first);
				cases.put(0, parser.nextScript());
				while (parser.input.hasOperatorAfterWhitespace(",")) {
					if (parser.input.hasIdentifierAfterWhitespace("default")) {
						if (cases.defaultReturnValue() != null) {
							throw new ScriptParsingException("Default already provided", parser.input);
						}
						parser.input.expectOperatorAfterWhitespace(":");
						cases.defaultReturnValue(parser.nextScript());
					}
					else {
						weights.add(parser.nextScript());
						parser.input.expectOperatorAfterWhitespace(":");
						cases.put(cases.size(), parser.nextScript());
					}
				}
				if (parser.endCodeBlock()) throw new ScriptParsingException("Can't declare variables *directly* inside a random switch.", parser.input);
				TypeInfo weightType = TypeInfos.widenUntilSameInt(weights.stream().map(InsnTree::getTypeInfo));
				weights.replaceAll((InsnTree tree) -> tree.cast(parser, weightType, CastMode.IMPLICIT_THROW, false));
				if (cases.defaultReturnValue() == null && weights.stream().map(InsnTree::getConstantValue).anyMatch((ConstantValue value) -> value.isConstant() && value.asDouble() > 0.0D)) {
					cases.defaultReturnValue(
						throw_(
							newInstance(
								ASSERT_FAIL,
								ldc("Random returned value out of range")
							)
						)
					);
				}
				MethodInfo runtimeMethod = new MethodInfo(
					ACC_PUBLIC,
					TypeInfos.OBJECT /* not used */,
					"randomSwitch",
					TypeInfos.INT,
					Stream.concat(
						Stream.of(receiver.getTypeInfo()),
						weights
						.stream()
						.filter((InsnTree tree) -> !tree.getConstantValue().isConstantOrDynamic())
						.map(InsnTree::getTypeInfo)
					)
					.toArray(TypeInfo.ARRAY_FACTORY)
				);
				ConstantValue[] constantArgs = (
					Stream.concat(
						Stream.of(constant(weightType)),
						weights
						.stream()
						.map((InsnTree tree) -> tree.getConstantValue().isConstantOrDynamic() ? tree.getConstantValue() : constant(0, tree.getTypeInfo()))
					)
					.toArray(ConstantValue.ARRAY_FACTORY)
				);
				InsnTree[] runtimeArgs = weights.stream().filter((InsnTree tree) -> !tree.getConstantValue().isConstantOrDynamic()).toArray(InsnTree.ARRAY_FACTORY);
				selector = (InsnTree actualReceiver) -> switch_(
					parser,
					invokeDynamic(
						RandomSwitch.BOOTSTRAP_METHOD,
						runtimeMethod,
						constantArgs,
						ObjectArrays.concat(actualReceiver, runtimeArgs)
					),
					cases
				);
			}
			else {
				throw new ScriptParsingException("Expected ',' or ':'", parser.input);
			}
			return mode.apply(receiver, selector);
		};
	}

	public static MemberKeywordHandler nextBetween() {
		return (ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
			int flags;
			if (receiver.getTypeInfo().getSort() == Sort.LONG) {
				flags = Permuter.BetweenInfo.FLAG_SEED_RECEIVER;
			}
			else if (receiver.getTypeInfo().extendsOrImplements(type(RandomGenerator.class))) {
				flags = Permuter.BetweenInfo.FLAG_RANDOM_RECEIVER;
			}
			else {
				throw new ScriptParsingException("Expected receiver to be of type long or Random, got " + receiver.getTypeInfo(), parser.input);
			}
			IntervalSyntax interval = IntervalSyntax.parse(parser);
			TypeInfo desiredType = TypeInfos.widenUntilSameInt(interval.min().getTypeInfo(), interval.max().getTypeInfo());
			InsnTree min = interval.min().cast(parser, desiredType, CastMode.IMPLICIT_THROW, false);
			InsnTree max = interval.max().cast(parser, desiredType, CastMode.IMPLICIT_THROW, false);
			if (interval.minInclusive()) flags |= Permuter.BetweenInfo.FLAG_MIN_INCLUSIVE;
			if (interval.maxInclusive()) flags |= Permuter.BetweenInfo.FLAG_MAX_INCLUSIVE;
			switch (desiredType.getSort()) {
				case INT -> flags |= Permuter.BetweenInfo.FLAG_INT_DESIRED;
				case LONG -> flags |= Permuter.BetweenInfo.FLAG_LONG_DESIRED;
				case FLOAT -> flags |= Permuter.BetweenInfo.FLAG_FLOAT_DESIRED;
				case DOUBLE -> flags |= Permuter.BetweenInfo.FLAG_DOUBLE_DESIRED;
			}
			int flags_ = flags;
			return mode.apply(receiver, (InsnTree actualReceiver) -> invokeStatic(Permuter.BETWEEN_INFO.getMethodFor(flags_), actualReceiver, min, max));
		};
	}
}