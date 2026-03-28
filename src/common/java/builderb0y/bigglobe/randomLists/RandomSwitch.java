package builderb0y.bigglobe.randomLists;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.objectweb.asm.Type;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.DoubleCompareConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.FloatCompareConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareZeroConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.LongCompareConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.AddInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree.CombinedMode;
import builderb0y.scripting.bytecode.tree.instructions.update.VariableUpdaterInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.VariableUpdaterInsnTree.VariableUpdaterEmitters;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class RandomSwitch {

	public static final String BASE_NAME = Type.getInternalName(RandomSwitch.class);
	public static final MethodInfo BOOTSTRAP_METHOD = MethodInfo.inCaller("randomSwitch");

	public static CallSite randomSwitch(MethodHandles.Lookup lookup, String name, MethodType type, Object... constantWeights) throws ReflectiveOperationException {
		//validate args.
		if (type.returnType() != int.class) {
			throw new IllegalArgumentException(type + " returns non-int");
		}
		boolean useSeed;
		LazyVarInfo rngSource;
		if (type.parameterType(0) == RandomGenerator.class) {
			rngSource = new LazyVarInfo("random", type(RandomGenerator.class));
			useSeed = false;
		}
		else if (type.parameterType(0) == long.class) {
			rngSource = new LazyVarInfo("seed", TypeInfos.LONG);
			useSeed = true;
		}
		else {
			throw new IllegalArgumentException("Expected first parameter to be a RandomGenerator or long, but it was " + type.parameterType(0));
		}
		SupportedType supportedType = SupportedType.forClass((Class<?>)(constantWeights[0]));
		List<LazyVarInfo> methodParameters = new ArrayList<>(type.parameterCount());
		methodParameters.add(rngSource);
		List<InsnTree> weightLoaders = new ArrayList<>(constantWeights.length);
		Object totalConstantWeight = switch (supportedType) {
			case INT -> new int[1];
			case LONG -> new long[1];
			case FLOAT -> new float[1];
			case DOUBLE -> new double[1];
		};
		int expectedParameterCount = 1;
		for (int index = 1, length = constantWeights.length; index < length; index++) {
			Object weight = constantWeights[index];
			if (switch (supportedType) {
				case INT -> ((Integer)(weight)).intValue() > 0;
				case LONG -> ((Long)(weight)).longValue() > 0L;
				case FLOAT -> ((Float)(weight)).floatValue() > 0.0F;
				case DOUBLE -> ((Double)(weight)).doubleValue() > 0.0D;
			}) {
				if (switch (supportedType) {
					case INT -> (((int[])(totalConstantWeight))[0] += ((int)(weight))) <= 0;
					case LONG -> (((long[])(totalConstantWeight))[0] += ((long)(weight))) <= 0L;
					case FLOAT -> !((((float[])(totalConstantWeight))[0] += ((float)(weight))) < Float.POSITIVE_INFINITY);
					case DOUBLE -> !((((double[])(totalConstantWeight))[0] += ((double)(weight))) < Double.POSITIVE_INFINITY);
				}) {
					throw new ArithmeticException("Weight overflowed");
				}
				weightLoaders.add(ldc(weight, supportedType.typeInfo));
			}
			else {
				LazyVarInfo varInfo = new LazyVarInfo("weight" + index, supportedType.typeInfo);
				weightLoaders.add(new LoadInsnTree(varInfo));
				methodParameters.add(varInfo);
				expectedParameterCount++;
			}
		}
		if (expectedParameterCount != type.parameterCount()) {
			throw new IllegalArgumentException(type.parameterCount() + " parameters were expected, but " + expectedParameterCount + " slots were empty");
		}

		//create class.
		ClassCompileContext clazz = new ClassCompileContext(
			ACC_PUBLIC | ACC_SUPER,
			ClassType.CLASS,
			BASE_NAME + "$Generated_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			TypeInfos.OBJECT,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		clazz.addNoArgConstructor(ACC_PUBLIC);
		MethodCompileContext method = clazz.newMethod(ACC_PUBLIC | ACC_STATIC, name, TypeInfos.INT, methodParameters.toArray(LazyVarInfo.ARRAY_FACTORY));
		LazyVarInfo totalWeight = method.scopes.addVariable("totalWeight", supportedType.typeInfo);
		store(totalWeight, ldc(Array.get(totalConstantWeight, 0), supportedType.typeInfo)).emitBytecode(method);
		for (int index = 1, size = methodParameters.size(); index < size; index++) {
			LazyVarInfo parameter = methodParameters.get(index);
			IfInsnTree.create(
					switch (supportedType) {
						case INT -> IntCompareZeroConditionTree.greaterThanZero(load(parameter));
						case LONG -> LongCompareConditionTree.greaterThan(load(parameter), ldc(0L));
						case FLOAT -> FloatCompareConditionTree.greaterThan(load(parameter), ldc(0.0F));
						case DOUBLE -> DoubleCompareConditionTree.greaterThan(load(parameter), ldc(0.0D));
					},
					new VariableUpdaterInsnTree(
						CombinedMode.VOID,
						VariableUpdaterEmitters.forLazyVariable(
							totalWeight,
							new AddInsnTree(
								getFromStack(supportedType.typeInfo),
								load(parameter),
								switch (supportedType) {
									case INT -> IADD;
									case LONG -> LADD;
									case FLOAT -> FADD;
									case DOUBLE -> DADD;
								}
							)
						)
					)
				)
				.emitBytecode(method);
		}
		ifThen(
			switch (supportedType) {
				case INT -> IntCompareZeroConditionTree.equalZero(load(totalWeight));
				case LONG -> LongCompareConditionTree.equal(load(totalWeight), ldc(0L));
				case FLOAT -> FloatCompareConditionTree.equal(load(totalWeight), ldc(0.0F));
				case DOUBLE -> DoubleCompareConditionTree.equal(load(totalWeight), ldc(0.0D));
			},
			return_(ldc(-1))
		)
			.emitBytecode(method);
		LazyVarInfo selector = method.scopes.addVariable("selector", supportedType.typeInfo);
		store(
			selector,
			invokeInstance(
				load(rngSource),
				switch (supportedType) {
					case INT -> (
						useSeed
							? MethodInfo.findMethod(Permuter.class, "nextBoundedInt", int.class, long.class, int.class)
							: MethodInfo.findMethod(RandomGenerator.class, "nextInt", int.class, int.class)
					);
					case LONG -> (
						useSeed
							? MethodInfo.findMethod(Permuter.class, "nextBoundedLong", long.class, long.class, long.class)
							: MethodInfo.findMethod(RandomGenerator.class, "nextLong", long.class, long.class)
					);
					case FLOAT -> (
						useSeed
							? MethodInfo.findMethod(Permuter.class, "nextBoundedFloat", float.class, long.class, float.class)
							: MethodInfo.findMethod(RandomGenerator.class, "nextFloat", float.class, float.class)
					);
					case DOUBLE -> (
						useSeed
							? MethodInfo.findMethod(Permuter.class, "nextBoundedDouble", double.class, long.class, double.class)
							: MethodInfo.findMethod(RandomGenerator.class, "nextDouble", double.class, double.class)
					);
				},
				load(totalWeight)
			)
		)
			.emitBytecode(method);
		for (int index = 0, size = weightLoaders.size() - 1; index < size; index++) {
			VariableUpdaterInsnTree subtractedValue = new VariableUpdaterInsnTree(
				CombinedMode.POST,
				VariableUpdaterEmitters.forLazyVariable(
					selector,
					new SubtractInsnTree(
						getFromStack(supportedType.typeInfo),
						weightLoaders.get(index),
						switch (supportedType) {
							case INT -> ISUB;
							case LONG -> LSUB;
							case FLOAT -> FSUB;
							case DOUBLE -> DSUB;
						}
					)
				)
			);
			IfInsnTree.create(
					switch (supportedType) {
						case INT -> IntCompareZeroConditionTree.lessThanZero(subtractedValue);
						case LONG -> LongCompareConditionTree.lessThan(subtractedValue, ldc(0L));
						case FLOAT -> FloatCompareConditionTree.lessThan(subtractedValue, ldc(0.0F));
						case DOUBLE -> DoubleCompareConditionTree.lessThan(subtractedValue, ldc(0.0D));
					},
					return_(ldc(index))
				)
				.emitBytecode(method);
		}
		return_(ldc(weightLoaders.size() - 1)).emitBytecode(method);
		method.endCode();
		MethodHandles.Lookup generated = MethodHandles.lookup().defineHiddenClass(clazz.toByteArray(), true);
		return new ConstantCallSite(generated.findStatic(generated.lookupClass(), name, type));
	}

	public static enum SupportedType {
		INT(int.class),
		LONG(long.class),
		FLOAT(float.class),
		DOUBLE(double.class);

		public static final SupportedType[] VALUES = values();

		public final Class<?> clazz;
		public final TypeInfo typeInfo;

		SupportedType(Class<?> clazz) {
			this.clazz = clazz;
			this.typeInfo = type(clazz);
		}

		public static SupportedType forClass(Class<?> clazz) {
			for (SupportedType type : VALUES) {
				if (type.clazz == clazz) return type;
			}
			throw new IllegalArgumentException("Unsupported class: " + clazz);
		}
	}
}