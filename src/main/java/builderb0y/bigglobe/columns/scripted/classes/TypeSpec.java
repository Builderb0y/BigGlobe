package builderb0y.bigglobe.columns.scripted.classes;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.data.Data;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;

public abstract class TypeSpec extends ElementSpec {

	public static final Path DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bigglobe.dumpCustomClasses", "bigglobe_custom_classes");

	public CompileStep compileState = CompileStep.FRESH;

	public boolean canProgressTo(CompileStep state) {
		if (this.compileState.ordinal() < state.ordinal() - 1) {
			throw new IllegalStateException("Skipped a step!");
		}
		else if (this.compileState.ordinal() == state.ordinal() - 1) {
			this.compileState = state;
			return true;
		}
		else if (this.compileState.ordinal() == state.ordinal()) {
			return false;
		}
		else {
			throw new IllegalStateException("Progressing backwards!");
		}
	}

	public void doProgressTo(CompileStep state, ClassHierarchy hierarchy) throws Exception {
		if (this.canProgressTo(state)) {
			state.action.execute(this, hierarchy);
		}
	}

	public abstract InsnTree parseConstant(ClassHierarchy hierarchy, Data data, InsnTree loadColumn);

	public abstract TypeInfo getTypeInfo();

	public abstract boolean isFinal();

	public abstract boolean isAbstract();

	public abstract @Nullable OverrideTracker getOverrideTracker();

	public void createTypeInfo(ClassHierarchy hierarchy, LinkedHashSet<RegistryEntry<ElementSpec>> cyclicDetector) throws CustomClassFormatException {}

	public void verify(ClassHierarchy hierarchy) throws CustomClassFormatException {}

	public void createClass(ClassHierarchy hierarchy) {}

	public void createMembers(ClassHierarchy hierarchy) {}

	public void compileMembers(ClassHierarchy hierarchy) throws ScriptParsingException {}

	public void link(ScriptClassLoader loader) {}

	public void setupEnvironment(MutableScriptEnvironment environment, @Nullable InsnTree loadCustomClass) {
		environment.addType(this.name(), this.getTypeInfo());
	}

	public static enum CompileStep {
		FRESH("doing nothing", null),
		CREATE_TYPE_INFO("creating type info", (TypeSpec type, ClassHierarchy hierarchy) -> type.createTypeInfo(hierarchy, new LinkedHashSet<>())),
		VERIFY("verifying", TypeSpec::verify),
		CREATE_CLASS("creating class", TypeSpec::createClass),
		CREATE_MEMBERS("creating members", TypeSpec::createMembers),
		COMPILE_MEMBERS("compiling members", TypeSpec::compileMembers);

		public static final CompileStep[]
			VALUES = values(),
			EXCEPT_FRESH = Arrays.copyOfRange(VALUES, 1, VALUES.length);

		public final String description;
		public final CompileAction action;

		CompileStep(String description, CompileAction action) {
			this.description = description;
			this.action = action;
		}

		@FunctionalInterface
		public static interface CompileAction {

			public abstract void execute(TypeSpec type, ClassHierarchy hierarchy) throws Exception;
		}
	}
}