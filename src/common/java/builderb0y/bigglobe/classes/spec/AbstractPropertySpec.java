package builderb0y.bigglobe.classes.spec;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.OverrideTracker;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AbstractPropertySpec extends BasePropertySpec {

	public final @IdentifierName String name;
	public final Holder<ElementSpec> property_type;
	public final boolean settable;
	public final @DefaultBoolean(false) boolean is_3d;
	public final transient Set<Holder<? extends DependencyView>> dependencies = new HashSet<>();

	public AbstractPropertySpec(
		@IdentifierName String name,
		Holder<ElementSpec> property_type,
		boolean settable,
		boolean is3d
	) {
		this.name = name;
		this.property_type = property_type;
		this.settable = settable;
		this.is_3d = is3d;
	}

	@Override
	public boolean isSettable() {
		return this.settable;
	}

	@Override
	public boolean is3D() {
		return this.is_3d;
	}

	@Override
	public Holder<ElementSpec> getPropertyType() {
		return this.property_type;
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
	public Set<Holder<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}
}