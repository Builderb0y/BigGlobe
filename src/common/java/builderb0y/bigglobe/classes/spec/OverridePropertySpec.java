package builderb0y.bigglobe.classes.spec;

import java.util.HashSet;
import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class OverridePropertySpec extends BasePropertySpec {

	public final Holder<ElementSpec> override;
	public BasePropertySpec override(ClassHierarchy hierarchy) {
		return requireType(this.override, BasePropertySpec.class, () -> hierarchy.idOf(this) + " > override");
	}
	public final ScriptUsage get;
	public final @VerifyNullable ScriptUsage set;
	public final transient SetBasedMutableDependencyView dependencies = SetBasedMutableDependencyView.from(new HashSet<>());

	public OverridePropertySpec(
		Holder<ElementSpec> owner,
		Holder<ElementSpec> override,
		ScriptUsage get,
		@VerifyNullable ScriptUsage set
	) {
		super(owner);
		this.override = override;
		this.get = get;
		this.set = set;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(super.streamDirectDependencies(), this.dependencies.streamDirectDependencies());
	}

	@Override
	public TypeSpec getPropertyTypeSpec(ClassHierarchy hierarchy) {
		return this.override(hierarchy).getPropertyTypeSpec(hierarchy);
	}

	@Override
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		BasePropertySpec override = this.override(hierarchy);
		if (this.isSettable() && !override.isSettable()) {
			throw new CustomClassFormatException("Override property " + UnregisteredObjectException.getID(hierarchy.entryOf(this)) + " cannot be settable if its override (" + UnregisteredObjectException.getID(this.override) + ") is not settable.");
		}
		else if (!this.isSettable() && override.isSettable()) {
			throw new CustomClassFormatException("Override property " + UnregisteredObjectException.getID(hierarchy.entryOf(this)) + " must be settable if its override (" + UnregisteredObjectException.getID(this.override) + ") is settable.");
		}
		this.owner(hierarchy).overrideTracker.addOverrideProperty(this);
	}

	@Override
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		super.compile(hierarchy);
		compile(hierarchy, this.context.get, this.get, load("this", this.owner(hierarchy).getTypeInfo()), this.dependencies, NO_EXTRAS);
		if (this.set != null) compile(hierarchy,  this.context.set, this.set, load("this", this.owner(hierarchy).getTypeInfo()), this.dependencies, (MutableScriptEnvironment environment) -> {
			environment.addVariableLoad("value", this.getPropertyTypeSpec(hierarchy).getTypeInfo());
		});
	}

	@Override
	public boolean isSettable() {
		return this.set != null;
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
	public void setupEnvironment(Holder<ElementSpec> self, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		//no-op. base method can be called as-is.
	}

	@Override
	public String name() {
		return this.override.value().name();
	}
}