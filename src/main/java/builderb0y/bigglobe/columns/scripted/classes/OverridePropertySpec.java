package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Set;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class OverridePropertySpec extends BasePropertySpec {

	public final Holder<ElementSpec> override;
	public final ScriptUsage get;
	public final @VerifyNullable ScriptUsage set;

	public OverridePropertySpec(
		Holder<ElementSpec> override,
		ScriptUsage get,
		@VerifyNullable ScriptUsage set
	) {
		this.override = override;
		this.get = get;
		this.set = set;
	}

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		if (!(this.override.value() instanceof BasePropertySpec override)) {
			throw new CustomClassFormatException("Override property " + UnregisteredObjectException.getID(hierarchy.entryOf(this)) + " overrides non-property " + UnregisteredObjectException.getID(this.override));
		}
		else {
			if (this.isSettable() && !override.isSettable()) {
				throw new CustomClassFormatException("Override property " + UnregisteredObjectException.getID(hierarchy.entryOf(this)) + " cannot be settable if its override (" + UnregisteredObjectException.getID(this.override) + ") is not settable.");
			}
			else if (!this.isSettable() && override.isSettable()) {
				throw new CustomClassFormatException("Override property " + UnregisteredObjectException.getID(hierarchy.entryOf(this)) + " must be settable if its override (" + UnregisteredObjectException.getID(this.override) + ") is settable.");
			}
		}
	}

	@Override
	public void compile(ClassHierarchy hierarchy, BaseClassSpec owner) throws ScriptParsingException {
		PropertyCompileContext propertyContext = owner.getCompileContext(this);
		InsnTree loadY = this.is3D() ? load("y", TypeInfos.INT) : null;
		compile(
			hierarchy, owner, propertyContext.get, this.get, loadY, this, (MutableScriptEnvironment environment) -> {
				if (this.is3D()) environment.addVariableLoad("y", TypeInfos.INT);
			}
		);
		if (this.set != null) compile(
			hierarchy, owner, propertyContext.set, this.set, loadY, this, (MutableScriptEnvironment environment) -> {
				if (this.is3D()) environment.addVariableLoad("y", TypeInfos.INT);
				environment.addVariableLoad("value", asType(this.getPropertyType()).getTypeInfo());
			}
		);
	}

	@Override
	public boolean isSettable() {
		return this.set != null;
	}

	@Override
	public boolean is3D() {
		return ((BasePropertySpec)(this.override.value())).is3D();
	}

	@Override
	public Holder<ElementSpec> getPropertyType() {
		return ((BasePropertySpec)(this.override.value())).getPropertyType();
	}

	@Override
	public int flags() {
		return ACC_PUBLIC;
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addOverrideProperty(this);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, @Nullable InsnTree loadCustomClass) {
		//no-op. base method can be called as-is.
	}

	@Override
	public String name() {
		return this.override.value().name();
	}

	@Override
	public Set<Holder<? extends DependencyView>> getDependencies() {
		return ((BasePropertySpec)(this.override.value())).getDependencies();
	}
}