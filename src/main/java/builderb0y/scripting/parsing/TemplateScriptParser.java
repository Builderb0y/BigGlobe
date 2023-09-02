package builderb0y.scripting.parsing;

import java.util.Collections;

import org.objectweb.asm.tree.MethodNode;

import builderb0y.bigglobe.features.ScriptedFeature.FeatureScript;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptTemplate.RequiredInput;
import builderb0y.scripting.util.ArrayBuilder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class TemplateScriptParser<I> extends ScriptParser<I> {

	public final GenericScriptTemplateUsage usage;

	public TemplateScriptParser(Class<I> implementingClass, GenericScriptTemplateUsage usage) {
		super(implementingClass, usage.getEntry().value().getSource());
		this.usage = usage;
	}

	public static <I> ScriptParser<I> createFrom(Class<I> implementingClass, ScriptUsage<GenericScriptTemplateUsage> usage) {
		return (
			usage.isScript()
			? new ScriptParser<>(implementingClass, usage.getScript())
			: new TemplateScriptParser<>(implementingClass, usage.getTemplate())
		);
	}

	@Override
	public InsnTree parseEntireInput() throws ScriptParsingException {
		this.usage.validateInputs(message -> new ScriptParsingException(message.get(), null));
		ArrayBuilder<InsnTree> initializers = new ArrayBuilder<>();
		for (RequiredInput input : this.usage.actualTemplate.getRequiredInputs()) {
			String inputSource = this.usage.getProvidedInputs().get(input.name());
			assert inputSource != null;
			ClassCompileContext classCopy = new ClassCompileContext(this.clazz.node.access, this.clazz.info);
			MethodCompileContext methodCopy = new MethodCompileContext(classCopy, new MethodNode(), this.method.info);
			ExpressionParser parserCopy = new ExpressionParser(inputSource, classCopy, methodCopy);
			parserCopy.environment.mutable(new MutableScriptEnvironment().addAll(this.environment.mutable()));
			FunctionHandler handler = new FunctionHandler.Named("invalid", (parser, name, arguments) -> {
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
}