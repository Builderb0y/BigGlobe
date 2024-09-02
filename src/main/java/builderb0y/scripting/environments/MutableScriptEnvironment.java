package builderb0y.scripting.environments;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;
import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.casting.IdentityCastInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.OpcodeCastInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@SuppressWarnings({ "unused", "UnusedReturnValue", "SameParameterValue" })
public class MutableScriptEnvironment implements ScriptEnvironment {

	public static final Predicate<Method>
		IS_STATIC   = (Method method) ->  Modifier.isStatic(method.getModifiers()),
		IS_INSTANCE = (Method method) -> !Modifier.isStatic(method.getModifiers()),
		NO_ARGS     = (Method method) -> method.getParameterCount() == 0;

	public Map<String,              VariableHandler.Named > variables      = new HashMap<>(16);
	public Map<NamedType,              FieldHandler.Named > fields         = new HashMap<>(16);
	public Map<String,         List<FunctionHandler.Named>> functions      = new HashMap<>(16);
	public Map<NamedType,      List<  MethodHandler.Named>> methods        = new HashMap<>(16);
	public Map<String,                     TypeInfo       > types          = new HashMap<>( 8);
	public Map<String,               KeywordHandler.Named > keywords       = new HashMap<>( 8);
	public Map<NamedType, List<MemberKeywordHandler.Named>> memberKeywords = new HashMap<>( 8);
	//         from          to
	public Map<TypeInfo,  Map<TypeInfo, CastHandlerHolder>> casters        = new HashMap<>(16);

	public static IdentifierDescriptor prefix(String type, String name, String descriptor, Object value) {
		return new IdentifierDescriptor(
			name != null ? name : "<any name>",
			new Object() {

				@Override
				public String toString() {
					return type + ' ' + descriptor + ": " + value;
				}
			}
		);
	}

	@Override
	public Stream<IdentifierDescriptor> listIdentifiers() {
		return Stream.of(
			this.variables.entrySet().stream().map((Map.Entry<String, VariableHandler.Named> entry) -> {
				return prefix("Variable", entry.getKey(), entry.getKey(), entry.getValue());
			}),

			this.fields.entrySet().stream().map((Map.Entry<NamedType, FieldHandler.Named> entry) -> {
				return prefix("Field", entry.getKey().name, entry.getKey().toString(), entry.getValue());
			}),

			this.functions.entrySet().stream().flatMap((Map.Entry<String, List<FunctionHandler.Named>> entry) -> {
				return entry.getValue().stream().map((FunctionHandler.Named handler) -> {
					return prefix("Function", entry.getKey(), entry.getKey(), handler);
				});
			}),

			this.methods.entrySet().stream().flatMap((Map.Entry<NamedType, List<MethodHandler.Named>> entry) -> {
				return entry.getValue().stream().map((MethodHandler.Named handler) -> {
					return prefix("Method", entry.getKey().name, entry.getKey().toString(), handler);
				});
			}),

			this.types.entrySet().stream().map((Map.Entry<String, TypeInfo> entry) -> {
				return prefix("Type", entry.getKey(), entry.getKey(), entry.getValue());
			}),

			this.keywords.entrySet().stream().map((Map.Entry<String, KeywordHandler.Named> entry) -> {
				return prefix("Keyword", entry.getKey(), entry.getKey(), entry.getValue());
			}),

			this.memberKeywords.entrySet().stream().flatMap((Map.Entry<NamedType, List<MemberKeywordHandler.Named>> entry) -> {
				return entry.getValue().stream().map((MemberKeywordHandler.Named handler) -> {
					return prefix("Member keyword", entry.getKey().name, entry.getKey().toString(), handler);
				});
			})
		)
		.flatMap(Function.identity());
	}

	public MutableScriptEnvironment addAllVariables(MutableScriptEnvironment that) {
		if (!that.variables.isEmpty()) {
			for (Map.Entry<String, VariableHandler.Named> entry : that.variables.entrySet()) {
				if (this.variables.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
					throw new IllegalArgumentException("Variable '" + entry.getKey() + "' is already defined in this scope");
				}
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllFields(MutableScriptEnvironment that) {
		if (!that.fields.isEmpty()) {
			for (Map.Entry<NamedType, FieldHandler.Named> entry : that.fields.entrySet()) {
				if (this.fields.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
					throw new IllegalArgumentException("Field '" + entry.getKey() + "' is already defined in this scope");
				}
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllFunctions(MutableScriptEnvironment that) {
		if (!that.functions.isEmpty()) {
			for (Map.Entry<String, List<FunctionHandler.Named>> entry : that.functions.entrySet()) {
				List<FunctionHandler.Named> handlers = this.functions.get(entry.getKey());
				if (handlers != null) handlers.addAll(entry.getValue());
				else this.functions.put(entry.getKey(), new ArrayList<>(entry.getValue()));
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllMethods(MutableScriptEnvironment that) {
		if (!that.methods.isEmpty()) {
			for (Map.Entry<NamedType, List<MethodHandler.Named>> entry : that.methods.entrySet()) {
				List<MethodHandler.Named> handlers = this.methods.get(entry.getKey());
				if (handlers != null) handlers.addAll(entry.getValue());
				else this.methods.put(entry.getKey(), new ArrayList<>(entry.getValue()));
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllTypes(MutableScriptEnvironment that) {
		if (!that.types.isEmpty()) {
			for (Map.Entry<String, TypeInfo> entry : that.types.entrySet()) {
				if (this.types.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
					throw new IllegalArgumentException("Type '" + entry.getKey() + "' is already defined in this scope");
				}
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllKeywords(MutableScriptEnvironment that) {
		if (!that.keywords.isEmpty()) {
			for (Map.Entry<String, KeywordHandler.Named> entry : that.keywords.entrySet()) {
				if (this.keywords.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
					throw new IllegalArgumentException("Keyword '" + entry.getKey() + "' is already defined in this scope");
				}
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllMemberKeywords(MutableScriptEnvironment that) {
		if (!that.memberKeywords.isEmpty()) {
			for (Map.Entry<NamedType, List<MemberKeywordHandler.Named>> entry : that.memberKeywords.entrySet()) {
				List<MemberKeywordHandler.Named> handlers = this.memberKeywords.get(entry.getKey());
				if (handlers != null) handlers.addAll(entry.getValue());
				else this.memberKeywords.put(entry.getKey(), new ArrayList<>(entry.getValue()));
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllCasters(MutableScriptEnvironment that) {
		if (!that.casters.isEmpty()) {
			for (Map.Entry<TypeInfo, Map<TypeInfo, CastHandlerHolder>> entry : that.casters.entrySet()) {
				Map<TypeInfo, CastHandlerHolder> from = this.casters.get(entry.getKey());
				Map<TypeInfo, CastHandlerHolder> to = entry.getValue();
				if (from == null) {
					this.casters.put(entry.getKey(), new HashMap<>(to));
				}
				else {
					for (Map.Entry<TypeInfo, CastHandlerHolder> entry2 : to.entrySet()) {
						if (from.putIfAbsent(entry2.getKey(), entry2.getValue()) != null) {
							throw new IllegalArgumentException("Caster " + entry.getKey() + " -> " + entry2.getKey() + " (" + entry2.getValue() + ") is already present in this environment");
						}
					}
				}
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAll(MutableScriptEnvironment that) {
		return (
			this
			.addAllVariables     (that)
			.addAllFields        (that)
			.addAllFunctions     (that)
			.addAllMethods       (that)
			.addAllTypes         (that)
			.addAllKeywords      (that)
			.addAllMemberKeywords(that)
			.addAllCasters       (that)
		);
	}

	public MutableScriptEnvironment multiAddAll(MutableScriptEnvironment... environments) {
		for (MutableScriptEnvironment environment : environments) {
			this.addAll(environment);
		}
		return this;
	}

	public MutableScriptEnvironment configure(Consumer<MutableScriptEnvironment> configurator) {
		configurator.accept(this);
		return this;
	}

	//////////////////////////////// variables ////////////////////////////////

	public MutableScriptEnvironment addVariable(String name, VariableHandler.Named variableHandler) {
		if (this.variables.putIfAbsent(name, variableHandler) != null) {
			throw new IllegalArgumentException("Variable '" + name + "' is already defined in this scope");
		}
		return this;
	}

	public MutableScriptEnvironment addVariable(String name, InsnTree tree) {
		return this.addVariable(name, new VariableHandler.Named(tree.describe(), (ExpressionParser parser, String name1) -> tree));
	}

	//////////////// load ////////////////

	public MutableScriptEnvironment addVariableLoad(String name, LazyVarInfo variable) {
		return this.addVariable(name, load(variable));
	}

	public MutableScriptEnvironment addVariableLoad(LazyVarInfo variable) {
		return this.addVariable(variable.name, load(variable));
	}

	public MutableScriptEnvironment addVariableLoad(String name, TypeInfo type) {
		return this.addVariable(name, load(name, type));
	}

	//////////////// getField ////////////////

	public MutableScriptEnvironment addVariableRenamedGetField(InsnTree receiver, String name, FieldInfo field) {
		return this.addVariable(name, InsnTrees.getField(receiver, field));
	}

	public MutableScriptEnvironment addVariableGetField(InsnTree receiver, FieldInfo field) {
		return this.addVariable(field.name, InsnTrees.getField(receiver, field));
	}

	public MutableScriptEnvironment addVariableRenamedGetField(InsnTree receiver, String exposedName, Class<?> in, String actualName) {
		return this.addVariable(exposedName, InsnTrees.getField(receiver, FieldInfo.getField(in, actualName)));
	}

	public MutableScriptEnvironment addVariableGetField(InsnTree receiver, Class<?> in, String name) {
		return this.addVariable(name, InsnTrees.getField(receiver, FieldInfo.getField(in, name)));
	}

	public MutableScriptEnvironment addVariableGetFields(InsnTree receiver, Class<?> in, String... names) {
		for (String name : names) {
			this.addVariableGetField(receiver, in, name);
		}
		return this;
	}

	//////////////// getStatic ////////////////

	public MutableScriptEnvironment addVariableGetStatic(String name, FieldInfo field) {
		return this.addVariable(name, getStatic(field));
	}

	public MutableScriptEnvironment addVariableGetStatic(FieldInfo field) {
		return this.addVariableGetStatic(field.name, field);
	}

	public MutableScriptEnvironment addVariableGetStatic(Class<?> in, String name) {
		return this.addVariableGetStatic(name, FieldInfo.getField(in, name));
	}

	public MutableScriptEnvironment addVariableGetStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addVariableGetStatic(in, name);
		}
		return this;
	}

	//////////////// invoke ////////////////

	public MutableScriptEnvironment addVariableRenamedInvoke(InsnTree receiver, String name, MethodInfo method) {
		return this.addVariable(name, invokeInstance(receiver, method));
	}

	public MutableScriptEnvironment addVariableInvoke(InsnTree receiver, MethodInfo method) {
		return this.addVariable(method.name, invokeInstance(receiver, method));
	}

	public MutableScriptEnvironment addVariableInvoke(InsnTree receiver, Class<?> in, String name) {
		return this.addVariable(name, invokeInstance(receiver, MethodInfo.getMethod(in, name)));
	}

	public MutableScriptEnvironment addVariableInvokes(InsnTree receiver, Class<?> in, String... names) {
		for (String name : names) {
			this.addVariableInvoke(receiver, in, name);
		}
		return this;
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment addVariableInvokeStatic(String name, MethodInfo method) {
		if (method.paramTypes.length != 0) throw new IllegalArgumentException("Static getter requires parameters");
		return this.addVariable(name, invokeStatic(method));
	}

	public MutableScriptEnvironment addVariableInvokeStatic(MethodInfo method) {
		return this.addVariableInvokeStatic(method.name, method);
	}

	public MutableScriptEnvironment addVariableInvokeStatic(Class<?> in, String getterName) {
		return this.addVariableInvokeStatic(getterName, MethodInfo.getMethod(in, getterName, NO_ARGS.and(IS_STATIC)));
	}

	public MutableScriptEnvironment addVariableInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addVariableInvokeStatic(in, name);
		}
		return this;
	}

	//////////////// constant ////////////////

	public MutableScriptEnvironment addVariableConstant(String name, ConstantValue constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, boolean constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, byte constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, char constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, short constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, int constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, long constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, float constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, double constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, String constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, TypeInfo constant) {
		return this.addVariable(name, ldc(constant));
	}

	public MutableScriptEnvironment addVariableConstant(String name, Object constant, TypeInfo type) {
		return this.addVariable(name, ldc(constant, type));
	}

	//////////////////////////////// fields ////////////////////////////////

	public MutableScriptEnvironment addField(TypeInfo owner, String name, FieldHandler.Named fieldHandler) {
		if (this.fields.putIfAbsent(new NamedType(owner, name), fieldHandler) != null) {
			throw new IllegalArgumentException("Field '" + owner + '.' + name + "' is already defined in this scope");
		}
		return this;
	}

	//////////////// get ////////////////

	public MutableScriptEnvironment addFieldGet(String name, FieldInfo field) {
		return this.addField(field.owner, name, new FieldHandler.Named(field.toString(), (ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> mode.makeField(parser, receiver, field)));
	}

	public MutableScriptEnvironment addFieldGet(FieldInfo field) {
		return this.addFieldGet(field.name, field);
	}

	public MutableScriptEnvironment addFieldGet(Class<?> in, String name) {
		return this.addFieldGet(name, FieldInfo.getField(in, name));
	}

	public MutableScriptEnvironment addFieldGets(Class<?> in, String... names) {
		for (String name : names) {
			this.addFieldGet(in, name);
		}
		return this;
	}

	//////////////// invoke ////////////////

	public MutableScriptEnvironment addFieldInvoke(String name, MethodInfo getter) {
		if (getter.paramTypes.length != 0) throw new IllegalArgumentException("Getter requires parameters");
		return this.addField(getter.owner, name, new FieldHandler.Named("fieldInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> mode.makeInvoker(parser, receiver, getter)));
	}

	public MutableScriptEnvironment addFieldInvoke(MethodInfo getter) {
		return this.addFieldInvoke(getter.name, getter);
	}

	public MutableScriptEnvironment addFieldRenamedInvoke(String exposedName, Class<?> in, String actualName) {
		return this.addFieldInvoke(exposedName, MethodInfo.getMethod(in, actualName, NO_ARGS.and(IS_INSTANCE)));
	}

	public MutableScriptEnvironment addFieldInvoke(Class<?> in, String name) {
		return this.addFieldRenamedInvoke(name, in, name);
	}

	public MutableScriptEnvironment addFieldInvokes(Class<?> in, String... names) {
		for (String name : names) {
			this.addFieldInvoke(in, name);
		}
		return this;
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment addFieldInvokeStatic(String name, MethodInfo getter) {
		if (getter.paramTypes.length != 1) throw new IllegalArgumentException("Static getter requires parameters");
		return this.addField(getter.paramTypes[0], name, new FieldHandler.Named("fieldInvokeStatic: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> mode.makeInvoker(parser, receiver, getter)));
	}

	public MutableScriptEnvironment addFieldInvokeStatic(MethodInfo getter) {
		return this.addFieldInvokeStatic(getter.name, getter);
	}

	public MutableScriptEnvironment addFieldInvokeStatic(Class<?> in, String name) {
		return this.addFieldInvokeStatic(MethodInfo.getMethod(in, name, IS_STATIC));
	}

	public MutableScriptEnvironment addFieldInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addFieldInvokeStatic(in, name);
		}
		return this;
	}

	//////////////// getter setter ////////////////

	public MutableScriptEnvironment addFieldGetterSetter(TypeInfo owner, String name, MethodInfo getter, MethodInfo setter) {
		return this.addField(owner, name, new FieldHandler.Named(
			"getter: " + getter + ", setter: " + setter,
			(ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
				return mode.makeGetterSetter(parser, receiver, getter, setter);
			}
		));
	}

	public MutableScriptEnvironment addFieldGetterSetterInstance(Class<?> in, String exposedName, String internalName, Class<?> type) {
		MethodInfo getter = MethodInfo.findMethod(in, internalName, type);
		MethodInfo setter = MethodInfo.findMethod(in, internalName, void.class, type);
		return this.addFieldGetterSetter(type(in), exposedName, getter, setter);
	}

	public MutableScriptEnvironment addFieldGetterSetterInstance(Class<?> in, String name, Class<?> type) {
		return this.addFieldGetterSetterInstance(in, name, name, type);
	}

	public MutableScriptEnvironment addFieldGetterSetterStatic(Class<?> owner, Class<?> in, String exposedName, String internalName, Class<?> type) {
		MethodInfo getter = MethodInfo.findMethod(in, internalName, type, owner);
		MethodInfo setter = MethodInfo.findMethod(in, internalName, void.class, owner, type);
		return this.addFieldGetterSetter(type(owner), exposedName, getter, setter);
	}

	public MutableScriptEnvironment addFieldGetterSetterStatic(Class<?> owner, Class<?> in, String name, Class<?> type) {
		return this.addFieldGetterSetterStatic(owner, in, name, name, type);
	}

	//////////////////////////////// functions ////////////////////////////////

	public MutableScriptEnvironment addFunction(String name, FunctionHandler.Named functionHandler) {
		this.functions.computeIfAbsent(name, (String $) -> new ArrayList<>(8)).add(functionHandler);
		return this;
	}

	public MutableScriptEnvironment addFunctionNoArgs(String name, InsnTree tree) {
		return this.addFunction(name, new FunctionHandler.Named("noArgs: " + tree.describe(), (ExpressionParser parser, String name1, InsnTree... arguments) -> {
			return arguments.length == 0 ? new CastResult(tree, false) : null;
		}));
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment addFunctionInvokeStatic(String name, MethodInfo method) {
		return this.addFunction(name, new FunctionHandler.Named("functionInvokeStatic: " + method, (ExpressionParser parser, String name1, InsnTree... arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(invokeStatic(method, castArguments), castArguments != arguments);
		}));
	}

	public MutableScriptEnvironment addFunctionInvokeStatic(MethodInfo method) {
		return this.addFunctionInvokeStatic(method.name, method);
	}

	public MutableScriptEnvironment addFunctionRenamedInvokeStatic(String exposedName, Class<?> in, String actualName) {
		return this.addFunctionInvokeStatic(exposedName, MethodInfo.getMethod(in, actualName, IS_STATIC));
	}

	public MutableScriptEnvironment addFunctionInvokeStatic(Class<?> in, String name) {
		return this.addFunctionInvokeStatic(MethodInfo.getMethod(in, name, IS_STATIC));
	}

	public MutableScriptEnvironment addFunctionInvokeStatic(Class<?> in, String name, Class<?> returnType, Class<?>... parameterTypes) {
		return this.addFunctionInvokeStatic(MethodInfo.findMethod(in, name, returnType, parameterTypes));
	}

	public MutableScriptEnvironment addFunctionInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addFunctionInvokeStatic(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addFunctionMultiInvokeStatic(Class<?> in, String name) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(name)) {
			this.addFunctionInvokeStatic(name, MethodInfo.forMethod(method));
		}
		return this;
	}

	public MutableScriptEnvironment addFunctionMultiInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addFunctionMultiInvokeStatic(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addFunctionRenamedMultiInvokeStatic(String exposedName, Class<?> in, String actualName) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(actualName)) {
			this.addFunctionInvokeStatic(exposedName, MethodInfo.forMethod(method));
		}
		return this;
	}

	//////////////// invoke ////////////////

	public MutableScriptEnvironment addFunctionInvoke(String name, InsnTree receiver, MethodInfo method) {
		return this.addFunction(name, new FunctionHandler.Named("functionInvoke: " + method + " for receiver " + receiver.describe(), (ExpressionParser parser, String name1, InsnTree... arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(invokeInstance(receiver, method, castArguments), castArguments != arguments);
		}));
	}

	public MutableScriptEnvironment addFunctionInvoke(InsnTree receiver, MethodInfo method) {
		return this.addFunctionInvoke(method.name, receiver, method);
	}

	public MutableScriptEnvironment addFunctionInvoke(InsnTree receiver, Class<?> in, String name) {
		return this.addFunctionInvoke(name, receiver, MethodInfo.getMethod(in, name, IS_INSTANCE));
	}

	public MutableScriptEnvironment addFunctionInvoke(InsnTree receiver, Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addFunctionInvoke(name, receiver, MethodInfo.findMethod(in, name, returnType, paramTypes));
	}

	public MutableScriptEnvironment addFunctionInvokes(InsnTree receiver, Class<?> in, String... names) {
		for (String name : names) {
			this.addFunctionInvoke(receiver, in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addFunctionMultiInvoke(InsnTree receiver, Class<?> in, String name) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(name)) {
			this.addFunctionInvoke(receiver, MethodInfo.forMethod(method));
		}
		return this;
	}

	public MutableScriptEnvironment addFunctionMultiInvokes(InsnTree receiver, Class<?> in, String... names) {
		for (String name : names) {
			this.addFunctionMultiInvoke(receiver, in, name);
		}
		return this;
	}

	//////////////////////////////// methods ////////////////////////////////

	public MutableScriptEnvironment addMethod(TypeInfo owner, String name, MethodHandler.Named methodHandler) {
		this.methods.computeIfAbsent(new NamedType(owner, name), (NamedType $) -> new ArrayList<>(8)).add(methodHandler);
		return this;
	}

	//////////////// invoke ////////////////

	public MutableScriptEnvironment addMethodInvoke(String name, MethodInfo method) {
		return this.addMethod(method.owner, name, new MethodHandler.Named("methodInvoke: " + method, (ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(mode.makeInvoker(parser, receiver, method, castArguments), castArguments != arguments);
		}));
	}

	public MutableScriptEnvironment addMethodInvoke(MethodInfo method) {
		return this.addMethodInvoke(method.name, method);
	}

	public MutableScriptEnvironment addMethodInvoke(Class<?> in, String name) {
		return this.addMethodInvoke(name, MethodInfo.getMethod(in, name, IS_INSTANCE));
	}

	public MutableScriptEnvironment addMethodRenamedInvoke(String exposedName, Class<?> in, String actualName) {
		return this.addMethodInvoke(exposedName, MethodInfo.getMethod(in, actualName, IS_INSTANCE));
	}

	public MutableScriptEnvironment addMethodRenamedInvokeSpecific(String exposedName, Class<?> in, String actualName, Class<?> returnType, Class<?> paramTypes) {
		return this.addMethodInvoke(exposedName, MethodInfo.findMethod(in, actualName, returnType, paramTypes));
	}

	public MutableScriptEnvironment addMethodInvokes(Class<?> in, String... names) {
		for (String name : names) {
			this.addMethodInvoke(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addMethodMultiInvoke(Class<?> in, String name) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(name)) {
			this.addMethodInvoke(name, MethodInfo.forMethod(method));
		}
		return this;
	}

	public MutableScriptEnvironment addMethodMultiInvokes(Class<?> in, String... names) {
		for (String name : names) {
			this.addMethodMultiInvoke(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addMethodInvokeSpecific(Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addMethodInvoke(name, MethodInfo.findMethod(in, name, returnType, paramTypes));
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment addMethodInvokeStatic(String name, MethodInfo method) {
		return this.addMethod(method.paramTypes[0], name, new MethodHandler.Named("methodInvokeStatic: " + method, (ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
			InsnTree[] concatArguments = ObjectArrays.concat(receiver, arguments);
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, concatArguments);
			return castArguments == null ? null : new CastResult(mode.makeInvoker(parser, method, castArguments), castArguments != concatArguments);
		}));
	}

	public MutableScriptEnvironment addMethodInvokeStatic(MethodInfo method) {
		return this.addMethodInvokeStatic(method.name, method);
	}

	public MutableScriptEnvironment addMethodRenamedInvokeStatic(String exposedName, Class<?> in, String actualName) {
		return this.addMethodInvokeStatic(exposedName, MethodInfo.getMethod(in, actualName, IS_STATIC));
	}

	public MutableScriptEnvironment addMethodInvokeStatic(Class<?> in, String name) {
		return this.addMethodRenamedInvokeStatic(name, in, name);
	}

	public MutableScriptEnvironment addMethodInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addMethodInvokeStatic(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addMethodInvokeStaticSpecific(Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addMethodInvokeStatic(MethodInfo.findMethod(in, name, returnType, paramTypes));
	}

	public MutableScriptEnvironment addMethodRenamedMultiInvokeStatic(String exposedName, Class<?> in, String actualName) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(actualName)) {
			this.addMethodInvokeStatic(exposedName, MethodInfo.forMethod(method));
		}
		return this;
	}

	public MutableScriptEnvironment addMethodMultiInvokeStatic(Class<?> in, String name) {
		return this.addMethodRenamedMultiInvokeStatic(name, in, name);
	}

	public MutableScriptEnvironment addMethodMultiInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addMethodMultiInvokeStatic(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addMethodRenamedInvokeStaticSpecific(String exposedName, Class<?> in, String actualName, Class<?> returnType, Class<?>... paramTypes) {
		return this.addMethodInvokeStatic(exposedName, MethodInfo.findMethod(in, actualName, returnType, paramTypes));
	}

	//////////////////////////////// types ////////////////////////////////

	public MutableScriptEnvironment addType(String name, TypeInfo type) {
		if (this.types.putIfAbsent(name, type) != null) {
			throw new IllegalArgumentException("Type " + name + " is already defined in this scope");
		}
		return this;
	}

	public MutableScriptEnvironment addType(String name, Class<?> type) {
		return this.addType(name, TypeInfo.of(type));
	}

	//////////////////////////////// qualified variables ////////////////////////////////

	public MutableScriptEnvironment addQualifiedVariable(TypeInfo owner, String name, VariableHandler variableHandler) {
		return this.addField(TypeInfos.CLASS, name, new FieldHandler.Named("qualifiedVariable on " + owner + ": " + variableHandler, (ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(owner)) {
				return variableHandler.create(parser, name1);
			}
			return null;
		}));
	}

	public MutableScriptEnvironment addQualifiedVariable(TypeInfo owner, String name, InsnTree tree) {
		return this.addQualifiedVariable(owner, name, new VariableHandler.Named(tree.describe(), (ExpressionParser parser, String name1) -> tree));
	}

	//////////////// getStatic ////////////////

	public MutableScriptEnvironment addQualifiedVariableGetStatic(TypeInfo owner, String name, FieldInfo field) {
		return this.addQualifiedVariable(owner, name, getStatic(field));
	}

	public MutableScriptEnvironment addQualifiedVariableGetStatic(String name, FieldInfo field) {
		return this.addQualifiedVariable(field.owner, name, getStatic(field));
	}

	public MutableScriptEnvironment addQualifiedVariableGetStatic(TypeInfo owner, FieldInfo field) {
		return this.addQualifiedVariable(owner, field.name, getStatic(field));
	}

	public MutableScriptEnvironment addQualifiedVariableGetStatic(FieldInfo field) {
		return this.addQualifiedVariable(field.owner, field.name, getStatic(field));
	}

	public MutableScriptEnvironment addQualifiedVariableGetStatic(TypeInfo owner, Class<?> in, String name) {
		return this.addQualifiedVariableGetStatic(owner, name, FieldInfo.getField(in, name));
	}

	public MutableScriptEnvironment addQualifiedVariableGetStatic(Class<?> in, String name) {
		return this.addQualifiedVariableGetStatic(TypeInfo.of(in), in, name);
	}

	public MutableScriptEnvironment addQualifiedVariableGetStatics(TypeInfo owner, Class<?> in, String... names) {
		for (String name : names) {
			this.addQualifiedVariableGetStatic(owner, in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addQualifiedVariableGetStatics(Class<?> in, String... names) {
		return this.addQualifiedVariableGetStatics(TypeInfo.of(in), in, names);
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment addQualifiedVariableInvokeStatic(TypeInfo owner, String name, MethodInfo method) {
		if (method.paramTypes.length != 0) throw new IllegalArgumentException("Qualified static getter requires parameters");
		return this.addQualifiedVariable(owner, name, invokeStatic(method));
	}

	public MutableScriptEnvironment addQualifiedVariableInvokeStatic(TypeInfo owner, MethodInfo method) {
		return this.addQualifiedVariableInvokeStatic(owner, method.name, method);
	}

	public MutableScriptEnvironment addQualifiedVariableInvokeStatic(String name, MethodInfo method) {
		return this.addQualifiedVariableInvokeStatic(method.owner, name, method);
	}

	public MutableScriptEnvironment addQualifiedVariableInvokeStatic(MethodInfo method) {
		return this.addQualifiedVariableInvokeStatic(method.owner, method.name, method);
	}

	public MutableScriptEnvironment addQualifiedVariableInvokeStatic(TypeInfo owner, Class<?> in, String name) {
		return this.addQualifiedVariableInvokeStatic(owner, name, MethodInfo.getMethod(in, name, IS_STATIC));
	}

	public MutableScriptEnvironment addQualifiedVariableInvokeStatic(Class<?> in, String name) {
		return this.addQualifiedVariableInvokeStatic(TypeInfo.of(in), in, name);
	}

	public MutableScriptEnvironment addQualifiedVariableInvokeStatics(TypeInfo owner, Class<?> in, String... names) {
		for (String name : names) {
			return this.addQualifiedVariableInvokeStatic(owner, in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addQualifiedVariableInvokeStatics(Class<?> in, String... names) {
		return this.addQualifiedVariableInvokeStatics(TypeInfo.of(in), in, names);
	}

	//////////////////////////////// qualified functions ////////////////////////////////

	public MutableScriptEnvironment addQualifiedFunction(TypeInfo owner, String name, FunctionHandler functionHandler) {
		return this.addMethod(TypeInfos.CLASS, name, new MethodHandler.Named("qualifiedFunction on " + owner + ": " + functionHandler, (ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(owner)) {
				return functionHandler.create(parser, name1, arguments);
			}
			return null;
		}));
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(TypeInfo owner, String name, MethodInfo method) {
		return this.addQualifiedFunction(owner, name, new FunctionHandler.Named("invokeStatic: " + method, (ExpressionParser parser, String name1, InsnTree... arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(invokeStatic(method, castArguments), castArguments != arguments);
		}));
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(String name, MethodInfo method) {
		return this.addQualifiedFunctionInvokeStatic(method.owner, name, method);
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(TypeInfo owner, MethodInfo method) {
		return this.addQualifiedFunctionInvokeStatic(owner, method.name, method);
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(MethodInfo method) {
		return this.addQualifiedFunctionInvokeStatic(method.owner, method.name, method);
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(TypeInfo owner, Class<?> in, String name) {
		return this.addQualifiedFunctionInvokeStatic(owner, MethodInfo.getMethod(in, name, IS_STATIC));
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(Class<?> in, String name) {
		return this.addQualifiedFunctionInvokeStatic(TypeInfo.of(in), MethodInfo.getMethod(in, name, IS_STATIC));
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(TypeInfo owner, Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addQualifiedFunctionInvokeStatic(owner, MethodInfo.findMethod(in, name, returnType, paramTypes));
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addQualifiedFunctionInvokeStatic(TypeInfo.of(in), MethodInfo.findMethod(in, name, returnType, paramTypes));
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatics(TypeInfo owner, Class<?> in, String... names) {
		for (String name : names) {
			this.addQualifiedFunctionInvokeStatic(owner, in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addQualifiedFunctionInvokeStatic(in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addQualifiedFunctionMultiInvokeStatic(TypeInfo owner, Class<?> in, String name) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(name)) {
			this.addQualifiedFunctionInvokeStatic(owner, name, MethodInfo.forMethod(method));
		}
		return this;
	}

	public MutableScriptEnvironment addQualifiedFunctionMultiInvokeStatic(Class<?> in, String name) {
		return this.addQualifiedFunctionMultiInvokeStatic(TypeInfo.of(in), in, name);
	}

	public MutableScriptEnvironment addQualifiedFunctionMultiInvokeStatics(TypeInfo owner, Class<?> in, String... names) {
		for (String name : names) {
			this.addQualifiedFunctionMultiInvokeStatic(owner, in, name);
		}
		return this;
	}

	public MutableScriptEnvironment addQualifiedFunctionMultiInvokeStatics(Class<?> in, String... names) {
		return this.addQualifiedFunctionMultiInvokeStatics(TypeInfo.of(in), in, names);
	}

	public MutableScriptEnvironment addQualifiedFunctionRenamedMultiInvokeStatic(TypeInfo owner, Class<?> in, String exposedName, String internalName) {
		for (Method method : ReflectionData.forClass(in).getDeclaredMethods(internalName)) {
			this.addQualifiedFunctionInvokeStatic(owner, exposedName, MethodInfo.forMethod(method));
		}
		return this;
	}

	//////////////// invoke ////////////////

	//todo: finish this section.

	//////////////// constructor ////////////////

	public MutableScriptEnvironment addQualifiedConstructor(MethodInfo constructor) {
		return this.addQualifiedFunction(constructor.owner, "new", new FunctionHandler.Named("constructor: " + constructor, (ExpressionParser parser, String name, InsnTree... arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, constructor, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(newInstance(constructor, castArguments), castArguments != arguments);
		}));
	}

	public MutableScriptEnvironment addQualifiedConstructor(Class<?> in) {
		return this.addQualifiedConstructor(MethodInfo.getConstructor(in));
	}

	public MutableScriptEnvironment addQualifiedSpecificConstructor(Class<?> in, Class<?>... parameterTypes) {
		return this.addQualifiedConstructor(MethodInfo.findConstructor(in, parameterTypes));
	}

	public MutableScriptEnvironment addQualifiedMultiConstructor(Class<?> in) {
		for (Constructor<?> constructor : ReflectionData.forClass(in).getConstructors()) {
			this.addQualifiedConstructor(MethodInfo.forConstructor(constructor));
		}
		return this;
	}

	//////////////////////////////// enums ////////////////////////////////

	public <E extends Enum<E>> MutableScriptEnvironment addEnum(Class<E> enumClass) {
		return this.addEnum(enumClass.getSimpleName(), enumClass, E::name);
	}

	public <E extends Enum<E>> MutableScriptEnvironment addEnum(String name, Class<E> enumClass) {
		return this.addEnum(name, enumClass, E::name);
	}

	public <E extends Enum<E>> MutableScriptEnvironment addEnum(String name, Class<E> enumClass, Function<E, String> nameGetter) {
		this.addType(name, enumClass);
		this.addCastConstant(FieldConstantFactory.forEnum(enumClass, nameGetter), true);
		return this;
	}

	//////////////////////////////// keywords ////////////////////////////////

	public MutableScriptEnvironment addKeyword(String name, KeywordHandler.Named keywordHandler) {
		if (this.keywords.putIfAbsent(name, keywordHandler) != null) {
			throw new IllegalArgumentException("Keyword " + name + " is already defined in this scope");
		}
		return this;
	}

	public MutableScriptEnvironment addMemberKeyword(TypeInfo type, String name, MemberKeywordHandler.Named memberKeywordHandler) {
		this.memberKeywords.computeIfAbsent(new NamedType(type, name), (NamedType namedType) -> new ArrayList<>(1)).add(memberKeywordHandler);
		return this;
	}

	//////////////////////////////// casting ////////////////////////////////

	public MutableScriptEnvironment addCast(TypeInfo from, TypeInfo to, boolean implicit, CastHandler castHandler) {
		while (castHandler instanceof CastHandlerHolder holder) {
			castHandler = holder.caster;
		}
		CastHandlerHolder holder = new CastHandlerHolder(implicit, castHandler);
		if (this.casters.computeIfAbsent(from, (TypeInfo $) -> new HashMap<>(8)).putIfAbsent(to, holder) != null) {
			throw new IllegalArgumentException("Caster " + from + " -> " + to + " is already present in this environment");
		}
		return this;
	}

	public MutableScriptEnvironment addCast(CastHandlerData caster) {
		return this.addCast(caster.from, caster.to, caster.implicit, caster);
	}

	public MutableScriptEnvironment addCasts(CastHandlerData... casters) {
		return this.addCast(casters[0].from, casters[casters.length - 1].to, Arrays.stream(casters).allMatch((CastHandlerData caster) -> caster.implicit), CastingSupport.allOf(casters));
	}

	public MutableScriptEnvironment addCastInvoke(MethodInfo method, boolean implicit) {
		return this.addCast(method.owner, method.returnType, implicit, CastingSupport.invokeVirtual(method));
	}

	public MutableScriptEnvironment addCastInvoke(Class<?> in, String name, boolean implicit) {
		return this.addCastInvoke(MethodInfo.getMethod(in, name, IS_INSTANCE), implicit);
	}

	public MutableScriptEnvironment addCastInvokeStatic(MethodInfo method, boolean implicit) {
		return this.addCast(method.paramTypes[0], method.returnType, implicit, CastingSupport.invokeStatic(method));
	}

	public MutableScriptEnvironment addCastInvokeStatic(Class<?> in, String name, boolean implicit) {
		return this.addCastInvokeStatic(MethodInfo.getMethod(in, name, IS_STATIC), implicit);
	}

	public MutableScriptEnvironment addCastInvokeStatic(Class<?> in, String name, boolean implicit, Class<?> returnType, Class<?>... paramTypes) {
		return this.addCastInvokeStatic(MethodInfo.findMethod(in, name, returnType, paramTypes), implicit);
	}

	public MutableScriptEnvironment addCastOpcode(TypeInfo from, TypeInfo to, boolean implicit, int opcode) {
		return this.addCast(from, to, implicit, (ExpressionParser parser, InsnTree value, TypeInfo to_, boolean implicit_) -> new OpcodeCastInsnTree(value, opcode, to_));
	}

	public MutableScriptEnvironment addCastIdentity(TypeInfo from, TypeInfo to, boolean implicit) {
		return this.addCast(from, to, implicit, (ExpressionParser parser, InsnTree value, TypeInfo to_, boolean implicit_) -> new IdentityCastInsnTree(value, to_));
	}

	public MutableScriptEnvironment addCastConstant(AbstractConstantFactory factory, boolean implicit) {
		return this.addCast(factory.inType, factory.outType, implicit, (ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit_) -> factory.create(parser, value, implicit_).tree);
	}

	//////////////////////////////// getters ////////////////////////////////

	public static record InsnTreeSource(InsnTree tree, Object source) {

		public static InsnTree get(ExpressionParser parser, InsnTree[] arguments, List<InsnTreeSource> sources) throws ScriptParsingException {
			return switch (sources.size()) {
				case 0 -> null;
				case 1 -> sources.get(0).tree;
				default -> throw new ScriptParsingException(
					"Ambiguous call matches the following:\n\t" +
					sources
					.stream()
					.map(InsnTreeSource::source)
					.map(Object::toString)
					.collect(Collectors.joining("\n\t")) +
					"\nActual form: " +
					Arrays
					.stream(arguments)
					.map(InsnTree::describe)
					.collect(Collectors.joining(", ", "(", ")")),

					parser.input
				);
			};
		}
	}

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		VariableHandler handler;
		InsnTree result;
		if ((handler = this.variables.get(name)) != null && (result = handler.create(parser, name)) != null) return result;
		if ((handler = this.variables.get(null)) != null && (result = handler.create(parser, name)) != null) return result;
		return null;
	}

	@Override
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) throws ScriptParsingException {
		class Accumulator {

			public final NamedType query = new NamedType();
			public InsnTree result;

			public boolean update(String n, TypeInfo type) throws ScriptParsingException {
				this.query.name = n;
				this.query.owner = type;
				FieldHandler handler = MutableScriptEnvironment.this.fields.get(this.query);
				return handler != null && (this.result = handler.create(parser, receiver, name, mode)) != null;
			}
		}
		Accumulator accumulator = new Accumulator();
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			if (accumulator.update(name, owner)) return accumulator.result;
			if (accumulator.update(null, owner)) return accumulator.result;
		}
		if (accumulator.update(name, null)) return accumulator.result;
		if (accumulator.update(null, null)) return accumulator.result;
		return accumulator.result;
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		class Accumulator {

			public final List<InsnTreeSource> results = new ArrayList<>(8);
			public boolean exact;

			public boolean update(String n) throws ScriptParsingException {
				List<FunctionHandler.Named> handlers = MutableScriptEnvironment.this.functions.get(n);
				if (handlers != null) {
					boolean exact = false;
					for (int index = 0, size = handlers.size(); index < size; index++) {
						CastResult casted = handlers.get(index).create(parser, name, arguments);
						if (casted != null) {
							if (!this.exact && !casted.requiredCasting) {
								this.results.clear();
								this.exact = true;
							}
							if (this.exact ? !casted.requiredCasting : true) {
								this.results.add(new InsnTreeSource(casted.tree, handlers.get(index)));
							}
						}
					}
				}
				return !this.results.isEmpty();
			}

			public InsnTree result() throws ScriptParsingException {
				return InsnTreeSource.get(parser, arguments, this.results);
			}
		}
		Accumulator accumulator = new Accumulator();
		if (accumulator.update(name)) return accumulator.result();
		if (accumulator.update(null)) return accumulator.result();
		return accumulator.result();
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) throws ScriptParsingException {
		class Accumulator {

			public final NamedType query = new NamedType();
			public final List<InsnTreeSource> results = new ArrayList<>(8);
			public boolean exact;

			public boolean update(String n, TypeInfo type) throws ScriptParsingException {
				this.query.name = n;
				this.query.owner = type;
				List<MethodHandler.Named> handlers = MutableScriptEnvironment.this.methods.get(this.query);
				if (handlers != null) {
					for (int index = 0, size = handlers.size(); index < size; index++) {
						CastResult casted = handlers.get(index).create(parser, receiver, name, mode, arguments);
						if (casted != null) {
							if (!this.exact && !casted.requiredCasting) {
								this.results.clear();
								this.exact = true;
							}
							if (this.exact ? !casted.requiredCasting : true) {
								this.results.add(new InsnTreeSource(casted.tree, handlers.get(index)));
							}
						}
					}
				}
				return !this.results.isEmpty();
			}

			public InsnTree result() throws ScriptParsingException {
				return InsnTreeSource.get(parser, arguments, this.results);
			}
		}
		Accumulator accumulator = new Accumulator();
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			if (accumulator.update(name, owner)) return accumulator.result();
			if (accumulator.update(null, owner)) return accumulator.result();
		}
		if (accumulator.update(name, null)) return accumulator.result();
		if (accumulator.update(null, null)) return accumulator.result();
		return accumulator.result();
	}

	@Override
	public @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		return this.types.get(name);
	}

	@Override
	public @Nullable InsnTree parseKeyword(ExpressionParser parser, String name) throws ScriptParsingException {
		KeywordHandler handler = this.keywords.get(name);
		return handler == null ? null : handler.create(parser, name);
	}

	@Override
	public @Nullable InsnTree parseMemberKeyword(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) throws ScriptParsingException {
		NamedType query = new NamedType();
		query.name = name;
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			query.owner = owner;
			List<MemberKeywordHandler.Named> handlers = this.memberKeywords.get(query);
			if (handlers != null) {
				for (MemberKeywordHandler.Named handler : handlers) {
					InsnTree result = handler.create(parser, receiver, name, mode);
					if (result != null) return result;
				}
			}
		}
		query.owner = null;
		List<MemberKeywordHandler.Named> handlers = this.memberKeywords.get(query);
		if (handlers != null) {
			for (MemberKeywordHandler.Named handler : handlers) {
				InsnTree result = handler.create(parser, receiver, name, mode);
				if (result != null) return result;
			}
		}
		return null;
	}

	@Override
	public @Nullable InsnTree cast(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit) {
		for (TypeInfo from : value.getTypeInfo().getAllAssignableTypes()) {
			for (Map.Entry<TypeInfo, CastHandlerHolder> entry : this.casters.getOrDefault(from, Collections.emptyMap()).entrySet()) {
				if ((!implicit || entry.getValue().implicit) && entry.getKey().extendsOrImplements(to)) {
					return entry.getValue().cast(parser, value, to, implicit);
				}
			}
		}
		return null;
	}

	//////////////////////////////// handlers ////////////////////////////////

	public static record CastResult(InsnTree tree, boolean requiredCasting) {}

	@FunctionalInterface
	public static interface VariableHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, String name) throws ScriptParsingException;

		public static record Named(String name, VariableHandler handler) implements VariableHandler {

			@Override
			public @Nullable InsnTree create(ExpressionParser parser, String name) throws ScriptParsingException {
				return this.handler.create(parser, name);
			}

			@Override
			public String toString() {
				return this.name;
			}
		}
	}

	@FunctionalInterface
	public static interface FieldHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) throws ScriptParsingException;

		public static record Named(String name, FieldHandler handler) implements FieldHandler {

			@Override
			public @Nullable InsnTree create(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) throws ScriptParsingException {
				return this.handler.create(parser, receiver, name, mode);
			}

			@Override
			public String toString() {
				return this.name;
			}
		}
	}

	@FunctionalInterface
	public static interface FunctionHandler {

		public abstract @Nullable CastResult create(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException;

		public static record Named(String name, FunctionHandler handler) implements FunctionHandler {

			@Override
			public @Nullable CastResult create(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
				return this.handler.create(parser, name, arguments);
			}

			@Override
			public String toString() {
				return this.name;
			}
		}
	}

	@FunctionalInterface
	public static interface MethodHandler {

		public abstract @Nullable CastResult create(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) throws ScriptParsingException;

		public static record Named(String name, MethodHandler handler) implements MethodHandler {

			@Override
			public @Nullable CastResult create(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) throws ScriptParsingException {
				return this.handler.create(parser, receiver, name, mode, arguments);
			}

			@Override
			public String toString() {
				return this.name;
			}
		}
	}

	@FunctionalInterface
	public static interface KeywordHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, String name) throws ScriptParsingException;

		public static record Named(String name, KeywordHandler handler) implements KeywordHandler {

			@Override
			public @Nullable InsnTree create(ExpressionParser parser, String name) throws ScriptParsingException {
				return this.handler.create(parser, name);
			}

			@Override
			public String toString() {
				return this.name;
			}
		}
	}

	@FunctionalInterface
	public static interface MemberKeywordHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) throws ScriptParsingException;

		public static record Named(String name, MemberKeywordHandler handler) implements MemberKeywordHandler {

			@Override
			public @Nullable InsnTree create(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) throws ScriptParsingException {
				return this.handler.create(parser, receiver, name, mode);
			}

			@Override
			public String toString() {
				return this.name;
			}
		}
	}

	@FunctionalInterface
	public static interface CastHandler {

		public abstract InsnTree cast(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit);

		public static record Named(String name, CastHandler handler) implements CastHandler {

			@Override
			public InsnTree cast(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit) {
				return this.handler.cast(parser, value, to, implicit);
			}

			@Override
			public String toString() {
				return this.name;
			}
		}
	}

	public static class NamedType {

		public @Nullable TypeInfo owner;
		public @Nullable String name;

		public NamedType() {}

		public NamedType(@Nullable TypeInfo owner, String name) {
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
			return (this.owner != null ? this.owner.getClassName() : "<any type>") + '.' + this.name;
		}
	}

	public static class CastHandlerHolder implements CastHandler {

		public CastHandler caster;
		public boolean implicit;

		public CastHandlerHolder(boolean implicit, CastHandler caster) {
			this.caster = caster;
			this.implicit = implicit;
		}

		@Override
		public InsnTree cast(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit) {
			return this.caster.cast(parser, value, to, implicit);
		}

		@Override
		public int hashCode() {
			return this.caster.hashCode() + Boolean.hashCode(this.implicit);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof CastHandlerHolder that &&
				this.caster.equals(that.caster) &&
				this.implicit == that.implicit
			);
		}

		@Override
		public String toString() {
			return this.caster + (this.implicit ? " (implicit)" : " (explicit)");
		}
	}

	public static class CastHandlerData extends CastHandlerHolder {

		public TypeInfo from, to;

		public CastHandlerData(TypeInfo from, TypeInfo to, boolean implicit, CastHandler caster) {
			super(implicit, caster);
			this.from = from;
			this.to = to;
		}

		public CastHandlerData changeInput(TypeInfo from) {
			return new CastHandlerData(from, this.to, this.implicit, this.caster);
		}

		public CastHandlerData changeOutput(TypeInfo to) {
			return new CastHandlerData(this.from, to, this.implicit, this.caster);
		}

		@Override
		public InsnTree cast(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit) {
			if (!value.getTypeInfo().equals(this.from)) {
				throw new IllegalArgumentException(this + " attempting to cast value of type " + value.getTypeInfo());
			}
			if (!to.equals(this.to)) {
				throw new IllegalArgumentException(this + " attempting to cast value to type " + to);
			}
			value = this.caster.cast(parser, value, to, implicit);
			if (!value.getTypeInfo().equals(this.to)) {
				throw new IllegalArgumentException(this + " cast value to incorrect type " + value.getTypeInfo());
			}
			return value;
		}

		@Override
		public int hashCode() {
			int hash = this.caster.hashCode();
			hash = hash * 31 + Boolean.hashCode(this.implicit);
			hash = hash * 31 + this.from.hashCode();
			hash = hash * 31 + this.to.hashCode();
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof CastHandlerData that &&
				this.caster.equals(that.caster) &&
				this.implicit == that.implicit &&
				this.from.equals(that.from) &&
				this.to.equals(that.to)
			);
		}

		@Override
		public String toString() {
			return this.caster + ": " + this.from + " -> " + this.to + (this.implicit ? " (implicit)" : " (explicit)");
		}
	}

	public static class MultiCastHandler implements CastHandler {

		public CastHandlerData[] casters;

		public MultiCastHandler(CastHandlerData... casters) {
			this.casters = casters;
		}

		@Override
		public InsnTree cast(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit) {
			for (CastHandlerData caster : this.casters) {
				value = caster.cast(parser, value, caster.to, implicit);
			}
			return value;
		}
	}
}