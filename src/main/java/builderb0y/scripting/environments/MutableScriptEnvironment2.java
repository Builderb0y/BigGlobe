package builderb0y.scripting.environments;

import java.lang.reflect.Method;
import java.util.*;

import com.google.common.collect.ObjectArrays;
import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MutableScriptEnvironment2 implements ScriptEnvironment {

	public Map<String,    List<VariableHandler>> variables;
	public Map<NamedType, List<   FieldHandler>> fields;
	public Map<String,    List<FunctionHandler>> functions;
	public Map<NamedType, List<  MethodHandler>> methods;
	public Map<String,    TypeInfo             > types;

	public MutableScriptEnvironment2() {
		this.variables = new HashMap<>(16);
		this.fields    = new HashMap<>(16);
		this.functions = new HashMap<>(16);
		this.methods   = new HashMap<>(16);
		this.types     = new HashMap<>( 8);
	}

	public MutableScriptEnvironment2(
		Map<String,    List<VariableHandler>> variables,
		Map<NamedType, List<   FieldHandler>> fields,
		Map<String,    List<FunctionHandler>> functions,
		Map<NamedType, List<  MethodHandler>> methods,
		Map<String,    TypeInfo             > types
	) {
		this.variables = variables;
		this.fields    = fields;
		this.functions = functions;
		this.methods   = methods;
		this.types     = types;
	}

	public MutableScriptEnvironment2 addAllVariables(MutableScriptEnvironment2 that) {
		this.variables.putAll(that.variables);
		return this;
	}

	public MutableScriptEnvironment2 addAllFields(MutableScriptEnvironment2 that) {
		this.fields.putAll(that.fields);
		return this;
	}

	public MutableScriptEnvironment2 addAllFunctions(MutableScriptEnvironment2 that) {
		this.functions.putAll(that.functions);
		return this;
	}

	public MutableScriptEnvironment2 addAllMethods(MutableScriptEnvironment2 that) {
		this.methods.putAll(that.methods);
		return this;
	}

	public MutableScriptEnvironment2 addAllTypes(MutableScriptEnvironment2 that) {
		this.types.putAll(that.types);
		return this;
	}

	public MutableScriptEnvironment2 addAll(MutableScriptEnvironment2 that) {
		this.variables.putAll(that.variables);
		this.fields   .putAll(that.fields);
		this.functions.putAll(that.functions);
		this.methods  .putAll(that.methods);
		this.types    .putAll(that.types);
		return this;
	}

	//////////////////////////////// variables ////////////////////////////////

	public MutableScriptEnvironment2 addVariable(String name, VariableHandler variableHandler) {
		this.variables.computeIfAbsent(name, $ -> new ArrayList<>(4)).add(variableHandler);
		return this;
	}

	public MutableScriptEnvironment2 addVariable(String name, InsnTree tree) {
		return this.addVariable(name, (parser, name1) -> tree);
	}

	//////////////// load ////////////////

	public MutableScriptEnvironment2 addVariableLoad(String name, VarInfo variable) {
		return this.addVariable(name, load(variable));
	}

	public MutableScriptEnvironment2 addVariableLoad(VarInfo variable) {
		return this.addVariable(variable.name, load(variable));
	}

	public MutableScriptEnvironment2 addVariableLoad(String name, int index, TypeInfo type) {
		return this.addVariable(name, load(name, index, type));
	}

	//////////////// getStatic ////////////////

	public MutableScriptEnvironment2 addVariableGetStatic(String name, FieldInfo field) {
		return this.addVariable(name, getStatic(field));
	}

	public MutableScriptEnvironment2 addVariableGetStatic(FieldInfo field) {
		return this.addVariableGetStatic(field.name, field);
	}

	public MutableScriptEnvironment2 addVariableGetStatic(Class<?> in, String name) {
		return this.addVariableGetStatic(name, FieldInfo.forField(ReflectionData.forClass(in).getDeclaredField(name)));
	}

	public MutableScriptEnvironment2 addVariableGetStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addVariableGetStatic(in, name);
		}
		return this;
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment2 addVariableInvokeStatic(String name, MethodInfo method) {
		if (method.paramTypes.length != 0) throw new IllegalArgumentException("Static getter requires parameters");
		return this.addVariable(name, invokeStatic(method));
	}

	public MutableScriptEnvironment2 addVariableInvokeStatic(MethodInfo method) {
		return this.addVariableInvokeStatic(method.name, method);
	}

	public MutableScriptEnvironment2 addVariableInvokeStatic(Class<?> in, String getterName) {
		return this.addVariableInvokeStatic(getterName, MethodInfo.forMethod(ReflectionData.forClass(in).findDeclaredMethod(getterName, m -> m.getParameterCount() == 0)));
	}

	public MutableScriptEnvironment2 addVariableInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addVariableInvokeStatic(in, name);
		}
		return this;
	}

	//////////////////////////////// fields ////////////////////////////////

	public MutableScriptEnvironment2 addField(TypeInfo owner, String name, FieldHandler fieldHandler) {
		this.fields.computeIfAbsent(new NamedType(owner, name), $ -> new ArrayList<>(4)).add(fieldHandler);
		return this;
	}

	//////////////// get ////////////////

	public MutableScriptEnvironment2 addFieldGet(String name, FieldInfo field) {
		return this.addField(field.owner, name, (parser, receiver, name1) -> InsnTrees.getField(receiver, field));
	}

	public MutableScriptEnvironment2 addFieldGet(FieldInfo field) {
		return this.addField(field.owner, field.name, (parser, receiver, name1) -> InsnTrees.getField(receiver, field));
	}

	public MutableScriptEnvironment2 addFieldGet(Class<?> in, String name) {
		return this.addFieldGet(name, FieldInfo.forField(ReflectionData.forClass(in).getDeclaredField(name)));
	}

	public MutableScriptEnvironment2 addFieldGets(Class<?> in, String... names) {
		for (String name : names) {
			this.addFieldGet(in, name);
		}
		return this;
	}

	//////////////// invoke ////////////////

	public MutableScriptEnvironment2 addFieldInvoke(String name, MethodInfo getter) {
		if (getter.paramTypes.length != 0) throw new IllegalArgumentException("Getter requires parameters");
		return this.addField(getter.owner, name, (parser, receiver, name1) -> invokeVirtualOrInterface(receiver, getter));
	}

	public MutableScriptEnvironment2 addFieldInvoke(MethodInfo getter) {
		return this.addFieldInvoke(getter.name, getter);
	}

	public MutableScriptEnvironment2 addFieldInvoke(Class<?> in, String name) {
		return this.addFieldInvoke(name, MethodInfo.forMethod(ReflectionData.forClass(in).findDeclaredMethod(name, m -> m.getParameterCount() == 0)));
	}

	public MutableScriptEnvironment2 addFieldInvokes(Class<?> in, String... names) {
		for (String name : names) {
			this.addFieldInvoke(in, name);
		}
		return this;
	}

	//////////////////////////////// functions ////////////////////////////////

	public MutableScriptEnvironment2 addFunction(String name, FunctionHandler functionHandler) {
		this.functions.computeIfAbsent(name, $ -> new ArrayList<>(8)).add(functionHandler);
		return this;
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment2 addFunctionInvokeStatic(String name, MethodInfo method) {
		return this.addFunction(name, (parser, name1, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : invokeStatic(method, castArguments);
		});
	}

	public MutableScriptEnvironment2 addFunctionInvokeStatic(MethodInfo method) {
		return this.addFunctionInvokeStatic(method.name, method);
	}

	public MutableScriptEnvironment2 addFunctionInvokeStatic(Class<?> in, String name) {
		return this.addFunctionInvokeStatic(MethodInfo.forMethod(ReflectionData.forClass(in).getDeclaredMethod(name)));
	}

	public MutableScriptEnvironment2 addFunctionInvokeStatic(Class<?> in, String name, Class<?> returnType, Class<?>... parameterTypes) {
		return this.addFunctionInvokeStatic(MethodInfo.forMethod(ReflectionData.forClass(in).findDeclaredMethod(name, returnType, parameterTypes)));
	}

	public MutableScriptEnvironment2 addFunctionInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addFunctionInvokeStatic(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment2 addFunctionMultiInvokeStatic(Class<?> in, String name) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(name)) {
			this.addFunctionInvokeStatic(name, MethodInfo.forMethod(method));
		}
		return this;
	}

	public MutableScriptEnvironment2 addFunctionMultiInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addFunctionMultiInvokeStatic(in, name);
		}
		return this;
	}

	//////////////// invoke ////////////////

	public MutableScriptEnvironment2 addFunctionInvoke(String name, InsnTree receiver, MethodInfo method) {
		return this.addFunction(name, (parser, name1, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : invokeVirtualOrInterface(receiver, method, castArguments);
		});
	}

	public MutableScriptEnvironment2 addFunctionInvoke(InsnTree receiver, MethodInfo method) {
		return this.addFunctionInvoke(method.name, receiver, method);
	}

	public MutableScriptEnvironment2 addFunctionInvoke(InsnTree receiver, Class<?> in, String name) {
		return this.addFunctionInvoke(name, receiver, MethodInfo.forMethod(ReflectionData.forClass(in).getDeclaredMethod(name)));
	}

	public MutableScriptEnvironment2 addFunctionInvoke(InsnTree receiver, Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addFunctionInvoke(name, receiver, MethodInfo.forMethod(ReflectionData.forClass(in).findDeclaredMethod(name, returnType, paramTypes)));
	}

	public MutableScriptEnvironment2 addFunctionInvokes(InsnTree receiver, Class<?> in, String... names) {
		for (String name : names) {
			this.addFunctionInvoke(receiver, in, name);
		}
		return this;
	}

	//////////////////////////////// methods ////////////////////////////////

	public MutableScriptEnvironment2 addMethod(TypeInfo owner, String name, MethodHandler methodHandler) {
		this.methods.computeIfAbsent(new NamedType(owner, name), $ -> new ArrayList<>(8)).add(methodHandler);
		return this;
	}

	//////////////// invoke ////////////////

	public MutableScriptEnvironment2 addMethodInvoke(String name, MethodInfo method) {
		return this.addMethod(method.owner, name, (parser, receiver, name1, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : invokeVirtualOrInterface(receiver, method, castArguments);
		});
	}

	public MutableScriptEnvironment2 addMethodInvoke(MethodInfo method) {
		return this.addMethodInvoke(method.name, method);
	}

	public MutableScriptEnvironment2 addMethodInvoke(Class<?> in, String name) {
		return this.addMethodInvoke(name, MethodInfo.forMethod(ReflectionData.forClass(in).getDeclaredMethod(name)));
	}

	public MutableScriptEnvironment2 addMethodInvokes(Class<?> in, String... names) {
		for (String name : names) {
			this.addMethodInvoke(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment2 addMethodMultiInvoke(Class<?> in, String name) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(name)) {
			this.addMethodInvoke(name, MethodInfo.forMethod(method));
		}
		return this;
	}

	public MutableScriptEnvironment2 addMethodMultiInvokes(Class<?> in, String... names) {
		for (String name : names) {
			this.addMethodMultiInvoke(in, name);
		}
		return this;
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment2 addMethodInvokeStatic(String name, MethodInfo method) {
		return this.addMethod(method.paramTypes[0], name, (parser, receiver, name1, arguments) -> {
			InsnTree[] concatArguments = ObjectArrays.concat(receiver, arguments);
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, concatArguments);
			return castArguments == null ? null : invokeStatic(method, castArguments);
		});
	}

	public MutableScriptEnvironment2 addMethodInvokeStatic(MethodInfo method) {
		return this.addMethodInvokeStatic(method.name, method);
	}

	public MutableScriptEnvironment2 addMethodInvokeStatic(Class<?> in, String name) {
		return this.addMethodInvokeStatic(name, MethodInfo.forMethod(ReflectionData.forClass(in).getDeclaredMethod(name)));
	}

	public MutableScriptEnvironment2 addMethodInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addMethodInvokeStatic(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment2 addMethodMultiInvokeStatic(Class<?> in, String name) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(name)) {
			this.addMethodInvokeStatic(name, MethodInfo.forMethod(method));
		}
		return this;
	}

	public MutableScriptEnvironment2 addMethodMultiInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addMethodMultiInvokeStatic(in, name);
		}
		return this;
	}

	//////////////////////////////// types ////////////////////////////////

	public MutableScriptEnvironment2 addType(String name, TypeInfo type) {
		this.types.put(name, type);
		return this;
	}

	public MutableScriptEnvironment2 addType(String name, Class<?> type) {
		return this.addType(name, TypeInfo.of(type));
	}

	//////////////////////////////// qualified variables ////////////////////////////////

	public MutableScriptEnvironment2 addQualifiedVariable(TypeInfo owner, String name, VariableHandler variableHandler) {
		return this.addField(TypeInfos.CLASS, name, (parser, receiver, name1) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(owner)) {
				return variableHandler.create(parser, name1);
			}
			return null;
		});
	}

	public MutableScriptEnvironment2 addQualifiedVariable(TypeInfo owner, String name, InsnTree tree) {
		return this.addQualifiedVariable(owner, name, (parser, name1) -> tree);
	}

	//////////////// getStatic ////////////////

	public MutableScriptEnvironment2 addQualifiedVariableGetStatic(TypeInfo owner, String name, FieldInfo field) {
		return this.addQualifiedVariable(owner, name, getStatic(field));
	}

	public MutableScriptEnvironment2 addQualifiedVariableGetStatic(String name, FieldInfo field) {
		return this.addQualifiedVariable(field.owner, name, getStatic(field));
	}

	public MutableScriptEnvironment2 addQualifiedVariableGetStatic(TypeInfo owner, FieldInfo field) {
		return this.addQualifiedVariable(owner, field.name, getStatic(field));
	}

	public MutableScriptEnvironment2 addQualifiedVariableGetStatic(FieldInfo field) {
		return this.addQualifiedVariable(field.owner, field.name, getStatic(field));
	}

	public MutableScriptEnvironment2 addQualifiedVariableGetStatic(TypeInfo owner, Class<?> in, String name) {
		return this.addQualifiedVariableGetStatic(owner, name, FieldInfo.forField(ReflectionData.forClass(in).getDeclaredField(name)));
	}

	public MutableScriptEnvironment2 addQualifiedVariableGetStatic(Class<?> in, String name) {
		return this.addQualifiedVariableGetStatic(TypeInfo.of(in), in, name);
	}

	public MutableScriptEnvironment2 addQualifiedVariableGetStatics(TypeInfo owner, Class<?> in, String... names) {
		for (String name : names) {
			this.addQualifiedVariableGetStatic(owner, in, name);
		}
		return this;
	}

	public MutableScriptEnvironment2 addQualifiedVariableGetStatics(Class<?> in, String... names) {
		return this.addQualifiedVariableGetStatics(TypeInfo.of(in), in, names);
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment2 addQualifiedVariableInvokeStatic(TypeInfo owner, String name, MethodInfo method) {
		if (method.paramTypes.length != 0) throw new IllegalArgumentException("Qualified static getter requires parameters");
		return this.addQualifiedVariable(owner, name, invokeStatic(method));
	}

	public MutableScriptEnvironment2 addQualifiedVariableInvokeStatic(TypeInfo owner, MethodInfo method) {
		return this.addQualifiedVariableInvokeStatic(owner, method.name, method);
	}

	public MutableScriptEnvironment2 addQualifiedVariableInvokeStatic(String name, MethodInfo method) {
		return this.addQualifiedVariableInvokeStatic(method.owner, name, method);
	}

	public MutableScriptEnvironment2 addQualifiedVariableInvokeStatic(MethodInfo method) {
		return this.addQualifiedVariableInvokeStatic(method.owner, method.name, method);
	}

	public MutableScriptEnvironment2 addQualifiedVariableInvokeStatic(TypeInfo owner, Class<?> in, String name) {
		return this.addQualifiedVariableInvokeStatic(owner, name, MethodInfo.forMethod(ReflectionData.forClass(in).getDeclaredMethod(name)));
	}

	public MutableScriptEnvironment2 addQualifiedVariableInvokeStatic(Class<?> in, String name) {
		return this.addQualifiedVariableInvokeStatic(TypeInfo.of(in), in, name);
	}

	public MutableScriptEnvironment2 addQualifiedVariableInvokeStatics(TypeInfo owner, Class<?> in, String... names) {
		for (String name : names) {
			return this.addQualifiedVariableInvokeStatic(owner, in, name);
		}
		return this;
	}

	public MutableScriptEnvironment2 addQualifiedVariableInvokeStatics(Class<?> in, String... names) {
		return this.addQualifiedVariableInvokeStatics(TypeInfo.of(in), in, names);
	}

	//////////////////////////////// qualified functions ////////////////////////////////

	public MutableScriptEnvironment2 addQualifiedFunction(TypeInfo owner, String name, FunctionHandler functionHandler) {
		return this.addMethod(TypeInfos.CLASS, name, (parser, receiver, name1, arguments) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(owner)) {
				return functionHandler.create(parser, name1, arguments);
			}
			return null;
		});
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment2 addQualifiedFunctionInvokeStatic(TypeInfo owner, String name, MethodInfo method) {
		return this.addQualifiedFunction(owner, name, (parser, name1, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : invokeStatic(method, castArguments);
		});
	}

	public MutableScriptEnvironment2 addQualifiedFunctionInvokeStatic(String name, MethodInfo method) {
		return this.addQualifiedFunctionInvokeStatic(method.owner, name, method);
	}

	public MutableScriptEnvironment2 addQualifiedFunctionInvokeStatic(TypeInfo owner, MethodInfo method) {
		return this.addQualifiedFunctionInvokeStatic(owner, method.name, method);
	}

	public MutableScriptEnvironment2 addQualifiedFunctionInvokeStatic(MethodInfo method) {
		return this.addQualifiedFunctionInvokeStatic(method.owner, method.name, method);
	}

	public MutableScriptEnvironment2 addQualifiedFunctionInvokeStatic(TypeInfo owner, Class<?> in, String name) {
		return this.addQualifiedFunctionInvokeStatic(owner, MethodInfo.forMethod(ReflectionData.forClass(in).getDeclaredMethod(name)));
	}

	public MutableScriptEnvironment2 addQualifiedFunctionInvokeStatic(Class<?> in, String name) {
		return this.addQualifiedFunctionInvokeStatic(TypeInfo.of(in), MethodInfo.forMethod(ReflectionData.forClass(in).getDeclaredMethod(name)));
	}

	public MutableScriptEnvironment2 addQualifiedFunctionInvokeStatic(TypeInfo owner, Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addQualifiedFunctionInvokeStatic(owner, MethodInfo.forMethod(ReflectionData.forClass(in).findDeclaredMethod(name, returnType, paramTypes)));
	}

	public MutableScriptEnvironment2 addQualifiedFunctionInvokeStatic(Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addQualifiedFunctionInvokeStatic(TypeInfo.of(in), MethodInfo.forMethod(ReflectionData.forClass(in).findDeclaredMethod(name, returnType, paramTypes)));
	}

	//////////////// invoke ////////////////

	//////////////////////////////// getters ////////////////////////////////

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		List<VariableHandler> handlers = this.variables.get(name);
		if (handlers != null) for (int index = 0, size = handlers.size(); index < size; index++) {
			InsnTree tree = handlers.get(index).create(parser, name);
			if (tree != null) return tree;
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		NamedType query = new NamedType();
		query.name = name;
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			query.owner = owner;
			List<FieldHandler> handlers = this.fields.get(query);
			if (handlers != null) for (int index = 0, size = handlers.size(); index < size; index++) {
				InsnTree tree = handlers.get(index).create(parser, receiver, name);
				if (tree != null) return tree;
			}
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

	//////////////////////////////// handlers ////////////////////////////////

	@FunctionalInterface
	public static interface VariableHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, String name) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface FieldHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface FunctionHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface MethodHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException;
	}

	public static class NamedType {

		public TypeInfo owner;
		public String name;

		public NamedType() {}

		public NamedType(TypeInfo owner, String name) {
			this.owner = owner;
			this.name = name;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.owner) * 31 + Objects.hashCode(this.name);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof NamedType that &&
				Objects.equals(this.owner, that.owner) &&
				Objects.equals(this.name, that.name)
			);
		}

		@Override
		public String toString() {
			return "NamedType: { owner: " + this.owner + ", name: " + this.name + " }";
		}
	}
}