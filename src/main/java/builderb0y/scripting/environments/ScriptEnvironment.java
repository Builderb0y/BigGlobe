package builderb0y.scripting.environments;

import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;
import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.GetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.GetFromStackInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.InvokeInstanceInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.InvokeStaticInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.nullability.NullMapperInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.nullability.NullableFakeInvokeStaticInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.nullability.NullableGetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.nullability.NullableInvokeInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ScriptEnvironment {

	/**
	returns an InsnTree which will load the variable
	associated with the provided name onto the stack.
	if a variable with the provided name does not exist, returns null.

	this method throws {@link ScriptParsingException} if the variable exists,
	but some other problem prevents it from being used.

	this method should NOT throw {@link ScriptParsingException} if
	the requested variable is not known to this ScriptEnvironment,
	because more than one ScriptEnvironment can be installed on an {@link ExpressionParser},
	and a different ScriptEnvironment may know about the variable.
	*/
	public default @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		return null;
	}

	/**
	returns an InsnTree which replaces the receiver with another object on the stack.
	conceptually, the name parameter denotes the name of some property that the receiver has,
	and the returned InsnTree retrieves that property at runtime.
	if no such property with the given name exists on the receiver,
	this method returns null.

	this method throws {@link ScriptParsingException} under circumstances
	that I can't think of right now, but I don't want to need to add this
	exception to the method signature later if/when I do think of something.
	*/
	public default @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) throws ScriptParsingException {
		return null;
	}

	public static enum GetFieldMode {

		NORMAL {

			@Override
			public InsnTree makeField(ExpressionParser parser, InsnTree receiver, FieldInfo field) {
				return new GetFieldInsnTree(receiver, field);
			}

			@Override
			public InsnTree makeInstanceGetter(ExpressionParser parser, InsnTree receiver, MethodInfo getter, InsnTree... arguments) {
				return new InvokeInstanceInsnTree(receiver, getter, arguments);
			}

			@Override
			public InsnTree makeStaticGetter(ExpressionParser parser, MethodInfo getter, InsnTree... extraArguments) {
				return new InvokeStaticInsnTree(getter, extraArguments);
			}
		},

		NULLABLE {

			@Override
			public InsnTree makeField(ExpressionParser parser, InsnTree receiver, FieldInfo field) {
				return new NullableGetFieldInsnTree(receiver, field);
			}

			@Override
			public InsnTree makeInstanceGetter(ExpressionParser parser, InsnTree receiver, MethodInfo getter, InsnTree... arguments) {
				return new NullableInvokeInsnTree(receiver, getter, arguments);
			}

			@Override
			public InsnTree makeStaticGetter(ExpressionParser parser, MethodInfo getter, InsnTree... extraArguments) {
				return new NullableFakeInvokeStaticInsnTree(getter, extraArguments);
			}
		};

		public abstract InsnTree makeField(ExpressionParser parser, InsnTree receiver, FieldInfo field);

		public abstract InsnTree makeInstanceGetter(ExpressionParser parser, InsnTree receiver, MethodInfo getter, InsnTree... arguments);

		public InsnTree makeStaticGetter(ExpressionParser parser, InsnTree receiver, MethodInfo getter, InsnTree... extraArguments) {
			return this.makeStaticGetter(parser, getter, ObjectArrays.concat(receiver, extraArguments));
		}

		public abstract InsnTree makeStaticGetter(ExpressionParser parser, MethodInfo getter, InsnTree... extraArguments);
	}

	/**
	returns an InsnTree which will invoke
	the function with the provided name,
	and push its return value onto the stack.
	if a function with the provided name does not exist,
	this method returns null.

	this method throws {@link ScriptParsingException} if the function exists,
	but some other problem prevents it from being used.
	for example, if the length of the arguments array does not
	match the number of parameters in the underlying function.
	*/
	public default @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		return null;
	}

	/**
	returns an InsnTree which will invoke a method
	with the given name on the given receiver,
	and push its return value onto the stack.
	if a method with the given name does not exist,
	this method returns null.

	this method throws {@link ScriptParsingException} if the method exists,
	but some other problem prevents it from being used.
	for example, if the length of the arguments array does not
	match the number of parameters in the underlying method.
	*/
	public default @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) throws ScriptParsingException {
		return null;
	}

	public static enum GetMethodMode {

		NORMAL {

			@Override
			public InsnTree makeField(ExpressionParser parser, InsnTree receiver, FieldInfo field) {
				return new GetFieldInsnTree(receiver, field);
			}

			@Override
			public InsnTree makeInstanceInvoker(ExpressionParser parser, InsnTree receiver, MethodInfo method, InsnTree... arguments) {
				return new InvokeInstanceInsnTree(receiver, method, arguments);
			}

			@Override
			public InsnTree makeStaticInvoker(ExpressionParser parser, MethodInfo method, InsnTree... extraArguments) {
				return new InvokeStaticInsnTree(method, extraArguments);
			}
		},

		NULLABLE {

			@Override
			public InsnTree makeField(ExpressionParser parser, InsnTree receiver, FieldInfo field) {
				return new NullableGetFieldInsnTree(receiver, field);
			}

			@Override
			public InsnTree makeInstanceInvoker(ExpressionParser parser, InsnTree receiver, MethodInfo method, InsnTree... arguments) {
				return new NullableInvokeInsnTree(receiver, method, arguments);
			}

			@Override
			public InsnTree makeStaticInvoker(ExpressionParser parser, MethodInfo method, InsnTree... extraArguments) {
				return new NullableFakeInvokeStaticInsnTree(method, extraArguments);
			}
		};

		public abstract InsnTree makeField(ExpressionParser parser, InsnTree receiver, FieldInfo field);

		public abstract InsnTree makeInstanceInvoker(ExpressionParser parser, InsnTree receiver, MethodInfo method, InsnTree... arguments);

		public InsnTree makeStaticInvoker(ExpressionParser parser, InsnTree receiver, MethodInfo method, InsnTree... extraArguments) {
			return this.makeStaticInvoker(parser, method, ObjectArrays.concat(receiver, extraArguments));
		}

		public abstract InsnTree makeStaticInvoker(ExpressionParser parser, MethodInfo method, InsnTree... extraArguments);
	}

	/**
	a hook to allow ScriptEnvironment's to define "special" functions.
	these are functions with non-standard syntax.
	for example, "if" is a special function, because its syntax is not
	if (condition, body), but rather if (condition: body).

	if no special function exists with the given name, this method returns null.
	*/
	public default @Nullable InsnTree parseKeyword(ExpressionParser parser, String name) throws ScriptParsingException {
		return null;
	}

	/**
	a hook to allow ScriptEnvironment's to define "special" methods.
	these are methods with non-standard syntax.

	if no special method exists with the given name, this method returns null.
	*/
	public default @Nullable InsnTree parseMemberKeyword(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) throws ScriptParsingException {
		return null;
	}

	public static enum MemberKeywordMode {

		NORMAL {

			@Override
			public InsnTree apply(InsnTree receiver, MemberKeywordFunction function) throws ScriptParsingException {
				return function.apply(receiver);
			}
		},

		NULLABLE {

			@Override
			public InsnTree apply(InsnTree receiver, MemberKeywordFunction function) throws ScriptParsingException {
				return new NullMapperInsnTree(receiver, function.apply(getFromStack(receiver.getTypeInfo())));
			}
		};

		/**
		creates an InsnTree from the provided receiver and function.
		the function is expected to transform the receiver into some other InsnTree.
		if this is the normal mode, then the function is called on the provided receiver.
		but for the nullable mode it's a bit more complex: a {@link NullMapperInsnTree}
		is created using the provided receiver, and the function is called on a
		{@link GetFromStackInsnTree} instead. this works because {@link NullMapperInsnTree}
		leaves a copy of the receiver on the stack when emitting bytecode for its mapper.
		*/
		public abstract InsnTree apply(InsnTree receiver, MemberKeywordFunction function) throws ScriptParsingException;

		@FunctionalInterface
		public static interface MemberKeywordFunction {

			public abstract InsnTree apply(InsnTree receiver) throws ScriptParsingException;
		}
	}

	/**
	returns a TypeInfo represented by the given name,
	or null if this ScriptEnvironment doesn't know of a TypeInfo with the given name.
	for example, if name is "String", this ScriptEnvironment may
	return a TypeInfo which corresponds to class {@link String}.
	*/
	public default @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		return null;
	}

	/**
	returns an InsnTree which will cast the provided value to the specified type,
	or null if this ScriptEnvironment does not know how to perform such a cast.
	@param implicit if true, this ScriptEnvironment is required to perform the cast implicitly.
	*/
	public default @Nullable InsnTree cast(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit) {
		return null;
	}

	public abstract Stream<String> listCandidates(String name);

	public static InsnTree[] castArguments(ExpressionParser parser, MethodInfo method, CastMode mode, InsnTree... arguments) {
		return castArguments(parser, method.name, method.paramTypes, mode, arguments);
	}

	public static InsnTree[] castArguments(ExpressionParser parser, String name, TypeInfo[] expectedTypes, CastMode mode, InsnTree... arguments) {
		int length = arguments.length;
		if (length != expectedTypes.length) {
			if (mode.nullable) return null;
			throw new IllegalArgumentException("Wrong number of arguments for " + name + ": expected " + expectedTypes.length + ", got " + length);
		}
		InsnTree[] result = arguments;
		for (int index = 0; index < length; index++) {
			InsnTree oldArg = arguments[index];
			InsnTree newArg = oldArg.cast(parser, expectedTypes[index], mode);
			if (newArg == null) return null;
			if (oldArg != newArg) {
				if (result == arguments) result = result.clone();
				result[index] = newArg;
			}
		}
		return result;
	}

	public static InsnTree castArgument(ExpressionParser parser, MethodInfo method, CastMode mode, InsnTree... arguments) {
		return castArgument(parser, method.name, method.paramTypes[0], mode, arguments);
	}

	public static InsnTree castArgument(ExpressionParser parser, String name, TypeInfo type, CastMode mode, InsnTree... arguments) {
		if (arguments.length != 1) {
			if (mode.nullable) return null;
			throw new IllegalArgumentException("Wrong number of arguments for " + name + ": expected 1, got " + arguments.length);
		}
		return arguments[0].cast(parser, type, mode);
	}
}