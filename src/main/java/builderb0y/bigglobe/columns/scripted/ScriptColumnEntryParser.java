package builderb0y.bigglobe.columns.scripted;

import java.util.function.Consumer;
import java.util.function.Supplier;

import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.ArrayBuilder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptColumnEntryParser extends ExpressionParser {

	public final ScriptUsage usage;

	public ScriptColumnEntryParser(ScriptUsage usage, ClassCompileContext clazz, MethodCompileContext method) {
		super(usage.findSource(), clazz, method);
		this.usage = usage;
	}

	@Override
	public ScriptColumnEntryParser addEnvironment(ScriptEnvironment environment) {
		return (ScriptColumnEntryParser)(super.addEnvironment(environment));
	}

	@Override
	public ScriptColumnEntryParser configureEnvironment(Consumer<MutableScriptEnvironment> configurator) {
		return (ScriptColumnEntryParser)(super.configureEnvironment(configurator));
	}

	@Override
	public InsnTree parseEntireInput() throws ScriptParsingException {
		if (this.usage.isTemplate()) {
			ArrayBuilder<InsnTree> initializers = TemplateScriptParser.parseInitializers(this, this.usage);
			initializers.add(super.parseEntireInput());
			return seq(initializers.toArray(InsnTree.ARRAY_FACTORY));
		}
		else {
			return super.parseEntireInput();
		}
	}
}