package builderb0y.scripting.environments;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.GetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.InvokeStaticInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MutableScriptEnvironment implements ScriptEnvironment {

	public Map<String,    InsnTree       > variables;
	public Map<NamedType, FieldInfo      > fields;
	public Map<String,    FunctionHandler> functions;
	public Map<NamedType, MethodHandler  > methods;
	public Map<NamedType, FunctionHandler> classFunctions;
	public Map<String,    TypeInfo       > types;

	public static record NamedType(TypeInfo owner, String name) {}

	public MutableScriptEnvironment() {
		this.variables      = new HashMap<>(16);
		this.fields         = new HashMap<>(16);
		this.functions      = new HashMap<>(16);
		this.methods        = new HashMap<>(16);
		this.classFunctions = new HashMap<>(16);
		this.types          = new HashMap<>( 8);
	}

	public MutableScriptEnvironment(
		Map<String,    InsnTree       > variables,
		Map<NamedType, FieldInfo      > fields,
		Map<String,    FunctionHandler> functions,
		Map<NamedType, MethodHandler  > methods,
		Map<NamedType, FunctionHandler> classFunctions,
		Map<String,    TypeInfo       > types
	) {
		this.variables      = variables;
		this.fields         = fields;
		this.functions      = functions;
		this.methods        = methods;
		this.classFunctions = classFunctions;
		this.types          = types;
	}

	public MutableScriptEnvironment addParameter(String name, int index, TypeInfo type) {
		return this.addParameter(new VarInfo(name, index, type));
	}

	public MutableScriptEnvironment addParameter(VarInfo variable) {
		this.variables.put(variable.name, new LoadInsnTree(variable));
		return this;
	}

	public MutableScriptEnvironment addVariable(String name, InsnTree tree) {
		this.variables.put(name, tree);
		return this;
	}

	public MutableScriptEnvironment addField(FieldInfo field) {
		this.fields.put(new NamedType(field.owner, field.name), field);
		return this;
	}

	public MutableScriptEnvironment addFunction(String name, FunctionHandler handler) {
		this.functions.put(name, handler);
		return this;
	}

	public MutableScriptEnvironment addMethod(TypeInfo owner, String name, MethodHandler handler) {
		this.methods.put(new NamedType(owner, name), handler);
		return this;
	}

	public MutableScriptEnvironment addClassFunction(TypeInfo owner, String name, FunctionHandler handler) {
		this.classFunctions.put(new NamedType(owner, name), handler);
		return this;
	}

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		return this.variables.get(name);
	}

	@Override
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		for (TypeInfo type : receiver.getTypeInfo().getAllAssignableTypes()) {
			FieldInfo field = this.fields.get(new NamedType(type, name));
			if (field != null) return new GetFieldInsnTree(receiver, field);
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		FunctionHandler handler = this.functions.get(name);
		return handler == null ? null : handler.createFunction(parser, name, arguments);
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		ConstantValue constant = receiver.getConstantValue();
		if (constant.isConstant() && constant.asJavaObject() instanceof TypeInfo staticType) {
			FunctionHandler handler = this.classFunctions.get(new NamedType(staticType, name));
			if (handler != null) return handler.createFunction(parser, name, arguments);
		}
		for (TypeInfo type : receiver.getTypeInfo().getAllAssignableTypes()) {
			MethodHandler handler = this.methods.get(new NamedType(type, name));
			if (handler != null) return handler.createMethod(parser, receiver, name, arguments);
		}
		return null;
	}

	@Override
	public @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		return this.types.get(name);
	}

	public static <I extends InsnTree> I getBestArguments(ExpressionParser parser, String name, MethodInfo[] infos, InsnTree[] arguments, BiFunction<MethodInfo, InsnTree[], I> constructor) throws ScriptParsingException {
		List<I> list = new ArrayList<>(1);
		for (MethodInfo info : infos) {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, info.name, info.paramTypes, CastMode.IMPLICIT_NULL, arguments);
			if (castArguments != null) {
				I tree = constructor.apply(info, castArguments);
				if (castArguments == arguments) {
					return tree;
				}
				else {
					list.add(tree);
				}
			}
		}
		if (list.size() == 1) {
			return list.get(0);
		}
		else throw new ScriptParsingException(
			(list.isEmpty() ? "Invalid" : "Ambiguous") +
			" arguments for " + name + "(): expected one of: " + (
				Arrays
				.stream(infos)
				.map(MethodInfo::getDescriptor)
				.collect(Collectors.joining(", ", "[", "]"))
			)
			+ ", got " + descriptorOfArguments(arguments),
			parser.input
		);
	}

	/**
	this is a hack to work around generics.
	see {@link ClassScriptEnvironment#getMethod(ExpressionParser, InsnTree, String, InsnTree...)}.
	*/
	public static <I extends InsnTree, M> I getBestArgumentsGeneric(
		ExpressionParser parser,
		String name,
		List<M> infos,
		Function<M, MethodInfo> methodInfoGetter,
		InsnTree[] arguments,
		BiFunction<M, InsnTree[], I> constructor
	)
	throws ScriptParsingException {
		List<I> list = new ArrayList<>(1);
		for (M m : infos) {
			MethodInfo info = methodInfoGetter.apply(m);
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, info.name, info.paramTypes, CastMode.IMPLICIT_NULL, arguments);
			if (castArguments != null) {
				I tree = constructor.apply(m, castArguments);
				if (castArguments == arguments) {
					return tree;
				}
				else {
					list.add(tree);
				}
			}
		}
		if (list.size() == 1) {
			return list.get(0);
		}
		else throw new ScriptParsingException(
			(list.isEmpty() ? "Invalid" : "Ambiguous") +
			" arguments for " + name + "(): expected one of: " + (
				infos
				.stream()
				.map(methodInfoGetter)
				.map(MethodInfo::getDescriptor)
				.collect(Collectors.joining(", ", "[", "]"))
			)
			+ ", got " + descriptorOfArguments(arguments),
			parser.input
		);
	}

	public static String descriptorOfArguments(InsnTree... arguments) {
		return (
			Arrays
			.stream(arguments)
			.map((InsnTree tree) -> tree.getTypeInfo().getDescriptor())
			.collect(Collectors.joining("", "(", ")"))
		);
	}

	@FunctionalInterface
	public static interface FunctionHandler {

		public abstract InsnTree createFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException;

		public static FunctionHandler of(MethodInfo method) {
			if (!method.isStatic()) {
				throw new IllegalArgumentException("Method is not static: " + method);
			}
			return (parser, name, arguments) -> {
				return make(parser, name, method, arguments);
			};
		}

		public static FunctionHandler ofAll(MethodInfo... infos) {
			for (MethodInfo info : infos) {
				if (!info.isStatic()) {
					throw new IllegalArgumentException("Method is not static: " + info);
				}
			}
			return (parser, name, arguments) -> {
				return getBestArguments(parser, name, infos, arguments, InvokeStaticInsnTree::create);
			};
		}

		public static InsnTree make(ExpressionParser parser, String name, MethodInfo method, InsnTree... arguments) throws ScriptParsingException {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, method.paramTypes, CastMode.IMPLICIT_NULL, arguments);
			if (castArguments != null) {
				return invokeStatic(method, castArguments);
			}
			else throw new ScriptParsingException(
				"Invalid arguments for " + name + "(): expected " + method.getDescriptor() + ", got " + descriptorOfArguments(arguments),
				parser.input
			);
		}

		public static FunctionHandler ofConstructor(MethodInfo constructor) {
			if (constructor.isStatic()) {
				throw new IllegalArgumentException("Constructor is static: " + constructor);
			}
			return (parser, name, arguments) -> {
				return make(parser, name, constructor, arguments);
			};
		}

		public static FunctionHandler ofAllConstructors(MethodInfo... constructors) {
			for (MethodInfo info : constructors) {
				if (info.isStatic()) {
					throw new IllegalArgumentException("Constructor is static: " + info);
				}
			}
			return (parser, name, arguments) -> {
				return getBestArguments(parser, name, constructors, arguments, InsnTrees::newInstance);
			};
		}

		public static InsnTree makeConstructor(ExpressionParser parser, MethodInfo constructor, InsnTree... arguments) throws ScriptParsingException {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, "new", constructor.paramTypes, CastMode.IMPLICIT_THROW, arguments);
			if (castArguments != null) {
				return newInstance(constructor, castArguments);
			}
			else {
				throw new ScriptParsingException(
					"Invalid arguments for " + constructor.owner.getSimpleName() + ".new(): expected " + constructor.getDescriptor() + ", got " + descriptorOfArguments(arguments),
					parser.input
				);
			}
		}
	}

	@FunctionalInterface
	public static interface MethodHandler {

		public abstract InsnTree createMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException;

		public static MethodHandler of(MethodInfo info) {
			if (info.isStatic()) {
				throw new IllegalArgumentException("Method is static: " + info);
			}
			return (parser, receiver, name, arguments) -> {
				return make(parser, receiver, name, info, arguments);
			};
		}

		public static InsnTree make(ExpressionParser parser, InsnTree receiver, String name, MethodInfo info, InsnTree... arguments) throws ScriptParsingException {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, info.paramTypes, CastMode.IMPLICIT_NULL, arguments);
			if (castArguments != null) {
				return invokeVirtualOrInterface(receiver, info, castArguments);
			}
			else throw new ScriptParsingException(
				"Invalid arguments for " + name + "(): expected " + info.getDescriptor() + ", got " + descriptorOfArguments(arguments),
				parser.input
			);
		}

		public static MethodHandler allOf(MethodInfo... infos) {
			for (MethodInfo info : infos) {
				if (info.isStatic()) {
					throw new IllegalArgumentException("Method is static: " + info);
				}
			}
			return (parser, receiver, name, arguments) -> {
				return getBestArguments(parser, name, infos, arguments, (info, castArguments) -> {
					return invokeVirtualOrInterface(receiver, info, castArguments);
				});
			};
		}
	}
}