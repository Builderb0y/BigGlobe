package builderb0y.scripting.environments;

import java.lang.reflect.Constructor;
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

@SuppressWarnings({ "unused", "UnusedReturnValue" })
public class MutableScriptEnvironment implements ScriptEnvironment {

	public Map<String,    VariableHandler      > variables      = new HashMap<>(16);
	public Map<NamedType, FieldHandler         > fields         = new HashMap<>(16);
	public Map<String,    List<FunctionHandler>> functions      = new HashMap<>(16);
	public Map<NamedType, List<  MethodHandler>> methods        = new HashMap<>(16);
	public Map<String,    TypeInfo             > types          = new HashMap<>( 8);
	public Map<String,    KeywordHandler       > keywords       = new HashMap<>( 8);
	public Map<NamedType, MemberKeywordHandler > memberKeywords = new HashMap<>( 8);

	public MutableScriptEnvironment addAllVariables(MutableScriptEnvironment that) {
		for (Map.Entry<String, VariableHandler> entry : that.variables.entrySet()) {
			if (this.variables.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
				throw new IllegalArgumentException(entry.getKey() + " is already defined in this scope");
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllFields(MutableScriptEnvironment that) {
		for (Map.Entry<NamedType, FieldHandler> entry : that.fields.entrySet()) {
			if (this.fields.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
				throw new IllegalArgumentException(entry.getKey() + " is already defined in this scope");
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllFunctions(MutableScriptEnvironment that) {
		for (Map.Entry<String, List<FunctionHandler>> entry : that.functions.entrySet()) {
			List<FunctionHandler> handlers = this.functions.get(entry.getKey());
			if (handlers != null) handlers.addAll(entry.getValue());
			else this.functions.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return this;
	}

	public MutableScriptEnvironment addAllMethods(MutableScriptEnvironment that) {
		for (Map.Entry<NamedType, List<MethodHandler>> entry : that.methods.entrySet()) {
			List<MethodHandler> handlers = this.methods.get(entry.getKey());
			if (handlers != null) handlers.addAll(entry.getValue());
			else this.methods.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return this;
	}

	public MutableScriptEnvironment addAllTypes(MutableScriptEnvironment that) {
		for (Map.Entry<String, TypeInfo> entry : that.types.entrySet()) {
			if (this.types.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
				throw new IllegalArgumentException(entry.getKey() + " is already defined in this scope");
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllKeywords(MutableScriptEnvironment that) {
		for (Map.Entry<String, KeywordHandler> entry : that.keywords.entrySet()) {
			if (this.keywords.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
				throw new IllegalArgumentException(entry.getKey() + " is already defined in this scope");
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAllMemberKeywords(MutableScriptEnvironment that) {
		for (Map.Entry<NamedType, MemberKeywordHandler> entry : that.memberKeywords.entrySet()) {
			if (this.memberKeywords.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
				throw new IllegalArgumentException(entry.getKey() + " is already defined in this scope");
			}
		}
		return this;
	}

	public MutableScriptEnvironment addAll(MutableScriptEnvironment that) {
		return (
			this
			.addAllVariables(that)
			.addAllFields   (that)
			.addAllFunctions(that)
			.addAllMethods  (that)
			.addAllTypes    (that)
			.addAllKeywords(that)
			.addAllMemberKeywords(that)
		);
	}

	public MutableScriptEnvironment multiAddAll(MutableScriptEnvironment... environments) {
		for (MutableScriptEnvironment environment : environments) {
			this.addAll(environment);
		}
		return this;
	}

	//////////////////////////////// variables ////////////////////////////////

	public MutableScriptEnvironment addVariable(String name, VariableHandler variableHandler) {
		if (this.variables.putIfAbsent(name, variableHandler) != null) {
			throw new IllegalArgumentException(name + " is already defined in this scope");
		}
		return this;
	}

	public MutableScriptEnvironment addVariable(String name, InsnTree tree) {
		return this.addVariable(name, (parser, name1) -> tree);
	}

	//////////////// load ////////////////

	public MutableScriptEnvironment addVariableLoad(String name, VarInfo variable) {
		return this.addVariable(name, load(variable));
	}

	public MutableScriptEnvironment addVariableLoad(VarInfo variable) {
		return this.addVariable(variable.name, load(variable));
	}

	public MutableScriptEnvironment addVariableLoad(String name, int index, TypeInfo type) {
		return this.addVariable(name, load(name, index, type));
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
		return this.addVariable(name, invokeVirtualOrInterface(receiver, method));
	}

	public MutableScriptEnvironment addVariableInvoke(InsnTree receiver, MethodInfo method) {
		return this.addVariable(method.name, invokeVirtualOrInterface(receiver, method));
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
		return this.addVariableInvokeStatic(getterName, MethodInfo.forMethod(ReflectionData.forClass(in).findDeclaredMethod(getterName, m -> m.getParameterCount() == 0)));
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

	public MutableScriptEnvironment addField(TypeInfo owner, String name, FieldHandler fieldHandler) {
		if (this.fields.putIfAbsent(new NamedType(owner, name), fieldHandler) != null) {
			throw new IllegalArgumentException(owner + "." + name + " is already defined in this scope");
		}
		return this;
	}

	//////////////// get ////////////////

	public MutableScriptEnvironment addFieldGet(String name, FieldInfo field) {
		return this.addField(field.owner, name, (parser, receiver, name1) -> InsnTrees.getField(receiver, field));
	}

	public MutableScriptEnvironment addFieldGet(FieldInfo field) {
		return this.addField(field.owner, field.name, (parser, receiver, name1) -> InsnTrees.getField(receiver, field));
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
		return this.addField(getter.owner, name, (parser, receiver, name1) -> invokeVirtualOrInterface(receiver, getter));
	}

	public MutableScriptEnvironment addFieldInvoke(MethodInfo getter) {
		return this.addFieldInvoke(getter.name, getter);
	}

	public MutableScriptEnvironment addFieldInvoke(Class<?> in, String name) {
		return this.addFieldInvoke(name, MethodInfo.forMethod(ReflectionData.forClass(in).findDeclaredMethod(name, m -> m.getParameterCount() == 0)));
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
		return this.addField(getter.paramTypes[0], name, (parser, receiver, name1) -> invokeStatic(getter, receiver));
	}

	public MutableScriptEnvironment addFieldInvokeStatic(MethodInfo getter) {
		return this.addFieldInvokeStatic(getter.name, getter);
	}

	public MutableScriptEnvironment addFieldInvokeStatic(Class<?> in, String name) {
		return this.addFieldInvokeStatic(MethodInfo.forMethod(ReflectionData.forClass(in).getDeclaredMethod(name)));
	}

	public MutableScriptEnvironment addFieldInvokeStatics(Class<?> in, String... names) {
		for (String name : names) {
			this.addFieldInvokeStatic(in, name);
		}
		return this;
	}

	//////////////////////////////// functions ////////////////////////////////

	public MutableScriptEnvironment addFunction(String name, FunctionHandler functionHandler) {
		this.functions.computeIfAbsent(name, $ -> new ArrayList<>(8)).add(functionHandler);
		return this;
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment addFunctionInvokeStatic(String name, MethodInfo method) {
		return this.addFunction(name, (parser, name1, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(invokeStatic(method, castArguments), castArguments != arguments);
		});
	}

	public MutableScriptEnvironment addFunctionInvokeStatic(MethodInfo method) {
		return this.addFunctionInvokeStatic(method.name, method);
	}

	public MutableScriptEnvironment addFunctionRenamedInvokeStatic(String exposedName, Class<?> in, String actualName) {
		return this.addFunctionInvokeStatic(exposedName, MethodInfo.getMethod(in, actualName));
	}

	public MutableScriptEnvironment addFunctionInvokeStatic(Class<?> in, String name) {
		return this.addFunctionInvokeStatic(MethodInfo.getMethod(in, name));
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
		return this.addFunction(name, (parser, name1, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(invokeVirtualOrInterface(receiver, method, castArguments), castArguments != arguments);
		});
	}

	public MutableScriptEnvironment addFunctionInvoke(InsnTree receiver, MethodInfo method) {
		return this.addFunctionInvoke(method.name, receiver, method);
	}

	public MutableScriptEnvironment addFunctionInvoke(InsnTree receiver, Class<?> in, String name) {
		return this.addFunctionInvoke(name, receiver, MethodInfo.getMethod(in, name));
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

	public MutableScriptEnvironment addMethod(TypeInfo owner, String name, MethodHandler methodHandler) {
		this.methods.computeIfAbsent(new NamedType(owner, name), $ -> new ArrayList<>(8)).add(methodHandler);
		return this;
	}

	//////////////// invoke ////////////////

	public MutableScriptEnvironment addMethodInvoke(String name, MethodInfo method) {
		return this.addMethod(method.owner, name, (parser, receiver, name1, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(invokeVirtualOrInterface(receiver, method, castArguments), castArguments != arguments);
		});
	}

	public MutableScriptEnvironment addMethodInvoke(MethodInfo method) {
		return this.addMethodInvoke(method.name, method);
	}

	public MutableScriptEnvironment addMethodInvoke(Class<?> in, String name) {
		return this.addMethodInvoke(name, MethodInfo.getMethod(in, name));
	}

	public MutableScriptEnvironment addMethodRenamedInvoke(String exposedName, Class<?> in, String actualName) {
		return this.addMethodInvoke(exposedName, MethodInfo.getMethod(in, actualName));
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
		return this.addMethod(method.paramTypes[0], name, (parser, receiver, name1, arguments) -> {
			InsnTree[] concatArguments = ObjectArrays.concat(receiver, arguments);
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, concatArguments);
			return castArguments == null ? null : new CastResult(invokeStatic(method, castArguments), castArguments != concatArguments);
		});
	}

	public MutableScriptEnvironment addMethodInvokeStatic(MethodInfo method) {
		return this.addMethodInvokeStatic(method.name, method);
	}

	public MutableScriptEnvironment addMethodRenamedInvokeStatic(String exposedName, Class<?> in, String actualName) {
		return this.addMethodInvokeStatic(exposedName, MethodInfo.getMethod(in, actualName));
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
		return this.addField(TypeInfos.CLASS, name, (parser, receiver, name1) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(owner)) {
				return variableHandler.create(parser, name1);
			}
			return null;
		});
	}

	public MutableScriptEnvironment addQualifiedVariable(TypeInfo owner, String name, InsnTree tree) {
		return this.addQualifiedVariable(owner, name, (parser, name1) -> tree);
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
		return this.addQualifiedVariableInvokeStatic(owner, name, MethodInfo.getMethod(in, name));
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
		return this.addMethod(TypeInfos.CLASS, name, (parser, receiver, name1, arguments) -> {
			ConstantValue constant = receiver.getConstantValue();
			if (constant.isConstant() && constant.asJavaObject().equals(owner)) {
				return functionHandler.create(parser, name1, arguments);
			}
			return null;
		});
	}

	//////////////// invokeStatic ////////////////

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(TypeInfo owner, String name, MethodInfo method) {
		return this.addQualifiedFunction(owner, name, (parser, name1, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(invokeStatic(method, castArguments), castArguments != arguments);
		});
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
		return this.addQualifiedFunctionInvokeStatic(owner, MethodInfo.getMethod(in, name));
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(Class<?> in, String name) {
		return this.addQualifiedFunctionInvokeStatic(TypeInfo.of(in), MethodInfo.getMethod(in, name));
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(TypeInfo owner, Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addQualifiedFunctionInvokeStatic(owner, MethodInfo.findMethod(in, name, returnType, paramTypes));
	}

	public MutableScriptEnvironment addQualifiedFunctionInvokeStatic(Class<?> in, String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.addQualifiedFunctionInvokeStatic(TypeInfo.of(in), MethodInfo.findMethod(in, name, returnType, paramTypes));
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

	//////////////// invoke ////////////////

	//todo: finish this section.

	//////////////// constructor ////////////////

	public MutableScriptEnvironment addQualifiedConstructor(MethodInfo constructor) {
		return this.addQualifiedFunction(constructor.owner, "new", (parser, name, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, constructor, CastMode.IMPLICIT_NULL, arguments);
			return castArguments == null ? null : new CastResult(newInstance(constructor, castArguments), castArguments != arguments);
		});
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

	//////////////////////////////// keywords ////////////////////////////////

	public MutableScriptEnvironment addKeyword(String name, KeywordHandler keywordHandler) {
		if (this.keywords.putIfAbsent(name, keywordHandler) != null) {
			throw new IllegalArgumentException("Keyword " + name + " is already defined in this scope");
		}
		return this;
	}

	public MutableScriptEnvironment addMemberKeyword(TypeInfo type, String name, MemberKeywordHandler memberKeywordHandler) {
		if (this.memberKeywords.putIfAbsent(new NamedType(type, name), memberKeywordHandler) != null) {
			throw new IllegalArgumentException("Member keyword " + type + '.' + name + " is already defined in this scope");
		}
		return this;
	}

	//////////////////////////////// getters ////////////////////////////////

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		VariableHandler handler = this.variables.get(name);
		return handler == null ? null : handler.create(parser, name);
	}

	@Override
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		NamedType query = new NamedType();
		query.name = name;
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			query.owner = owner;
			FieldHandler handler = this.fields.get(query);
			if (handler != null) return handler.create(parser, receiver, name);
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
					if (!casted.requiredCasting) {
						return casted.tree;
					}
					else if (result == null) {
						result = casted.tree;
					}
				}
			}
			return result;
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
			if (handlers != null) {
				InsnTree result = null;
				for (int index = 0, size = handlers.size(); index < size; index++) {
					CastResult casted = handlers.get(index).create(parser, receiver, name, arguments);
					if (casted != null) {
						if (!casted.requiredCasting) {
							return casted.tree;
						}
						else if (result == null) {
							result = casted.tree;
						}
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

	@Override
	public @Nullable InsnTree parseKeyword(ExpressionParser parser, String name) throws ScriptParsingException {
		KeywordHandler handler = this.keywords.get(name);
		return handler == null ? null : handler.create(parser, name);
	}

	@Override
	public @Nullable InsnTree parseMemberKeyword(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		NamedType query = new NamedType();
		query.name = name;
		for (TypeInfo owner : receiver.getTypeInfo().getAllAssignableTypes()) {
			query.owner = owner;
			MemberKeywordHandler handler = this.memberKeywords.get(query);
			if (handler != null) return handler.create(parser, receiver, name);
		}
		return null;
	}

	//////////////////////////////// handlers ////////////////////////////////

	public static record CastResult(InsnTree tree, boolean requiredCasting) {}

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

		public abstract @Nullable CastResult create(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface MethodHandler {

		public abstract @Nullable CastResult create(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface KeywordHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, String name) throws ScriptParsingException;
	}

	@FunctionalInterface
	public static interface MemberKeywordHandler {

		public abstract @Nullable InsnTree create(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException;
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