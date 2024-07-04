package builderb0y.scripting.environments;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.StackMap;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class UserScriptEnvironment implements ScriptEnvironment {

	public ExpressionParser parser;
	public StackMap<String,         PendingLocal    > variables;
	public StackMap<NamedType,      FieldInfo       > fields;
	public StackMap<String,    List<FunctionHandler>> functions;
	public StackMap<NamedType, List<MethodHandler  >> methods;
	public StackMap<String,         TypeInfo        > types;
	public Set<DelayedMethod> delayedMethods;

	public UserScriptEnvironment() {
		this.variables = new StackMap<>(8);
		this.fields    = new StackMap<>(8);
		this.functions = new StackMap<>(4);
		this.methods   = new StackMap<>(4);
		this.types     = new StackMap<>(4);
		this.delayedMethods = Collections.emptySet();
	}

	public UserScriptEnvironment(UserScriptEnvironment from) {
		this.variables = new StackMap<>(from.variables);
		this.fields    = new StackMap<>(from.fields);
		this.functions = new StackMap<>(from.functions);
		this.methods   = new StackMap<>(from.methods);
		this.types     = new StackMap<>(from.types);
		this.delayedMethods = new HashSet<>(from.delayedMethods);
	}

	@Override
	public Stream<IdentifierDescriptor> listIdentifiers() {
		return Stream.of(
			this.variables.entrySet().stream().map((Map.Entry<String, PendingLocal> entry) -> {
				return MutableScriptEnvironment.prefix("Variable", entry.getKey(), entry.getKey(), entry.getValue());
			}),

			this.fields.entrySet().stream().map((Map.Entry<NamedType, FieldInfo> entry) -> {
				return MutableScriptEnvironment.prefix("Field", entry.getKey().name, entry.getKey().toString(), entry.getValue());
			}),

			this.functions.entrySet().stream().flatMap((Map.Entry<String, List<FunctionHandler>> entry) -> {
				return entry.getValue().stream().map((FunctionHandler handler) -> {
					return MutableScriptEnvironment.prefix("Function", entry.getKey(), entry.getKey(), handler);
				});
			}),

			this.methods.entrySet().stream().flatMap((Map.Entry<NamedType, List<MethodHandler>> entry) -> {
				return entry.getValue().stream().map((MethodHandler handler) -> {
					return MutableScriptEnvironment.prefix("Method", entry.getKey().name, entry.getKey().toString(), handler);
				});
			}),

			this.types.entrySet().stream().map((Map.Entry<String, TypeInfo> entry) -> {
				return MutableScriptEnvironment.prefix("Type", entry.getKey(), entry.getKey(), entry.getValue());
			})
		)
		.flatMap(Function.identity());
	}

	public void reserveVariable(String name) {
		PendingLocal old = this.variables.putIfAbsent(name, new PendingLocal(name));
		if (old != null) throw new IllegalArgumentException("Variable '" + name + "' has already been declared in this scope.");
	}

	public void setVariableType(String name, TypeInfo type) {
		PendingLocal variable = this.variables.get(name);
		if (variable == null) throw new IllegalArgumentException("Variable '" + name + "' has not yet been declared in this scope.");
		if (variable.type != null) throw new IllegalStateException("Variable '" + type + "' is already of type " + variable.type + " when trying to set the type to " + type);
		variable.type = type;
	}

	public void reserveVariable(String name, TypeInfo type) {
		PendingLocal old = this.variables.putIfAbsent(name, new PendingLocal(name, type));
		if (old != null) throw new IllegalArgumentException("Variable '" + name + "' has already been declared in this scope.");
	}

	public void assignVariable(String name) {
		PendingLocal local = this.variables.get(name);
		if (local == null) throw new IllegalArgumentException("Variable '" + name + "' has not yet been declared in this scope.");
		if (local.assigned) throw new IllegalStateException("Variable '" + name + "' has already been assigned.");
		local.assigned = true;
	}

	public void reserveAndAssignVariable(String name, TypeInfo type) {
		PendingLocal newLocal = new PendingLocal(name, type);
		newLocal.assigned = true;
		PendingLocal old = this.variables.putIfAbsent(name, newLocal);
		if (old != null) throw new IllegalArgumentException("Variable '" + name + "' has already been declared in this scope.");
	}

	public void addFunction(String name, FunctionHandler functionHandler) {
		this.functions.computeIfAbsent(name, (String ignored) -> new ArrayList<>(4)).add(functionHandler);
	}

	public void addFieldGetterAndSetter(FieldInfo field) {
		NamedType namedType = new NamedType(field.owner, field.name);
		this.fields.put(namedType, field);
		List<MethodHandler> handlers = this.methods.computeIfAbsent(namedType, (NamedType ignored) -> new ArrayList<>(2));
		MethodInfo getter = new MethodInfo(ACC_PUBLIC, field.owner, field.name, field.type);
		MethodInfo setter = new MethodInfo(ACC_PUBLIC, field.owner, field.name, TypeInfos.VOID, field.type);
		handlers.add(
			(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
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
		this.methods.computeIfAbsent(new NamedType(owner, name), (NamedType ignored) -> new ArrayList<>(4)).add(handler);
	}

	public void addClassFunction(String name, MethodHandler methodHandler) {
		this.methods.computeIfAbsent(new NamedType(TypeInfos.CLASS, name), (NamedType ignored) -> new ArrayList<>(4)).add(methodHandler);
	}

	public void addClassFunction(TypeInfo type, String name, MethodInfo method) {
		this.addClassFunction(name, (ExpressionParser parser, InsnTree receiver, String name_, GetMethodMode mode, InsnTree... arguments) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(type)) {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
				if (castArguments != null) return new CastResult(mode.makeInvoker(parser, method, castArguments), castArguments != arguments);
			}
			return null;
		});
	}

	public void addConstructor(TypeInfo type, MethodInfo method) {
		this.addClassFunction("new", (ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
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
		PendingLocal local = this.variables.get(name);
		if (local != null) {
			LazyVarInfo variable = local.variable();
			this.markVariableUsed(variable);
			return load(variable);
		}
		else {
			return null;
		}
	}

	public void markVariableUsed(LazyVarInfo variable) {
		if (!this.delayedMethods.isEmpty()) {
			for (DelayedMethod method : this.delayedMethods) {
				method.onVariableUsed(variable);
			}
		}
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

	public static class PendingLocal {

		public String name;
		public TypeInfo type;
		public boolean assigned;

		public PendingLocal(String name) {
			this.name = name;
		}

		public PendingLocal(String name, TypeInfo type) {
			this.name = name;
			this.type = type;
		}

		public LazyVarInfo variable() {
			if (this.type == null) throw new IllegalArgumentException("Variable '" + this.name + "' has not had its type inferred yet.");
			if (!this.assigned) throw new IllegalArgumentException("Variable '" + this.name + "' has not been assigned to yet.");
			return new LazyVarInfo(this.name, this.type);
		}

		public LoadInsnTree loader() {
			return load(this.variable());
		}

		@Override
		public String toString() {
			return this.name + " : " + (this.type != null ? this.type.toString() : "(type not yet inferred)") + (this.assigned ? " (available)" : " (not yet assigned to)");
		}
	}
}