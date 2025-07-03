package builderb0y.bigglobe.columns.scripted.classes;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AbstractMethodSpec extends BaseMethodSpec {

	public final @IdentifierName String name;
	public final RegistryEntry<ElementSpec> return_type;
	public final ParameterSpec[] parameters;
	public final transient Set<RegistryEntry<? extends DependencyView>> dependencies = new HashSet<>();

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
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, @Nullable InsnTree loadCustomClass) {
		MethodCompileContext methodContext = owner.getCompileContext(this);
		environment.addMethodInvoke(methodContext.info);
		if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(methodContext.clazz.info)) {
			environment.addFunctionInvoke(loadCustomClass, methodContext.info);
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