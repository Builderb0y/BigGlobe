package builderb0y.bigglobe.columns.scripted.classes;

import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;

public abstract class MemberSpec extends ElementSpec {

	public abstract void track(OverrideTracker tracker) throws CustomClassFormatException;

	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {}

	public void create(ClassHierarchy hierarchy, BaseClassSpec owner) {}

	public void compile(ClassHierarchy hierarchy, BaseClassSpec owner) throws ScriptParsingException {}

	public abstract void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, ClassCompileContext caller);
}