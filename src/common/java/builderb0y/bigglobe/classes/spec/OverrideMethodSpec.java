package builderb0y.bigglobe.classes.spec;

import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class OverrideMethodSpec extends BaseMethodSpec {

	public final Holder<ElementSpec> override;

	public BaseMethodSpec override(ClassHierarchy hierarchy) {
		return requireType(this.override, BaseMethodSpec.class, () -> hierarchy.idOf(this) + " > override");
	}

	@Override
	public TypeSpec returnTypeSpec(ClassHierarchy hierarchy) {
		return this.override(hierarchy).returnTypeSpec(hierarchy);
	}

	public final ScriptUsage code;

	public OverrideMethodSpec(
		Holder<ElementSpec> owner,
		Holder<ElementSpec> override,
		ScriptUsage code
	) {
		super(owner);
		this.override = override;
		this.code = code;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(super.streamDirectDependencies(), Stream.of(this.override));
	}

	@Override
	@MustBeInvokedByOverriders
	public void reference(ClassHierarchy hierarchy) throws DetailedException {
		super.reference(hierarchy);
		this.override(hierarchy).overrides.add(hierarchy.entryOf(this));
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		this.override(hierarchy);
		this.owner(hierarchy).overrideTracker.addOverrideMethod(this);
	}

	@Override
	@MustBeInvokedByOverriders
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		super.compile(hierarchy);
		this.compile(hierarchy, this.code, load("this", this.owner(hierarchy).getTypeInfo()), (ExpressionParser parser) -> {
			for (ParameterSpec parameter : this.getParameters()) {
				if (parameter.import_) {
					//will automatically add variable to MutableScriptEnvironment.
					parser.addImportedValue(parameter.name, load(parameter.name, parameter.typeInfo()));
				}
				else {
					parser.environment.mutable().addVariableLoad(parameter.name, parameter.typeInfo());
				}
			}
		});
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, ExpressionParser parser, ExternalEnvironmentParams params) {
		//no-op. base method can be called as-is.
	}

	@Override
	public Holder<ElementSpec> getReturnType() {
		return ((BaseMethodSpec)(this.override.value())).getReturnType();
	}

	@Override
	public ParameterSpec[] getParameters() {
		return ((BaseMethodSpec)(this.override.value())).getParameters();
	}

	@Override
	public int accessFlags() {
		return ACC_PUBLIC;
	}

	@Override
	public String name() {
		return this.override.value().name();
	}
}