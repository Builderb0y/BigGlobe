package builderb0y.scripting.environments;

import java.util.stream.Stream;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.util.StackMap;

public class UserScriptEnvironment extends MutableScriptEnvironment {

	public UserScriptEnvironment() {
		super(
			new StackMap<>(16),
			new StackMap<>(8),
			new StackMap<>(8),
			new StackMap<>(8),
			new StackMap<>(8),
			new StackMap<>(4)
		);
	}

	public UserScriptEnvironment(UserScriptEnvironment from) {
		super(
			new StackMap<>(from.variables()),
			new StackMap<>(from.fields()),
			new StackMap<>(from.functions()),
			new StackMap<>(from.methods()),
			new StackMap<>(from.classFunctions()),
			new StackMap<>(from.types())
		);
	}

	public StackMap<String, InsnTree> variables() {
		return ((StackMap<String, InsnTree>)(this.variables));
	}

	public StackMap<NamedType, FieldInfo> fields() {
		return ((StackMap<NamedType, FieldInfo>)(this.fields));
	}

	public StackMap<String, FunctionHandler> functions() {
		return ((StackMap<String, FunctionHandler>)(this.functions));
	}

	public StackMap<NamedType, MethodHandler> methods() {
		return ((StackMap<NamedType, MethodHandler>)(this.methods));
	}

	public StackMap<NamedType, FunctionHandler> classFunctions() {
		return ((StackMap<NamedType, FunctionHandler>)(this.classFunctions));
	}

	public StackMap<String, TypeInfo> types() {
		return ((StackMap<String, TypeInfo>)(this.types));
	}

	public boolean hasNewVariables() {
		return this.variables().hasNewElements();
	}

	public VariableDeclarationInsnTree newVariable(String name, TypeInfo type) {
		VariableDeclarationInsnTree tree = new VariableDeclarationInsnTree(name, type);
		this.variables().putUnique(name, tree.loader);
		return tree;
	}

	public Stream<VarInfo> streamVariables() {
		return this.variables.values().stream().filter(LoadInsnTree.class::isInstance).map(LoadInsnTree.class::cast).map(load -> load.variable);
	}

	public Iterable<VarInfo> getVariables() {
		return this.streamVariables()::iterator;
	}

	public void push() {
		this.variables().push();
		this.fields   ().push();
		this.functions().push();
		this.methods  ().push();
		this.types    ().push();
	}

	public void pop() {
		this.variables().pop();
		this.fields   ().pop();
		this.functions().pop();
		this.methods  ().pop();
		this.types    ().pop();
	}

	public boolean isFullyPopped() {
		return this.variables().sizes.isEmpty();
	}

	public int getStackSize() {
		return this.variables().sizes.size();
	}
}