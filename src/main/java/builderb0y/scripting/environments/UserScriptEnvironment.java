package builderb0y.scripting.environments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment2.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment2.MethodHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment2.NamedType;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.StackMap;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class UserScriptEnvironment implements ScriptEnvironment {

	public StackMap<String, LoadInsnTree> variables;
	public StackMap<NamedType, FieldInfo> fields;
	public StackMap<String, List<FunctionHandler>> functions;
	public StackMap<NamedType, List<MethodHandler>> methods;
	public StackMap<String, TypeInfo> types;

	public UserScriptEnvironment() {
		this.variables = new StackMap<>(8);
		this.fields    = new StackMap<>(8);
		this.functions = new StackMap<>(4);
		this.methods   = new StackMap<>(4);
		this.types     = new StackMap<>(4);
	}

	public UserScriptEnvironment(UserScriptEnvironment from) {
		this.variables = new StackMap<>(from.variables);
		this.fields    = new StackMap<>(from.fields);
		this.functions = new StackMap<>(from.functions);
		this.methods   = new StackMap<>(from.methods);
		this.types     = new StackMap<>(from.types);
	}

	public void addFunction(String name, FunctionHandler functionHandler) {
		this.functions.computeIfAbsent(name, $ -> new ArrayList<>(4)).add(functionHandler);
	}

	public void addField(String name, FieldInfo field) {
		this.fields.put(new NamedType(field.owner, name), field);
	}

	public void addField(FieldInfo field) {
		this.addField(field.name, field);
	}

	public void addClassFunction(String name, MethodHandler methodHandler) {
		this.methods.computeIfAbsent(new NamedType(TypeInfos.CLASS, name), $ -> new ArrayList<>(4)).add(methodHandler);
	}

	public void addClassFunction(TypeInfo type, String name, MethodInfo method) {
		this.addClassFunction(name, (parser, receiver, name1, arguments) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(type)) {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
				if (castArguments != null) return invokeStatic(method, castArguments);
			}
			return null;
		});
	}

	public void addConstructor(TypeInfo type, MethodInfo method) {
		this.addClassFunction("new", (parser, receiver, name1, arguments) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(type)) {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
				if (castArguments != null) return newInstance(method, castArguments);
			}
			return null;
		});
	}

	//////////////////////////////// getters ////////////////////////////////

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		return this.variables.get(name);
	}

	@Override
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		NamedType query = new NamedType();
		query.name = name;
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			query.owner = owner;
			FieldInfo field = this.fields.get(query);
			if (field != null) return InsnTrees.getField(receiver, field);
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		List<FunctionHandler> handlers = this.functions.get(name);
		if (handlers != null) for (int index = 0, size = handlers.size(); index < size; index++) {
			InsnTree tree = handlers.get(index).create(parser, name, arguments);
			if (tree != null) return tree;
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		NamedType query = new NamedType();
		query.name = name;
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			query.owner = owner;
			List<MethodHandler> handlers = this.methods.get(query);
			if (handlers != null) for (int index = 0, size = handlers.size(); index < size; index++) {
				InsnTree tree = handlers.get(index).create(parser, receiver, name, arguments);
				if (tree != null) return tree;
			}
		}
		return null;
	}

	@Override
	public @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		return this.types.get(name);
	}

	public boolean hasNewVariables() {
		return this.variables.hasNewElements();
	}

	public VariableDeclarationInsnTree newVariable(String name, TypeInfo type) {
		VariableDeclarationInsnTree tree = new VariableDeclarationInsnTree(name, type);
		this.variables.put(name, tree.loader);
		return tree;
	}

	public Stream<VarInfo> streamVariables() {
		return this.variables.values().stream().map(load -> load.variable);
	}

	public Iterable<VarInfo> getVariables() {
		return this.streamVariables()::iterator;
	}

	public void push() {
		this.variables.push();
		this.fields   .push();
		this.functions.push();
		this.methods  .push();
		this.types    .push();
	}

	public void pop() {
		this.variables.pop();
		this.fields   .pop();
		this.functions.pop();
		this.methods  .pop();
		this.types    .pop();
	}

	public boolean isFullyPopped() {
		return this.variables.sizes.isEmpty();
	}

	public int getStackSize() {
		return this.variables.sizes.size();
	}
}