package builderb0y.scripting.bytecode;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.StackMap;

public class UserDefinitions {

	public StackMap<String, VarInfo> variables = new StackMap<>();
	public StackMap<String, FunctionHandler> functions = new StackMap<>();
	public StackMap<String, TypeInfo> classes = new StackMap<>();
	public int anonymousCounter;

	public void pushScope() {
		this.variables.push();
		this.functions.push();
		this.classes.push();
	}

	public void popScope() {
		this.variables.pop();
		this.functions.pop();
		this.classes.pop();
	}

	public boolean isEmpty() {
		return this.variables.sizes.isEmpty();
	}

	/**
	returns true if any new variables were added via {@link #newVariable(String, TypeInfo)}
	or {@link #newAnonymousVariable(TypeInfo)} since the last cll to {@link #pushScope()}.
	*/
	public boolean hasNewVariables() {
		return this.variables.hasNewElements();
	}

	public VariableDeclarationInsnTree newVariable(String name, TypeInfo type) {
		VariableDeclarationInsnTree tree = new VariableDeclarationInsnTree(name, type);
		this.variables.putUnique(name, tree.loader.variable);
		return tree;
	}

	public VariableDeclarationInsnTree newAnonymousVariable(TypeInfo type) {
		return this.newVariable("$" + this.anonymousCounter++, type);
	}

	public @Nullable InsnTree getVariable(String name) {
		VarInfo info = this.variables.get(name);
		return info == null ? null : new LoadInsnTree(info);
	}

	public @Nullable InsnTree getMethod(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		FunctionHandler handler = this.functions.get(name);
		return handler == null ? null : handler.createFunction(parser, name, arguments);
	}
}