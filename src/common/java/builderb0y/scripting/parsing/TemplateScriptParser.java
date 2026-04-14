package builderb0y.scripting.parsing;

import java.util.*;
import java.util.function.Consumer;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.input.ScriptTemplate.RequiredInput;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.ArrayBuilder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class TemplateScriptParser<I> extends ScriptParser<I> {

	public final ScriptUsage usage;

	public TemplateScriptParser(Class<I> implementingClass, ScriptUsage usage, int flags) {
		super(implementingClass, usage.getSource(), usage.debug_name, flags);
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
		if (this.usage.getTemplate() != null) {
			ArrayBuilder<InsnTree> initializers = parseInitializers(this, this.usage);
			initializers.add(super.parseEntireInput());
			return seq(initializers.toArray(InsnTree.ARRAY_FACTORY));
		}
		else {
			return super.parseEntireInput();
		}
	}

	public static void checkInputs(ScriptUsage usage) throws ScriptParsingException {
		List<RequiredInput> requiredInputs = usage.getTemplate().value().inputs;
		Map<String, String> providedInputs = usage.getInputs();
		if (
			(requiredInputs != null && !requiredInputs.isEmpty()) ||
			(providedInputs != null && !providedInputs.isEmpty())
		) {
			if (requiredInputs == null) requiredInputs = Collections.emptyList();
			if (providedInputs == null) providedInputs = Collections.emptyMap();
			providedInputs = new HashMap<>(providedInputs);
			Set<String> missingInputs = null;
			for (RequiredInput requiredInput : requiredInputs) {
				if (providedInputs.remove(requiredInput.name()) == null && requiredInput.fallback() == null) {
					if (missingInputs == null) missingInputs = new HashSet<>(4);
					missingInputs.add(requiredInput.name());
				}
			}
			if (missingInputs != null || !providedInputs.isEmpty()) {
				//did I put too much effort into this error message?
				//it's longer than the verification algorithm, so... probably.
				Set<String> unknownInputs = providedInputs.keySet();
				StringBuilder message = new StringBuilder(64);
				if (missingInputs != null) {
					if (missingInputs.size() == 1) {
						message.append("Missing input: ").append(missingInputs.iterator().next());
					}
					else {
						message.append("Missing inputs: ").append(missingInputs);
					}
				}
				if (!unknownInputs.isEmpty()) {
					if (!message.isEmpty()) message.append("; ");
					if (unknownInputs.size() == 1) {
						message.append("Unknown input: ").append(unknownInputs.iterator().next());
					}
					else {
						message.append("Unknown inputs: ").append(unknownInputs);
					}
				}
				throw new ScriptParsingException(message.toString(), null);
			}
		}
	}

	public static ArrayBuilder<InsnTree> parseInitializers(ExpressionParser parser, ScriptUsage usage) throws ScriptParsingException {
		checkInputs(usage);
		ArrayBuilder<InsnTree> initializers = new ArrayBuilder<>();
		for (RequiredInput input : usage.getTemplate().value().getInputs()) {
			String inputSource = usage.getInputs().getOrDefault(input.name(), input.fallback());
			if (inputSource == null) {
				throw new ScriptParsingException("Missing input " + input.name(), null);
			}
			TypeInfo type = parser.environment.getType(parser, input.type());
			if (type == null) {
				throw new ScriptParsingException("Unknown type: " + input.type(), null);
			}
			ExpressionParser parserCopy = new InnerMethodExpressionParser(parser, inputSource, type);
			FunctionHandler.Named handler = new FunctionHandler.Named("invalid", (ExpressionParser parser_, String name, InsnTree... arguments) -> {
				throw new ScriptParsingException(name + " is not allowed in script inputs", parser_.input);
			});
			parserCopy.environment.mutable().functions.put("return", Collections.singletonList(handler));
			parser.environment.user().reserveVariable(input.name(), type);
			InsnTree inputTree = parserCopy.nextScript().cast(parserCopy, type, CastMode.IMPLICIT_THROW, false);
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