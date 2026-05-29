package builderb0y.bigglobe.math;

import java.lang.invoke.*;
import java.util.Arrays;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.objectweb.asm.Type;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class GeneralSmoothstep {

	public static final MethodInfo BSM = MethodInfo.inCaller("getCallSite");

	public static abstract class SmoothstepOperator implements DoubleUnaryOperator {

		public abstract float applyAsFloat(float x);

		@Override
		public abstract double applyAsDouble(double x);

		public abstract int getLowerSmoothening();

		public abstract int getUpperSmoothening();
	}

	public static record CacheKey(int lowerSmoothening, int upperSmoothening) {

		public CacheKey {
			if (lowerSmoothening < 0) throw new IllegalArgumentException("lowerSmoothening must be greater than or equal to 0 (was " + lowerSmoothening + ")");
			if (upperSmoothening < 0) throw new IllegalArgumentException("upperSmoothening must be greater than or equal to 0 (was " + upperSmoothening + ")");
			if (lowerSmoothening > 256) throw new IllegalArgumentException("lowerSmoothening must be less than or equal to 256 (was " + lowerSmoothening + ")");
			if (upperSmoothening > 256) throw new IllegalArgumentException("upperSmoothening must be less than or equal to 256 (was " + upperSmoothening + ")");
		}
	}

	public static record CacheEntry(
		MethodHandles.Lookup lookup,
		SmoothstepOperator operator,
		CallSite floatCallSite,
		CallSite doubleCallSite
	) {

		public Class<? extends SmoothstepOperator> operatorClass() {
			return this.operator.getClass();
		}

		public int lowerSmoothening() {
			return this.operator.getLowerSmoothening();
		}

		public int upperSmoothening() {
			return this.operator.getUpperSmoothening();
		}

		public MethodHandle floatTarget() {
			return this.floatCallSite.getTarget();
		}

		public MethodHandle doubleTarget() {
			return this.doubleCallSite.getTarget();
		}
	}

	public static final Map<CacheKey, CacheEntry> CACHE = new Object2ObjectOpenHashMap<>();

	public static SmoothstepOperator getOperator(int lowerSmoothening, int upperSmoothening) {
		return getCacheEntry(lowerSmoothening, upperSmoothening).operator;
	}

	public static final MethodType
		FLOAT_METHOD_TYPE = MethodType.methodType(float.class, float.class),
		DOUBLE_METHOD_TYPE = MethodType.methodType(double.class, double.class);

	public static CallSite getCallSite(MethodHandles.Lookup lookup, String name, MethodType methodType, int lowerSmoothening, int upperSmoothening) {
		if (methodType == FLOAT_METHOD_TYPE) return getCacheEntry(lowerSmoothening, upperSmoothening).floatCallSite;
		if (methodType == DOUBLE_METHOD_TYPE) return getCacheEntry(lowerSmoothening, upperSmoothening).doubleCallSite;
		throw new WrongMethodTypeException(methodType.toString());
	}

	public static CacheEntry getCacheEntry(int lowerSmoothing, int upperSmoothening) {
		synchronized (CACHE) {
			CacheKey key = new CacheKey(lowerSmoothing, upperSmoothening);
			CacheEntry entry = CACHE.get(key);
			if (entry == null) {
				entry = createCacheEntry(lowerSmoothing, upperSmoothening);
				CACHE.put(key, entry);
			}
			return entry;
		}
	}

	public static volatile int counter;

	public static CacheEntry createCacheEntry(int lowerSmoothening, int upperSmoothening) {
		assert Thread.holdsLock(CACHE);
		ClassCompileContext clazz = new ClassCompileContext(
			ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC | ACC_SUPER,
			ClassType.CLASS,
			Type.getInternalName(SmoothstepOperator.class) + "$Generated_" + counter++,
			TypeInfo.of(SmoothstepOperator.class),
			TypeInfo.ARRAY_FACTORY.empty()
		);
		clazz.addNoArgConstructor(ACC_PUBLIC);

		MethodCompileContext smoothstepF = clazz.newMethod(ACC_PUBLIC | ACC_STATIC, "smoothstep", TypeInfos.FLOAT, new LazyVarInfo("x", TypeInfos.FLOAT));
		emitGuard(smoothstepF);
		emitCode(smoothstepF, lowerSmoothening, upperSmoothening);
		smoothstepF.endCode();

		MethodCompileContext smoothstepD = clazz.newMethod(ACC_PUBLIC | ACC_STATIC, "smoothstep", TypeInfos.DOUBLE, new LazyVarInfo("x", TypeInfos.DOUBLE));
		emitGuard(smoothstepD);
		emitCode(smoothstepD, lowerSmoothening, upperSmoothening);
		smoothstepD.endCode();

		MethodCompileContext applyAsFloat = clazz.newMethod(ACC_PUBLIC, "applyAsFloat", TypeInfos.FLOAT, new LazyVarInfo("x", TypeInfos.FLOAT));
		return_(invokeStatic(smoothstepF.info, load("x", TypeInfos.FLOAT))).emitBytecode(applyAsFloat);
		applyAsFloat.endCode();

		MethodCompileContext applyAsDouble = clazz.newMethod(ACC_PUBLIC, "applyAsDouble", TypeInfos.DOUBLE, new LazyVarInfo("x", TypeInfos.DOUBLE));
		return_(invokeStatic(smoothstepD.info, load("x", TypeInfos.DOUBLE))).emitBytecode(applyAsDouble);
		applyAsDouble.endCode();

		MethodCompileContext getLowerSmoothening = clazz.newMethod(ACC_PUBLIC, "getLowerSmoothening", TypeInfos.INT);
		return_(ldc(lowerSmoothening)).emitBytecode(getLowerSmoothening);
		getLowerSmoothening.endCode();

		MethodCompileContext getUpperSmoothening = clazz.newMethod(ACC_PUBLIC, "getUpperSmoothening", TypeInfos.INT);
		return_(ldc(upperSmoothening)).emitBytecode(getUpperSmoothening);
		getUpperSmoothening.endCode();

		clazz.addToString("SmoothstepOperator(" + lowerSmoothening + ", " + upperSmoothening + ')');

		return defineClass(clazz);
	}

	public static void emitGuard(MethodCompileContext method) {
		TypeInfo type = method.info.returnType;
		LazyVarInfo x = new LazyVarInfo("x", type);
		ifThen(not(gt(CastingSupport.dummyParser(), load(x), ldc(0, type))), return_(ldc(0, type))).emitBytecode(method);
		ifThen(not(lt(CastingSupport.dummyParser(), load(x), ldc(1, type))), return_(ldc(1, type))).emitBytecode(method);
	}

	public static void emitCode(MethodCompileContext method, int lowerSmoothening, int upperSmoothening) {
		TypeInfo type = method.info.returnType;
		LazyVarInfo x = new LazyVarInfo("x", type);
		if (upperSmoothening > 0) {
			ExpressionParser parser = CastingSupport.dummyParser();
			LazyVarInfo x1 = method.scopes.addVariable("x1", type);
			store(x1, sub(parser, ldc(1.0F), load(x))).emitBytecode(method);
			FastPow.emitNormalInstructions(method, x, lowerSmoothening + 1, Integer.MAX_VALUE);
			//x ^ (lowerSmoothening + 1) on the stack.
			int[] coefficients = genCoefficients(lowerSmoothening, upperSmoothening);
			int index = coefficients.length - 1;
			InsnTree polynomial = ldc(coefficients[index], type);
			while (--index >= 0) {
				polynomial = add(parser, mul(parser, polynomial, load(x1)), ldc(coefficients[index], type));
			}
			return_(mul(parser, getFromStack(type), polynomial)).emitBytecode(method);
		}
		else {
			FastPow.emitNormalInstructions(method, x, lowerSmoothening + 1, Integer.MAX_VALUE);
			return_(getFromStack(type)).emitBytecode(method);
		}
	}

	public static int[] genCoefficients(int lowerSmoothening, int upperSmoothening) {
		int[] coefficients = new int[upperSmoothening + 1];
		Arrays.fill(coefficients, 1);
		for (int outer = 0; outer < lowerSmoothening; outer++) {
			for (int inner = 0; inner < upperSmoothening; inner++) {
				coefficients[inner + 1] += coefficients[inner];
			}
		}
		return coefficients;
	}

	public static CacheEntry defineClass(ClassCompileContext clazz) {
		try {
			MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(clazz.toByteArray(), true);
			Class<?> operatorClass = lookup.lookupClass();
			return new CacheEntry(
				lookup,
				(SmoothstepOperator)(lookup.findConstructor(operatorClass, MethodType.methodType(void.class)).invoke()),
				new ConstantCallSite(lookup.findStatic(operatorClass, "smoothstep", FLOAT_METHOD_TYPE)),
				new ConstantCallSite(lookup.findStatic(operatorClass, "smoothstep", DOUBLE_METHOD_TYPE))
			);
		}
		catch (Throwable throwable) {
			throw AutoCodecUtil.rethrow(throwable);
		}
	}
}