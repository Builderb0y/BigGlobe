package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static org.objectweb.asm.Opcodes.*;

public class OverridePropertySpec extends BasePropertySpec {

	public final RegistryEntry<ElementSpec> override;
	public final ScriptUsage get;
	public final @VerifyNullable ScriptUsage set;

	public OverridePropertySpec(
		RegistryEntry<ElementSpec> override,
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
		compile(hierarchy, owner, propertyContext.get, this.get, this);
		if (this.set != null) compile(hierarchy, owner, propertyContext.set, this.set, this);
	}

	@Override
	public boolean isSettable() {
		return this.set != null;
	}

	@Override
	public RegistryEntry<ElementSpec> getPropertyType() {
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
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return ((BasePropertySpec)(this.override.value())).getDependencies();
	}
}