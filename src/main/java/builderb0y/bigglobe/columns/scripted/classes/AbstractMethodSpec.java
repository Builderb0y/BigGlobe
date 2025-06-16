package builderb0y.bigglobe.columns.scripted.classes;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AbstractMethodSpec extends MethodSpec {

	public final @IdentifierName String name;
	public final RegistryEntry<ElementSpec> return_type;
	public final ParameterSpec[] parameters;
	public transient final Set<RegistryEntry<? extends DependencyView>> dependencies = new HashSet<>();

	public AbstractMethodSpec(
		@IdentifierName String name,
		RegistryEntry<ElementSpec> return_type,
		ParameterSpec[] parameters
	) {
		this.name = name;
		this.return_type = return_type;
		this.parameters = parameters;
	}

	@Override
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addAbstractMethod(this);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, ClassCompileContext caller) {
		MethodCompileContext methodContext = owner.getCompileContext(this);
		environment.addMethodInvoke(methodContext.info);
		if (caller.info.extendsOrImplements(methodContext.clazz.info)) {
			environment.addFunctionInvoke(load("this", caller.info), methodContext.info);
		}
	}

	@Override
	public RegistryEntry<ElementSpec> getReturnType() {
		return this.return_type;
	}

	@Override
	public ParameterSpec[] getParameters() {
		return this.parameters;
	}

	@Override
	public int flags() {
		return ACC_PUBLIC | ACC_ABSTRACT;
	}

	@Override
	public String name() {
		return this.name;
	}
}