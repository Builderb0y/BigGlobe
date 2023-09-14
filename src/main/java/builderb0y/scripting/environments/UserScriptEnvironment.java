package builderb0y.scripting.environments;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.NamedType;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.StackMap;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class UserScriptEnvironment implements ScriptEnvironment {

	public ExpressionParser parser;
	public StackMap<String,         LoadInsnTree    > variables;
	public StackMap<NamedType,      FieldInfo       > fields;
	public StackMap<String,    List<FunctionHandler>> functions;
	public StackMap<NamedType, List<MethodHandler  >> methods;
	public StackMap<String,         TypeInfo        > types;

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

	@Override
	public Stream<String> listCandidates(String name) {
		return Stream.of(
			Stream.ofNullable(this.variables.get(name))
			.map(variable -> "Variable " + name + ": " + variable.variable),

			this.fields.entrySet().stream()
			.filter(entry -> Objects.equals(entry.getKey().name, name))
			.map(entry -> "Field " + entry.getKey() + ": " + entry.getValue()),

			Stream.ofNullable(this.functions.get(name))
			.flatMap(List::stream)
			.map(function -> "Function " + name + ": " + function),

			this.methods.entrySet().stream()
			.filter(entry -> Objects.equals(entry.getKey().name, name))
			.flatMap(entry -> entry.getValue().stream().map(handler -> Map.entry(entry.getKey(), handler)))
			.map(entry -> "Method " + entry.getKey() + ": " + entry.getValue()),

			Stream.ofNullable(this.types.get(name))
			.map(type -> "Type " + name + ": " + type)
		)
		.flatMap(Function.identity());
	}

	public void addFunction(String name, FunctionHandler functionHandler) {
		this.functions.computeIfAbsent(name, $ -> new ArrayList<>(4)).add(functionHandler);
	}

	public void addFieldGetterAndSetter(FieldInfo field) {
		NamedType namedType = new NamedType(field.owner, field.name);
		this.fields.put(namedType, field);
		List<MethodHandler> handlers = this.methods.computeIfAbsent(namedType, $ -> new ArrayList<>(2));
		MethodInfo getter = new MethodInfo(ACC_PUBLIC, field.owner, field.name, field.type);
		MethodInfo setter = new MethodInfo(ACC_PUBLIC, field.owner, field.name, TypeInfos.VOID, field.type);
		handlers.add(
			(parser, receiver, name, mode, arguments) -> {
				return switch (arguments.length) {
					case 0 -> {
						yield new CastResult(mode.makeInvoker(parser, receiver, getter), false);
					}
					case 1 -> {
						InsnTree argument = arguments[0].cast(parser, field.type, CastMode.IMPLICIT_NULL);
						if (argument == null) yield null;
						yield new CastResult(mode.makeInvoker(parser, receiver, setter, argument), argument != arguments[0]);
					}
					default -> {
						yield null;
					}
				};
			}
		);
	}

	public void addMethod(TypeInfo owner, String name, MethodHandler handler) {
		this.methods.computeIfAbsent(new NamedType(owner, name), $ -> new ArrayList<>(4)).add(handler);
	}

	public void addClassFunction(String name, MethodHandler methodHandler) {
		this.methods.computeIfAbsent(new NamedType(TypeInfos.CLASS, name), $ -> new ArrayList<>(4)).add(methodHandler);
	}

	public void addClassFunction(TypeInfo type, String name, MethodInfo method) {
		this.addClassFunction(name, (parser, receiver, name1, mode, arguments) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(type)) {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
				if (castArguments != null) return new CastResult(mode.makeInvoker(parser, method, castArguments), castArguments != arguments);
			}
			return null;
		});
	}

	public void addConstructor(TypeInfo type, MethodInfo method) {
		this.addClassFunction("new", (parser, receiver, name1, mode, arguments) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(type)) {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
				if (castArguments != null) return new CastResult(newInstance(method, castArguments), castArguments != arguments);
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
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) throws ScriptParsingException {
		NamedType query = new NamedType();
		query.name = name;
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			query.owner = owner;
			FieldInfo field = this.fields.get(query);
			if (field != null) return mode.makeField(parser, receiver, field);
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		List<FunctionHandler> handlers = this.functions.get(name);
		if (handlers != null) {
			InsnTree result = null;
			for (int index = 0, size = handlers.size(); index < size; index++) {
				CastResult casted = handlers.get(index).create(parser, name, arguments);
				if (casted != null) {
					if (!casted.requiredCasting()) return casted.tree();
					else if (result == null) result = casted.tree();
				}
			}
			return result;
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) throws ScriptParsingException {
		NamedType query = new NamedType();
		query.name = name;
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			query.owner = owner;
			List<MethodHandler> handlers = this.methods.get(query);
			if (handlers != null) {
				InsnTree result = null;
				for (int index = 0, size = handlers.size(); index < size; index++) {
					CastResult casted = handlers.get(index).create(parser, receiver, name, mode, arguments);
					if (casted != null) {
						if (!casted.requiredCasting()) return casted.tree();
						else if (result == null) result = casted.tree();
					}
				}
				return result;
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

	public VarInfo newVariable(String name, TypeInfo type) {
		try {
			this.parser.checkVariable(name);
		}
		catch (ScriptParsingException exception) {
			throw new RuntimeException(exception.getMessage(), exception);
		}
		VarInfo variable = new VarInfo(name, -1, type);
		this.variables.put(name, load(variable));
		return variable;
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