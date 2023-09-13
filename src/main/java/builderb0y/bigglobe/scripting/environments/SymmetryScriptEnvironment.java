package builderb0y.bigglobe.scripting.environments;

import java.util.Arrays;

import builderb0y.bigglobe.util.Symmetry;
import builderb0y.scripting.bytecode.FieldConstantFactory;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SymmetryScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
		.addType("Symmetry", Symmetry.class)
		.addCastConstant(FieldConstantFactory.forEnum(Symmetry.class), true)
		.addQualifiedVariableGetStatics(Symmetry.class, Arrays.stream(Symmetry.VALUES).map(Symmetry::name).toArray(String[]::new))
		.addMethodMultiInvokes(Symmetry.class, "getX", "getZ", "apply", "andThen", "compose", "inverse")
		.addQualifiedFunctionInvokeStatics(Symmetry.class, "rotation", "randomRotation", "flip", "randomFlip", "randomRotationAndFlip")
	);

	public static MutableScriptEnvironment create(InsnTree loadRandom) {
		return (
			new MutableScriptEnvironment()
			.addAll(INSTANCE)
			.addQualifiedFunction(type(Symmetry.class), "randomRotation", Handlers.builder(Symmetry.class, "randomRotation").addImplicitArgument(loadRandom).buildFunction())
			.addQualifiedFunction(type(Symmetry.class), "randomFlip", Handlers.builder(Symmetry.class, "randomFlip").addImplicitArgument(loadRandom).buildFunction())
			.addQualifiedFunction(type(Symmetry.class), "randomRotationAndFlip", Handlers.builder(Symmetry.class, "randomRotationAndFlip").addImplicitArgument(loadRandom).buildFunction())
		);
	}
}