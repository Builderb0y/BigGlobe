package builderb0y.bigglobe.columns.scripted.classes;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class MemberSpec extends ElementSpec {

	public abstract void track(OverrideTracker tracker) throws CustomClassFormatException;

	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {}

	public void create(ClassHierarchy hierarchy, BaseClassSpec owner) {}

	public void compile(ClassHierarchy hierarchy, BaseClassSpec owner) throws ScriptParsingException {}

	public static final Consumer<MutableScriptEnvironment> NO_EXTRAS = (MutableScriptEnvironment environment) -> {};

	public static void compile(
		ClassHierarchy hierarchy,
		BaseClassSpec owner,
		MethodCompileContext methodContext,
		ScriptUsage code,
		InsnTree loadY,
		MutableDependencyView dependencies,
		Consumer<MutableScriptEnvironment> extra
	)
		throws ScriptParsingException {
		hierarchy.registry.setMethodCode(
			methodContext,
			code,
			null,
			/*
			new DirectCastInsnTree(
				getField(
					load("this", owner.getTypeInfo()),
					owner.baseColumnField()
				),
				hierarchy.registry.columnCompileContext.columnTypeInfo(),
				false
			),
			*/
			loadY,
			load("this", owner.getTypeInfo()),
			dependencies,
			extra
		);
	}

	public abstract void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, @Nullable InsnTree loadCustomClass);

	@Override
	public abstract String toString();
}