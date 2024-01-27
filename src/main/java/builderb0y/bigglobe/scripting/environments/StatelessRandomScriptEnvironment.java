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
		.addMethodRenamedInvokeStaticSpecific("nextBoolean", Permuter.class, "nextBoolean", boolean.class, long.class)
		.addMethodRenamedInvokeStaticSpecific("nextBoolean", Permuter.class, "nextChancedBoolean", boolean.class, long.class, float.class)
		.addMethodRenamedInvokeStaticSpecific("nextBoolean", Permuter.class, "nextChancedBoolean", boolean.class, long.class, double.class)

		.addMethodRenamedInvokeStaticSpecific("nextInt", Permuter.class, "nextUniformInt", int.class, long.class)
		.addMethodRenamedInvokeStaticSpecific("nextInt", Permuter.class, "nextBoundedInt", int.class, long.class, int.class)
		.addMethodRenamedInvokeStaticSpecific("nextInt", Permuter.class, "nextBoundedInt", int.class, long.class, int.class, int.class)

		.addMethodRenamedInvokeStaticSpecific("nextLong", Permuter.class, "nextUniformLong", long.class, long.class)
		.addMethodRenamedInvokeStaticSpecific("nextLong", Permuter.class, "nextBoundedLong", long.class, long.class, long.class)
		.addMethodRenamedInvokeStaticSpecific("nextLong", Permuter.class, "nextBoundedLong", long.class, long.class, long.class, long.class)

		.addMethodRenamedInvokeStaticSpecific("nextFloat", Permuter.class, "nextPositiveFloat", float.class, long.class)
		.addMethodRenamedInvokeStaticSpecific("nextFloat", Permuter.class, "nextBoundedFloat", float.class, long.class, float.class)
		.addMethodRenamedInvokeStaticSpecific("nextFloat", Permuter.class, "nextBoundedFloat", float.class, long.class, float.class, float.class)

		.addMethodRenamedInvokeStaticSpecific("nextDouble", Permuter.class, "nextPositiveDouble", double.class, long.class)
		.addMethodRenamedInvokeStaticSpecific("nextDouble", Permuter.class, "nextBoundedDouble", double.class, long.class, double.class)
		.addMethodRenamedInvokeStaticSpecific("nextDouble", Permuter.class, "nextBoundedDouble", double.class, long.class, double.class, double.class)
	);
}