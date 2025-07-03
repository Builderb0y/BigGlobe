package builderb0y.bigglobe.columns.scripted.classes;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AbstractPropertySpec extends BasePropertySpec {

	public final @IdentifierName String name;
	public final RegistryEntry<ElementSpec> type;
	public final boolean settable;
	public final transient Set<RegistryEntry<? extends DependencyView>> dependencies = new HashSet<>();

	public AbstractPropertySpec(@IdentifierName String name, RegistryEntry<ElementSpec> type, boolean settable) {
		this.name = name;
		this.type = type;
		this.settable = settable;
	}

	@Override
	public boolean isSettable() {
		return this.settable;
	}

	@Override
	public RegistryEntry<ElementSpec> getPropertyType() {
		return this.type;
	}

	@Override
	public int flags() {
		return ACC_PUBLIC | ACC_ABSTRACT;
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addAbstractProperty(this);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, @Nullable InsnTree loadCustomClass) {
		PropertyCompileContext propertyContext = owner.getCompileContext(this);
		if (this.settable) {
			environment.addFieldGetterSetter(propertyContext.get.clazz.info, this.name, propertyContext.get.info, propertyContext.set.info);
			if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(propertyContext.get.clazz.info)) {
				environment.addVariableGetterSetter(loadCustomClass, this.name, propertyContext.get.info, propertyContext.set.info);
			}
		}
		else {
			environment.addFieldInvoke(propertyContext.get.info);
			if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(propertyContext.get.clazz.info)) {
				environment.addVariableInvoke(loadCustomClass, propertyContext.get.info);
			}
		}
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}
}