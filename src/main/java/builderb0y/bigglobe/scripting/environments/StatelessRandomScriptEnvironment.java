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
		.addMethodInvoke("nextInt", RandomScriptEnvironment.PERMUTER_INFO.nextPositiveInt)
		.addMethodInvoke("nextInt", RandomScriptEnvironment.PERMUTER_INFO.nextIntBound)
		.addMethodInvoke("nextInt", RandomScriptEnvironment.PERMUTER_INFO.nextIntOriginBound)

		.addMethodInvoke("nextLong", RandomScriptEnvironment.PERMUTER_INFO.nextPositiveLong)
		.addMethodInvoke("nextLong", RandomScriptEnvironment.PERMUTER_INFO.nextLongBound)
		.addMethodInvoke("nextLong", RandomScriptEnvironment.PERMUTER_INFO.nextLongOriginBound)

		.addMethodInvoke("nextFloat", RandomScriptEnvironment.PERMUTER_INFO.nextPositiveFloat)
		.addMethodInvoke("nextFloat", RandomScriptEnvironment.PERMUTER_INFO.nextFloatBound)
		.addMethodInvoke("nextFloat", RandomScriptEnvironment.PERMUTER_INFO.nextFloatOriginBound)

		.addMethodInvoke("nextDouble", RandomScriptEnvironment.PERMUTER_INFO.nextPositiveDouble)
		.addMethodInvoke("nextDouble", RandomScriptEnvironment.PERMUTER_INFO.nextDoubleBound)
		.addMethodInvoke("nextDouble", RandomScriptEnvironment.PERMUTER_INFO.nextDoubleOriginBound)

		.addMethodInvoke("nextBoolean", RandomScriptEnvironment.PERMUTER_INFO.nextBoolean)
		.addMethodInvoke("nextBoolean", RandomScriptEnvironment.PERMUTER_INFO.nextChancedBooleanF)
		.addMethodInvoke("nextBoolean", RandomScriptEnvironment.PERMUTER_INFO.nextChancedBooleanD)
	);
}