package builderb0y.bigglobe.columns.scripted.classes;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NormalMethodSpec extends BaseMethodSpec {

	public final @IdentifierName String name;
	public final RegistryEntry<ElementSpec> return_type;
	public final ParameterSpec[] parameters;
	public final ScriptUsage code;
	public final transient Set<RegistryEntry<? extends DependencyView>> dependencies = new HashSet<>();

	public NormalMethodSpec(
		String name,
		RegistryEntry<ElementSpec> return_type,
		ParameterSpec[] parameters,
		ScriptUsage code
	) {
		this.name = name;
		this.return_type = return_type;
		this.parameters = parameters;
		this.code = code;
	}

	@Override
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addInstanceMethod(this);
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
	public void compile(ClassHierarchy hierarchy, BaseClassSpec clazz) throws ScriptParsingException {
		this.compile(hierarchy, clazz, this.code);
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
		return ACC_PUBLIC;
	}

	@Override
	public String name() {
		return this.name;
	}
}