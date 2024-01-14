package builderb0y.bigglobe.columns.scripted;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.objectweb.asm.tree.MethodNode;

import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptTemplate.RequiredInput;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.ArrayBuilder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptColumnEntryParser extends ExpressionParser {

	public final ScriptUsage<GenericScriptTemplateUsage> usage;

	public ScriptColumnEntryParser(ScriptUsage<GenericScriptTemplateUsage> usage, ClassCompileContext clazz, MethodCompileContext method) {
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

	/** note: this logic should match {@link TemplateScriptParser#parseEntireInput()}. */
	@Override
	public InsnTree parseEntireInput() throws ScriptParsingException {
		if (this.usage.isTemplate()) {
			GenericScriptTemplateUsage genericUsage = this.usage.getTemplate();
			genericUsage.validateInputs((Supplier<String> message) -> new ScriptParsingException(message.get(), null));
			ArrayBuilder<InsnTree> initializers = new ArrayBuilder<>();
			for (RequiredInput input : genericUsage.actualTemplate.getRequiredInputs()) {
				String inputSource = genericUsage.getProvidedInputs().get(input.name());
				assert inputSource != null;
				ClassCompileContext classCopy = new ClassCompileContext(this.clazz.node.access, this.clazz.info);
				MethodCompileContext methodCopy = new MethodCompileContext(classCopy, new MethodNode(), this.method.info);
				ExpressionParser parserCopy = new ExpressionParser(inputSource, classCopy, methodCopy);
				parserCopy.environment.mutable(new MutableScriptEnvironment().addAll(this.environment.mutable()));
				FunctionHandler handler = new FunctionHandler.Named("invalid", (ExpressionParser parser, String name, InsnTree... arguments) -> {
					throw new ScriptParsingException(name + " is not allowed in script inputs", parser.input);
				});
				parserCopy.environment.mutable().functions.put("return", Collections.singletonList(handler));
				TypeInfo type = parserCopy.environment.getType(this, input.type());
				if (type == null) {
					throw new ScriptParsingException("Unknown type: " + input.type(), null);
				}
				InsnTree inputTree = parserCopy.nextScript().cast(parserCopy, type, CastMode.IMPLICIT_THROW);
				VarInfo declaration = this.environment.user().newVariable(input.name(), type);
				InsnTree initializer = new VariableDeclareAssignInsnTree(declaration, inputTree);
				this.environment.mutable()
					.addVariable(input.name(), load(declaration))
					.addVariable('$' + input.name(), inputTree);
				initializers.add(initializer);
			}
			initializers.add(super.parseEntireInput());
			return seq(initializers.toArray(InsnTree.ARRAY_FACTORY));
		}
		else {
			return super.parseEntireInput();
		}
	}
}