package builderb0y.scripting.environments;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ObjectArrays;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.Typeable;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.invokers.BaseInvokeInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.*;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.ReflectionData;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class Handlers {

	public static Builder builder(Class<?> in, String name) {
		return new ReflectiveBuilder(in, name);
	}

	public static Builder inCaller(String name) {
		return new ReflectiveBuilder(ConstantFactory.STACK_WALKER.getCallerClass(), name);
	}

	public static Builder builder(MethodInfo method) {
		return new ManualBuilder(method);
	}

	@FunctionalInterface
	public static interface Callback {

		public abstract void onReferenced(ExpressionParser parser, CastResult result);

		public static Callback combine(Callback a, Callback b) {
			return (ExpressionParser parser, CastResult result) -> {
				a.onReferenced(parser, result);
				b.onReferenced(parser, result);
			};
		}
	}

	public static abstract class Builder implements Argument {

		public final List<Argument> arguments;
		public int currentRequiredIndex;
		public boolean addedAsNested;
		public boolean pure;
		public Callback callback;

		public Builder() {
			this.arguments = new ArrayList<>(8);
		}

		public abstract Builder invalidateCache();

		public abstract MethodInfo resolve();

		public abstract Builder returnClass(Class<?> clazz);

		public abstract Builder returnType(TypeInfo type);

		public Builder addReceiverArgument(Class<?> clazz) {
			if (this.usesArguments() || this.usesReceiver()) {
				throw new IllegalArgumentException("Receiver argument must be the first argument.");
			}
			else {
				this.arguments.add(new ReceiverArgument(clazz));
			}
			return this.invalidateCache();
		}

		public Builder addReceiverArgument(TypeInfo type) {
			if (this.usesArguments() || this.usesReceiver()) {
				throw new IllegalArgumentException("Receiver argument must be the first argument.");
			}
			this.arguments.add(new ReceiverArgument(type));
			return this.invalidateCache();
		}

		public Builder addRequiredArgument(Class<?> clazz) {
			this.arguments.add(new RequiredArgument(clazz, this.currentRequiredIndex++));
			return this.invalidateCache();
		}

		public Builder addRequiredArgument(TypeInfo type) {
			this.arguments.add(new RequiredArgument(type, this.currentRequiredIndex++));
			return this.invalidateCache();
		}

		public Builder addImplicitArgument(InsnTree tree) {
			this.arguments.add(new ImplicitArgument(tree));
			return this.invalidateCache();
		}

		public Builder addImplicitArgumentOfType(InsnTree tree, Class<?> type) {
			this.arguments.add(new ImplicitArgument(tree, type(type)));
			return this.invalidateCache();
		}

		public Builder addNestedArgument(Builder builder) {
			if (this.usesReceiver() && builder.usesReceiver()) {
				throw new IllegalArgumentException("Attempt to add receiver argument twice.");
			}
			builder.addedAsNested = true;
			builder.addToIndex(this.currentRequiredIndex++);
			this.arguments.add(builder);
			return this.invalidateCache();
		}

		@Deprecated //you probably want the other overload.
		public Builder addArguments() {
			return this;
		}

		public Builder addArguments(Object... args) {
			for (Object arg : args) {
				if (arg instanceof Class<?> clazz) this.addRequiredArgument(clazz);
				else if (arg instanceof TypeInfo type) this.addRequiredArgument(type);
				else if (arg instanceof InsnTree tree) this.addImplicitArgument(tree);
				else if (arg instanceof Builder builder) this.addNestedArgument(builder);
				else if (arg instanceof ReceiverArgument argument) this.addReceiverArgument(argument.type);
				else if (arg instanceof Character character) this.addRequiredArgument(TypeInfo.parse(character.charValue()));
				else if (arg instanceof CharSequence string) {
					for (TypeInfo type : TypeInfo.parseAll(string)) {
						this.addRequiredArgument(type);
					}
				}
				else throw new IllegalArgumentException("Unrecognized argument: " + arg);
			}
			return this.invalidateCache();
		}

		public Builder callback(Callback callback) {
			this.callback = this.callback == null ? callback : Callback.combine(this.callback, callback);
			return this;
		}

		public VariableHandler.Named buildVariable() {
			if (this.usesReceiver() || this.usesArguments()) {
				throw new IllegalStateException("Can't build variable when builder requires receiver or arguments.");
			}
			boolean deprecated = this.resolve().isDeprecated();
			return new VariableHandler.Named(
				this.toString(),
				(ExpressionParser parser, String name) -> {
					CastResult result = this.getFrom(parser, null, InsnTree.ARRAY_FACTORY.empty());
					if (result == null) return null;
					if (deprecated) {
						ScriptLogger.LOGGER.warn("A script used a deprecated variable: " + name + '\n' + parser.input.getSourceForError() + " <--- HERE");
					}
					return result.tree();
				}
			);
		}

		public FieldHandler.Named buildField() {
			if (this.usesArguments()) {
				throw new IllegalStateException("Can't build field when builder requires arguments.");
			}
			if (!this.usesReceiver()) {
				throw new IllegalStateException("Can't build field without receiver.");
			}
			boolean deprecated = this.resolve().isDeprecated();
			return new FieldHandler.Named(
				this.toString(),
				(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
					CastResult result = this.getFrom(parser, receiver, InsnTree.ARRAY_FACTORY.empty());
					if (result == null) return null;
					if (deprecated) {
						ScriptLogger.LOGGER.warn("A script used a deprecated field: " + name + '\n' + parser.input.getSourceForError() + " <--- HERE");
					}
					BaseInvokeInsnTree invoker = (BaseInvokeInsnTree)(result.tree());
					if (invoker.method.isStatic()) {
						return mode.makeInvoker(parser, invoker.method, invoker.args);
					}
					else {
						return mode.makeInvoker(parser, invoker.args[0], invoker.method, Arrays.copyOfRange(invoker.args, 1, invoker.args.length));
					}
				}
			);
		}

		public FunctionHandler.Named buildFunction() {
			if (this.usesReceiver()) {
				throw new IllegalStateException("Can't build function when builder requires receiver.");
			}
			boolean deprecated = this.resolve().isDeprecated();
			return new FunctionHandler.Named(
				this.toString(),
				(ExpressionParser parser, String name, InsnTree... arguments) -> {
					CastResult result = this.getFrom(parser, null, arguments);
					if (result == null) return null;
					if (deprecated) {
						ScriptLogger.LOGGER.warn("A script used a deprecated function: " + name + '\n' + parser.input.getSourceForError() + " <--- HERE");
					}
					return result;
				}
			);
		}

		public MethodHandler.Named buildMethod() {
			if (!this.usesReceiver()) {
				throw new IllegalStateException("Can't build method without receiver.");
			}
			boolean deprecated = this.resolve().isDeprecated();
			return new MethodHandler.Named(
				this.toString(),
				(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
					CastResult result = this.getFrom(parser, receiver, arguments);
					if (result == null) return null;
					if (deprecated) {
						ScriptLogger.LOGGER.warn("A script used a deprecated method: " + name + '\n' + parser.input.getSourceForError() + " <--- HERE");
					}
					BaseInvokeInsnTree invoker = (BaseInvokeInsnTree)(result.tree());
					return new CastResult(mode.makeInvoker(parser, invoker.method, invoker.args), result.requiredCasting());
				}
			);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.resolve().returnType;
		}

		@Override
		public @Nullable CastResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			int fromLength = providedArgs.length;
			if (!this.addedAsNested && this.currentRequiredIndex != fromLength) return null;
			int toLength = this.arguments.size();
			InsnTree[] runtimeArgs = new InsnTree[toLength];
			boolean requiredCasting = false;
			for (int index = 0; index < toLength; index++) {
				CastResult castResult = this.arguments.get(index).getFrom(parser, receiver, providedArgs);
				if (castResult == null) return null;
				runtimeArgs[index] = castResult.tree();
				requiredCasting |= castResult.requiredCasting();
			}
			MethodInfo resolution = this.resolve();
			if (resolution.isStatic()) {
				CastResult result = new CastResult(invokeStatic(resolution, runtimeArgs), requiredCasting);
				if (this.callback != null) this.callback.onReferenced(parser, result);
				return result;
			}
			else {
				InsnTree runtimeReceiver = runtimeArgs[0];
				runtimeArgs = Arrays.copyOfRange(runtimeArgs, 1, toLength);
				CastResult result = new CastResult(invokeInstance(runtimeReceiver, resolution, runtimeArgs), requiredCasting);
				if (this.callback != null) this.callback.onReferenced(parser, result);
				return result;
			}
		}

		@Override
		public void addToIndex(int toAdd) {
			for (Argument argument : this.arguments) {
				argument.addToIndex(toAdd);
			}
		}

		@Override
		public boolean usesReceiver() {
			for (Argument argument : this.arguments) {
				if (argument.usesReceiver()) return true;
				if (argument.usesArguments()) return false;
			}
			return false;
		}

		@Override
		public boolean usesArguments() {
			for (Argument argument : this.arguments) {
				if (argument.usesArguments()) return true;
			}
			return false;
		}
	}

	public static class ReflectiveBuilder extends Builder {

		public final Class<?> in;
		public final String name;

		public Class<?> returnClass;
		public TypeInfo returnType;
		public Method cachedMethod;
		public MethodInfo cachedMethodInfo;

		public ReflectiveBuilder(Class<?> in, String name) {
			this.in = in;
			this.name = name;
		}

		@Override
		public Builder invalidateCache() {
			this.cachedMethod = null;
			this.cachedMethodInfo = null;
			return this;
		}

		public Method resolveRaw() {
			return this.cachedMethod != null ? this.cachedMethod : (
				this.cachedMethod = ReflectionData.forClass(this.in).findDeclaredMethod(
					this.name, (Method method) -> {
						if (this.returnClass != null && this.returnClass != method.getReturnType()) {
							return false;
						}
						Class<?>[] actualTypes = method.getParameterTypes();
						if (!Modifier.isStatic(method.getModifiers())) {
							actualTypes = ObjectArrays.concat(method.getDeclaringClass(), actualTypes);
						}
						List<Argument> arguments = this.arguments;
						if (actualTypes.length != arguments.size()) {
							return false;
						}
						for (int index = 0, size = arguments.size(); index < size; index++) {
							if (actualTypes[index] != arguments.get(index).getTypeInfo().toClass()) {
								return false;
							}
						}
						return true;
					}
				)
			);
		}

		@Override
		public MethodInfo resolve() {
			if (this.cachedMethodInfo != null) return this.cachedMethodInfo;
			MethodInfo method = MethodInfo.forMethod(this.resolveRaw());
			if (this.pure) method = method.pure();
			return this.cachedMethodInfo = method;
		}

		@Override
		public Builder returnClass(Class<?> clazz) {
			this.returnClass = clazz;
			this.returnType = type(clazz);
			return this.invalidateCache();
		}

		@Override
		public Builder returnType(TypeInfo type) {
			this.returnType = type;
			this.returnClass = type.toClass();
			return this.invalidateCache();
		}

		@Override
		public String toString() {
			return this.in.getName() + '.' + this.name + this.arguments.stream().map(Argument::toString).collect(Collectors.joining(", ", "(", ")"));
		}
	}

	public static class ManualBuilder extends Builder {

		public final MethodInfo methodInfo;

		public ManualBuilder(MethodInfo methodInfo) {
			this.methodInfo = methodInfo;
		}

		@Override
		public Builder invalidateCache() {
			return this;
		}

		@Override
		public MethodInfo resolve() {
			return this.methodInfo;
		}

		@Override
		public Builder returnClass(Class<?> clazz) {
			throw new UnsupportedOperationException("You already specified an exact method.");
		}

		@Override
		public Builder returnType(TypeInfo type) {
			throw new UnsupportedOperationException("You already specified an exact method.");
		}

		@Override
		public String toString() {
			return this.methodInfo.owner.getClassName() + '.' + this.methodInfo.name + this.arguments.stream().map(Argument::toString).collect(Collectors.joining(", ", "(", ")"));
		}
	}

	public static interface Argument extends Typeable {

		public abstract @Nullable CastResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs);

		public abstract void addToIndex(int toAdd);

		public abstract boolean usesReceiver();

		public abstract boolean usesArguments();
	}

	public static class RequiredArgument implements Argument {

		public final TypeInfo type;
		public int requiredIndex;

		public RequiredArgument(Class<?> clazz, int requiredIndex) {
			this(type(clazz), requiredIndex);
		}

		public RequiredArgument(TypeInfo type, int requiredIndex) {
			this.type = type;
			this.requiredIndex = requiredIndex;
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public @Nullable CastResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			InsnTree argument = providedArgs[this.requiredIndex];
			InsnTree castArgument = argument.cast(parser, this.type, CastMode.IMPLICIT_NULL, false);
			if (castArgument == null) return null;
			return new CastResult(castArgument, castArgument != argument);
		}

		@Override
		public void addToIndex(int toAdd) {
			this.requiredIndex += toAdd;
		}

		@Override
		public boolean usesReceiver() {
			return false;
		}

		@Override
		public boolean usesArguments() {
			return true;
		}

		@Override
		public String toString() {
			return "Required: " + this.type;
		}
	}

	public static class ImplicitArgument implements Argument {

		public final InsnTree tree;
		public final TypeInfo type;

		public ImplicitArgument(InsnTree tree) {
			this.tree = tree;
			this.type = tree.getTypeInfo();
		}

		public ImplicitArgument(InsnTree tree, TypeInfo type) {
			if (!tree.getTypeInfo().extendsOrImplements(type)) {
				throw new IllegalArgumentException(tree + " is not a subclass of " + type);
			}
			this.tree = tree;
			this.type = type;
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public @Nullable CastResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			return new CastResult(this.tree, false);
		}

		@Override
		public void addToIndex(int toAdd) {
			//no-op.
		}

		@Override
		public boolean usesReceiver() {
			return false;
		}

		@Override
		public boolean usesArguments() {
			return false;
		}

		@Override
		public String toString() {
			return "Implicit: " + this.tree.describe();
		}
	}

	public static class ReceiverArgument implements Argument {

		public final TypeInfo type;

		public ReceiverArgument(Class<?> clazz) {
			this(type(clazz));
		}

		public ReceiverArgument(TypeInfo type) {
			this.type = type;
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public @Nullable CastResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			InsnTree castReceiver = receiver.cast(parser, this.type, CastMode.IMPLICIT_NULL, false);
			if (castReceiver == null) return null;
			return new CastResult(castReceiver, castReceiver != receiver);
		}

		@Override
		public void addToIndex(int toAdd) {
			//no-op.
		}

		@Override
		public boolean usesReceiver() {
			return true;
		}

		@Override
		public boolean usesArguments() {
			return false;
		}

		@Override
		public String toString() {
			return "Receiver: " + this.type;
		}
	}
}