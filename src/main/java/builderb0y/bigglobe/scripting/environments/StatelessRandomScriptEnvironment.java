package builderb0y.bigglobe.scripting.environments;

import com.google.common.collect.ObjectArrays;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class StatelessRandomScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
		.addMethod(type(long.class), "newSeed", new MethodHandler.Named("long.newSeed(int...)", (parser, receiver, name, mode, arguments) -> {
			//primitive long will never be null, so I don't need to check the mode here.
			if (arguments.length == 0) {
				return new CastResult(add(parser, receiver, ldc(Permuter.PHI64)), false);
			}
			return RandomScriptEnvironment.createSeed(parser, ObjectArrays.concat(receiver, arguments));
		}))
		.addMethodInvokeStatic("nextInt", RandomScriptEnvironment.PERMUTER_INFO.nextPositiveInt)
		.addMethodInvokeStatic("nextInt", RandomScriptEnvironment.PERMUTER_INFO.nextIntBound)
		.addMethodInvokeStatic("nextInt", RandomScriptEnvironment.PERMUTER_INFO.nextIntOriginBound)

		.addMethodInvokeStatic("nextLong", RandomScriptEnvironment.PERMUTER_INFO.nextPositiveLong)
		.addMethodInvokeStatic("nextLong", RandomScriptEnvironment.PERMUTER_INFO.nextLongBound)
		.addMethodInvokeStatic("nextLong", RandomScriptEnvironment.PERMUTER_INFO.nextLongOriginBound)

		.addMethodInvokeStatic("nextFloat", RandomScriptEnvironment.PERMUTER_INFO.nextPositiveFloat)
		.addMethodInvokeStatic("nextFloat", RandomScriptEnvironment.PERMUTER_INFO.nextFloatBound)
		.addMethodInvokeStatic("nextFloat", RandomScriptEnvironment.PERMUTER_INFO.nextFloatOriginBound)

		.addMethodInvokeStatic("nextDouble", RandomScriptEnvironment.PERMUTER_INFO.nextPositiveDouble)
		.addMethodInvokeStatic("nextDouble", RandomScriptEnvironment.PERMUTER_INFO.nextDoubleBound)
		.addMethodInvokeStatic("nextDouble", RandomScriptEnvironment.PERMUTER_INFO.nextDoubleOriginBound)

		.addMethodInvokeStatic("nextBoolean", RandomScriptEnvironment.PERMUTER_INFO.nextBoolean)
		.addMethodInvokeStatic("nextBoolean", RandomScriptEnvironment.PERMUTER_INFO.nextChancedBooleanF)
		.addMethodInvokeStatic("nextBoolean", RandomScriptEnvironment.PERMUTER_INFO.nextChancedBooleanD)

		.addMethodInvokeStatic("roundInt", RandomScriptEnvironment.PERMUTER_INFO.roundRandomlyIF)
		.addMethodInvokeStatic("roundInt", RandomScriptEnvironment.PERMUTER_INFO.roundRandomlyID)
		.addMethodInvokeStatic("roundLong", RandomScriptEnvironment.PERMUTER_INFO.roundRandomlyLF)
		.addMethodInvokeStatic("roundLong", RandomScriptEnvironment.PERMUTER_INFO.roundRandomlyLD)

		.addMethodInvokeStatic("nextBoolean", RandomScriptEnvironment.PERMUTER_INFO.nextChancedBooleanF)
		.addMethodInvokeStatic("nextBoolean", RandomScriptEnvironment.PERMUTER_INFO.nextChancedBooleanD)
	);
}