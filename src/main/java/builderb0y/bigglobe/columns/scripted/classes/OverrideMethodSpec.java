package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Set;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static org.objectweb.asm.Opcodes.*;

public class OverrideMethodSpec extends BaseMethodSpec {

	public final RegistryEntry<ElementSpec> override;
	public final ScriptUsage code;

	public OverrideMethodSpec(
		RegistryEntry<ElementSpec> override,
		ScriptUsage code
	) {
		this.override = override;
		this.code = code;
	}

	@Override
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return ((BaseMethodSpec)(this.override.value())).getDependencies();
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addOverrideMethod(this);
	}

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		if (!(this.override.value() instanceof BaseMethodSpec)) {
			throw new CustomClassFormatException("Override method " + this.override.value().name() + " overrides non-method " + UnregisteredObjectException.getID(this.override));
		}
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, ClassCompileContext caller) {
		//no-op. base method can be called as-is.
	}

	@Override
	public void compile(ClassHierarchy hierarchy, BaseClassSpec clazz) throws ScriptParsingException {
		this.compile(hierarchy, clazz, this.code);
	}

	@Override
	public RegistryEntry<ElementSpec> getReturnType() {
		return ((BaseMethodSpec)(this.override.value())).getReturnType();
	}

	@Override
	public ParameterSpec[] getParameters() {
		return ((BaseMethodSpec)(this.override.value())).getParameters();
	}

	@Override
	public int flags() {
		return ACC_PUBLIC;
	}

	@Override
	public String name() {
		return this.override.value().name();
	}
}