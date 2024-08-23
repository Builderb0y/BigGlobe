package builderb0y.scripting.parsing.special;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarePostAssignInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record MultiDeclarationSyntax(@Nullable TypeInfo type, VariableInitializer[] variables, boolean returnLast) {

	public static record VariableInitializer(String name, InsnTree initializer) {

	}

	public static MultiDeclarationSyntax parse(ExpressionParser parser, @Nullable TypeInfo type) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		if (parser.input.hasAfterWhitespace(')')) {
			return new MultiDeclarationSyntax(type, new VariableInitializer[0], false);
		}
		List<VariableInitializer> variables = new ArrayList<>();
		while (true) {
			String name = parser.verifyName(parser.input.expectIdentifierAfterWhitespace(), "variable");
			parser.checkVariable(name);
			InsnTree initializer;
			boolean returning;
			if (type != null) {
				parser.environment.user().reserveVariable(name, type);
				if (parser.input.hasOperatorAfterWhitespace("=")) returning = false;
				else if (parser.input.hasOperatorAfterWhitespace(":=")) returning = true;
				else throw new ScriptParsingException("Expected '=' or ':='", parser.input);
				initializer = parser.nextVariableInitializer(type, true);
				parser.environment.user().assignVariable(name);
			}
			else {
				parser.environment.user().reserveVariable(name);
				if (parser.input.hasOperatorAfterWhitespace("=")) returning = false;
				else if (parser.input.hasOperatorAfterWhitespace(":=")) returning = true;
				else throw new ScriptParsingException("Expected '=' or ':='", parser.input);
				initializer = parser.nextSingleExpression();
				if (initializer.getTypeInfo().getSort() == Sort.VOID) {
					throw new ScriptParsingException("void-typed variables are not allowed.", parser.input);
				}
				parser.environment.user().setVariableType(name, initializer.getTypeInfo());
				parser.environment.user().assignVariable(name);
			}
			variables.add(new VariableInitializer(name, initializer));
			if (parser.input.hasAfterWhitespace(')')) {
				return new MultiDeclarationSyntax(type, variables.toArray(new VariableInitializer[variables.size()]), returning);
			}
			parser.input.hasOperatorAfterWhitespace(",");
		}
	}

	public InsnTree[] trees() {
		VariableInitializer[] initializers = this.variables;
		int variableCount = initializers.length;
		if (variableCount == 0) return InsnTree.ARRAY_FACTORY.empty();
		InsnTree[] trees = new InsnTree[variableCount];
		for (int index = 0; index < variableCount; index++) {
			VariableInitializer initializer = initializers[index];
			TypeInfo type = this.type != null ? this.type : initializer.initializer.getTypeInfo();
			trees[index] = (
				index == variableCount - 1 && this.returnLast
					? new VariableDeclarePostAssignInsnTree(new LazyVarInfo(initializer.name, type), initializer.initializer)
					: new VariableDeclareAssignInsnTree(new LazyVarInfo(initializer.name, type), initializer.initializer)
			);
		}
		return trees;
	}

	public InsnTree sequence() {
		InsnTree[] trees = this.trees();
		return trees.length == 0 ? noop : seq(trees);
	}
}