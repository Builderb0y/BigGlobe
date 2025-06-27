package builderb0y.bigglobe.columns.scripted.classes;

import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;
import static builderb0y.scripting.bytecode.InsnTrees.load;

public abstract class MemberSpec extends ElementSpec {

	public abstract void track(OverrideTracker tracker) throws CustomClassFormatException;

	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {}

	public void create(ClassHierarchy hierarchy, BaseClassSpec owner) {}

	public void compile(ClassHierarchy hierarchy, BaseClassSpec owner) throws ScriptParsingException {}

	public static void compile(
		ClassHierarchy hierarchy,
		BaseClassSpec owner,
		MethodCompileContext methodContext,
		ScriptUsage code,
		MutableDependencyView dependencies
	)
	throws ScriptParsingException {
		new ScriptColumnEntryParser(code, methodContext.clazz, methodContext, hierarchy.registry.parserFlags())
		.configureEnvironment(JavaUtilScriptEnvironment.withoutRandom())
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.configureEnvironment(MinecraftScriptEnvironment.create())
		.addEnvironment(SymmetryScriptEnvironment.INSTANCE)
		.configureEnvironment(NbtScriptEnvironment.createMutable())
		.addEnvironment(WoodPaletteScriptEnvironment.BASE)
		.addEnvironment(RandomScriptEnvironment.BASE)
		.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
		.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
		.configureEnvironment(
			hierarchy.registry.externalEnvironmentSetterUpper(
				new ExternalEnvironmentParams()
				.withColumn(
					new DirectCastInsnTree(
						getField(
							load("this", owner.classCompileContext.info),
							owner.baseColumnField()
						),
						hierarchy.registry.columnContext.columnType(),
						false
					)
				)
				.trackDependencies(dependencies)
			)
		)
		.configureEnvironment((MutableScriptEnvironment environment) -> {
			hierarchy.setupEnvironment(environment, owner.classCompileContext);
		})
		.parseEntireInput()
		.emitBytecode(methodContext);
		methodContext.endCode();
	}

	public abstract void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, ClassCompileContext caller);

	@Override
	public abstract String toString();
}