package builderb0y.bigglobe.scripting.environments;

import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.InsnTrees;
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
import builderb0y.scripting.util.InfoHolder;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class RandomScriptEnvironment {

	public static final RandomGeneratorInfo RNG_INFO = new RandomGeneratorInfo();
	public static class RandomGeneratorInfo extends InfoHolder {

		public MethodInfo nextBoolean;
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
		@Disambiguate(name = "nextGaussian", returnType = double.class, paramTypes = {})
		public MethodInfo nextGaussian;

		@Disambiguate(name = "nextGaussian", returnType = double.class, paramTypes = { double.class, double.class })
		public MethodInfo nextGaussianMeanDev;
		public MethodInfo nextExponential;

		public RandomGeneratorInfo() {
			super(RandomGenerator.class);
		}
	}

	public static final PermuterInfo PERMUTER_INFO = new PermuterInfo();
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

		@Disambiguate(name = "nextPositiveFloat", returnType = float.class, paramTypes = { long.class })
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


	public static MutableScriptEnvironment create(InsnTree loader) {
		return (
			new MutableScriptEnvironment()
			.addType("Random", RandomGenerator.class)
			.addVariable("random", loader)
			.addQualifiedFunction(type(RandomGenerator.class), "new", new FunctionHandler.Named("Random.new(long [, int...])", (parser, name, arguments) -> {
				if (arguments.length == 0) return null;
				CastResult seed = createSeed(parser, arguments);
				return new CastResult(newInstance(PERMUTER_INFO.constructor, seed.tree()), seed.requiredCasting());
			}))
			.addMethodInvoke("nextInt", RNG_INFO.nextInt)
			.addMethodInvoke("nextInt", RNG_INFO.nextIntBound)
			.addMethodInvoke("nextInt", RNG_INFO.nextIntOriginBound)
			.addMethodInvoke("nextLong", RNG_INFO.nextLong)
			.addMethodInvoke("nextLong", RNG_INFO.nextLongBound)
			.addMethodInvoke("nextLong", RNG_INFO.nextLongOriginBound)
			.addMethodInvoke("nextFloat", RNG_INFO.nextFloat)
			.addMethodInvoke("nextFloat", RNG_INFO.nextFloatBound)
			.addMethodInvoke("nextFloat", RNG_INFO.nextFloatOriginBound)
			.addMethodInvoke("nextDouble", RNG_INFO.nextDouble)
			.addMethodInvoke("nextDouble", RNG_INFO.nextDoubleBound)
			.addMethodInvoke("nextDouble", RNG_INFO.nextDoubleOriginBound)
			.addMethodInvoke("nextBoolean", RNG_INFO.nextBoolean)
			.addMethodInvokeStatic("nextBoolean", PERMUTER_INFO.rngNextChancedBooleanF)
			.addMethodInvokeStatic("nextBoolean", PERMUTER_INFO.rngNextChancedBooleanD)
			.addMethodInvoke("nextGaussian", RNG_INFO.nextGaussian)
			.addMethodInvoke("nextGaussian", RNG_INFO.nextGaussianMeanDev)
			.addMethodInvoke("nextExponential", RNG_INFO.nextExponential)
			.addMethodInvoke("roundInt", PERMUTER_INFO.rngRoundRandomlyIF)
			.addMethodInvoke("roundInt", PERMUTER_INFO.rngRoundRandomlyID)
			.addMethodInvoke("roundLong", PERMUTER_INFO.rngRoundRandomlyLF)
			.addMethodInvoke("roundLong", PERMUTER_INFO.rngRoundRandomlyLD)
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
					.apply(loader, (InsnTree actualReceiver) -> {
						return switch_(
							parser,
							invokeInstance(
								actualReceiver,
								RNG_INFO.nextIntBound,
								ldc(arguments.length)
							),
							cases
						);
					}),
					false
				);
			}))
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
			seed = invokeStatic(PERMUTER_INFO.permuteI, seed, next);
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
				sort == Sort.FLOAT ? PERMUTER_INFO.rngNextChancedBooleanF : PERMUTER_INFO.rngNextChancedBooleanD,
				receiver,
				firstPart
			);
		}
		else { //random.if(a)
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
}