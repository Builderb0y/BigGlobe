package builderb0y.bigglobe.columns.scripted.classes;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NormalPropertySpec extends BasePropertySpec {

	public final @IdentifierName String name;
	public final RegistryEntry<ElementSpec> type;
	public final ScriptUsage get;
	public final @VerifyNullable ScriptUsage set;
	public final transient Set<RegistryEntry<? extends DependencyView>> dependencies = new HashSet<>();

	public NormalPropertySpec(@IdentifierName String name, RegistryEntry<ElementSpec> type, ScriptUsage get, @VerifyNullable ScriptUsage set) {
		this.name = name;
		this.type = type;
		this.get  = get;
		this.set  = set;
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, ClassCompileContext caller) {
		PropertyCompileContext propertyContext = owner.getCompileContext(this);
		if (this.set != null) {
			environment.addFieldGetterSetter(propertyContext.get.clazz.info, this.name, propertyContext.get.info, propertyContext.set.info);
			if (caller.info.extendsOrImplements(propertyContext.get.clazz.info)) {
				environment.addVariableGetterSetter(load("this", caller.info), this.name, propertyContext.get.info, propertyContext.set.info);
			}
		}
		else {
			environment.addFieldInvoke(propertyContext.get.info);
			if (caller.info.extendsOrImplements(propertyContext.get.clazz.info)) {
				environment.addVariableInvoke(load("this", caller.info), propertyContext.get.info);
			}
		}
	}

	@Override
	public void compile(ClassHierarchy hierarchy, BaseClassSpec owner) throws ScriptParsingException {
		PropertyCompileContext propertyContext = owner.getCompileContext(this);
		compile(hierarchy, owner, propertyContext.get, this.get, this);
		if (this.set != null) compile(hierarchy, owner, propertyContext.set, this.set, this);
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addNormalProperty(this);
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public RegistryEntry<ElementSpec> getPropertyType() {
		return this.type;
	}

	@Override
	public boolean isSettable() {
		return this.set != null;
	}

	@Override
	public int flags() {
		return ACC_PUBLIC;
	}

	@Override
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}
}