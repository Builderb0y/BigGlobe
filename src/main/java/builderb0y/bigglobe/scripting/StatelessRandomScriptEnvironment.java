package builderb0y.bigglobe.scripting;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class StatelessRandomScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
		.addMethod(type(long.class), "newSeed", new MethodHandler.Named("long.newSeed(int...)", (parser, receiver, name, arguments) -> {
			if (arguments.length == 0) {
				return new CastResult(add(parser, receiver, ldc(Permuter.PHI64)), false);
			}
			return RandomScriptEnvironment.createSeed(parser, arguments);
		}))
		.addMethodMultiInvokeStatics(StatelessRandomScriptEnvironment.class, "nextBoolean", "nextInt", "nextLong", "nextFloat", "nextDouble")
	);

	public static boolean nextBoolean(long seed) {
		return Permuter.nextBoolean(seed);
	}

	public static boolean nextBoolean(long seed, float chance) {
		return Permuter.nextChancedBoolean(seed, chance);
	}

	public static boolean nextBoolean(long seed, double chance) {
		return Permuter.nextChancedBoolean(seed, chance);
	}

	public static int nextInt(long seed) {
		return Permuter.nextUniformInt(seed);
	}

	public static int nextInt(long seed, int max) {
		return Permuter.nextBoundedInt(seed, max);
	}

	public static int nextInt(long seed, int min, int max) {
		return Permuter.nextBoundedInt(seed, min, max);
	}

	public static long nextLong(long seed) {
		return Permuter.nextUniformLong(seed);
	}

	public static long nextLong(long seed, long max) {
		return Permuter.nextBoundedLong(seed, max);
	}

	public static long nextLong(long seed, long min, long max) {
		return Permuter.nextBoundedLong(seed, min, max);
	}

	public static float nextFloat(long seed) {
		return Permuter.nextPositiveFloat(seed);
	}

	public static float nextFloat(long seed, float max) {
		return Permuter.nextBoundedFloat(seed, max);
	}

	public static float nextFloat(long seed, float min, float max) {
		return Permuter.nextBoundedFloat(seed, min, max);
	}

	public static double nextDouble(long seed) {
		return Permuter.nextPositiveDouble(seed);
	}

	public static double nextDouble(long seed, double max) {
		return Permuter.nextBoundedDouble(seed, max);
	}

	public static double nextDouble(long seed, double min, double max) {
		return Permuter.nextBoundedDouble(seed, min, max);
	}
}