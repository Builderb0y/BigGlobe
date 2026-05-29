package builderb0y.bigglobe.classes.spec;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

public abstract class MemberSpec extends ElementSpec {

	public final Holder<ElementSpec> owner;
	public BaseClassSpec owner(ClassHierarchy hierarchy) {
		return requireType(this.owner, BaseClassSpec.class, () -> hierarchy.idOf(this) + " > owner");
	}

	public MemberSpec(Holder<ElementSpec> owner) {
		this.owner = owner;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.of(this.owner);
	}

	@Override
	@MustBeInvokedByOverriders
	public void reference(ClassHierarchy hierarchy) throws DetailedException {
		super.reference(hierarchy);
		this.owner(hierarchy).members.add(hierarchy.entryOf(this));
	}

	public static final Consumer<MutableScriptEnvironment> NO_EXTRAS = (MutableScriptEnvironment environment) -> {};

	public static void compile(
		ClassHierarchy hierarchy,
		MethodCompileContext methodContext,
		ScriptUsage code,
		InsnTree loadCustomClass,
		MutableDependencyView dependencies,
		Consumer<MutableScriptEnvironment> extra
	)
	throws ScriptParsingException {
		hierarchy.registry.setMethodCode(
			methodContext,
			code,
			null,
			null,
			loadCustomClass,
			null,
			dependencies,
			extra
		);
	}

	@Override
	public abstract String toString();
}