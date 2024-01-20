package builderb0y.scripting.parsing;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.objectweb.asm.tree.MethodNode;

import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptTemplate.RequiredInput;
import builderb0y.scripting.parsing.ScriptTemplate.ScriptTemplateUsage;
import builderb0y.scripting.util.ArrayBuilder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class TemplateScriptParser<I> extends ScriptParser<I> {

	public final ScriptUsage<GenericScriptTemplateUsage> usage;

	public TemplateScriptParser(Class<I> implementingClass, ScriptUsage<GenericScriptTemplateUsage> usage) {
		super(implementingClass, usage.findSource(), usage.debug_name);
		this.usage = usage;
	}

	@Override
	public TemplateScriptParser<I> addEnvironment(ScriptEnvironment environment) {
		return (TemplateScriptParser<I>)(super.addEnvironment(environment));
	}

	@Override
	public TemplateScriptParser<I> configureEnvironment(Consumer<MutableScriptEnvironment> configurator) {
		return (TemplateScriptParser<I>)(super.configureEnvironment(configurator));
	}

	@Override
	public InsnTree parseEntireInput() throws ScriptParsingException {
		if (this.usage.isTemplate()) {
			GenericScriptTemplateUsage genericUsage = this.usage.getTemplate();
			genericUsage.validateInputs((Supplier<String> message) -> new ScriptParsingException(message.get(), null));
			ArrayBuilder<InsnTree> initializers = parseInitializers(this, genericUsage);
			initializers.add(super.parseEntireInput());
			return seq(initializers.toArray(InsnTree.ARRAY_FACTORY));
		}
		else {
			return super.parseEntireInput();
		}
	}

	public static ArrayBuilder<InsnTree> parseInitializers(ExpressionParser parser, ScriptTemplateUsage usage) throws ScriptParsingException {
		ArrayBuilder<InsnTree> initializers = new ArrayBuilder<>();
		for (RequiredInput input : usage.getActualTemplate().getRequiredInputs()) {
			String inputSource = usage.getProvidedInputs().get(input.name());
			assert inputSource != null;
			TypeInfo type = parser.environment.getType(parser, input.type());
			if (type == null) {
				throw new ScriptParsingException("Unknown type: " + input.type(), null);
			}
			ExpressionParser parserCopy = new InnerMethodExpressionParser(parser, inputSource, type);
			FunctionHandler handler = new FunctionHandler.Named("invalid", (ExpressionParser parser_, String name, InsnTree... arguments) -> {
				throw new ScriptParsingException(name + " is not allowed in script inputs", parser_.input);
			});
			parserCopy.environment.mutable().functions.put("return", Collections.singletonList(handler));
			parser.environment.user().reserveVariable(input.name(), type);
			InsnTree inputTree = parserCopy.nextScript().cast(parserCopy, type, CastMode.IMPLICIT_THROW);
			parser.environment.user().assignVariable(input.name());
			LazyVarInfo declaration = new LazyVarInfo(input.name(), type);
			InsnTree initializer = new VariableDeclareAssignInsnTree(declaration, inputTree);
			parser.environment.mutable()
			.addVariable(input.name(), load(declaration))
			.addVariable('$' + input.name(), inputTree);
			initializers.add(initializer);
		}
		return initializers;
	}
}