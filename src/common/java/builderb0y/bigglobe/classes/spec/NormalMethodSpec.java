package builderb0y.bigglobe.classes.spec;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NormalMethodSpec extends BaseMethodSpec {

	public final @IdentifierName String name;
	public final Holder<ElementSpec> return_type;

	@Override
	public TypeSpec returnTypeSpec(ClassHierarchy hierarchy) {
		return requireType(this.return_type, TypeSpec.class, () -> hierarchy.idOf(this) + " > return_type");
	}

	public final ParameterSpec[] parameters;
	public final ScriptUsage code;

	public NormalMethodSpec(
		Holder<ElementSpec> owner,
		String name,
		Holder<ElementSpec> return_type,
		ParameterSpec[] parameters,
		ScriptUsage code
	) {
		super(owner);
		this.name = name;
		this.return_type = return_type;
		this.parameters = parameters;
		this.code = code;
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		this.owner(hierarchy).overrideTracker.addInstanceMethod(this);
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		MethodInfo methodInfo = this.context.info;
		environment.addMethod(Handlers.methodBuilder(methodInfo).addReceiverArgument(methodInfo.owner).addArguments((Object[])(methodInfo.paramTypes)).onUsed(params.dependencyCallback(self)).buildMethod());
	}

	@Override
	@MustBeInvokedByOverriders
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		super.compile(hierarchy);
		this.compile(hierarchy, this.code, load("this", this.owner(hierarchy).getTypeInfo()), (MutableScriptEnvironment environment) -> {
			for (ParameterSpec parameter : this.parameters) {
				environment.addVariableLoad(parameter.name, parameter.typeInfo());
			}
		});
	}

	@Override
	public Holder<ElementSpec> getReturnType() {
		return this.return_type;
	}

	@Override
	public ParameterSpec[] getParameters() {
		return this.parameters;
	}

	@Override
	public int accessFlags() {
		return ACC_PUBLIC;
	}

	@Override
	public String name() {
		return this.name;
	}
}