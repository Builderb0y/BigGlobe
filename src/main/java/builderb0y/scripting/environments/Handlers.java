package builderb0y.scripting.environments;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.Typeable;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.InvokeInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.InvokeStaticInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.*;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.ReflectionData;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class Handlers {

	public static Builder builder(Class<?> in, String name) {
		return new Builder(in, name);
	}

	public static Builder inCaller(String name) {
		return new Builder(ConstantFactory.STACK_WALKER.getCallerClass(), name);
	}

	public static class Builder implements Argument {

		public final Class<?> in;
		public final String name;
		public final List<Argument> arguments;
		public int currentRequiredIndex;
		public boolean addedAsNested;

		public Class<?> returnClass;
		public TypeInfo returnType;
		public Method cachedMethod;
		public MethodInfo cachedMethodInfo;

		public boolean pure;

		public Consumer<CastResult> also;

		public Builder(Class<?> in, String name) {
			this.in = in;
			this.name = name;
			this.arguments = new ArrayList<>(8);
		}

		public Builder invalidateCache() {
			this.cachedMethod = null;
			this.cachedMethodInfo = null;
			return this;
		}

		public Method resolveRaw() {
			return this.cachedMethod != null ? this.cachedMethod : (
				this.cachedMethod = ReflectionData.forClass(this.in).findDeclaredMethod(this.name, method -> {
					if (this.returnClass != null && this.returnClass != method.getReturnType()) {
						return false;
					}
					Class<?>[] actualTypes = method.getParameterTypes();
					List<Argument> arguments = this.arguments;
					for (int index = 0, size = arguments.size(); index < size; index++) {
						if (actualTypes[index] != arguments.get(index).getArgumentClass()) {
							return false;
						}
					}
					return true;
				})
			);
		}

		public MethodInfo resolve() {
			if (this.cachedMethodInfo != null) return this.cachedMethodInfo;
			MethodInfo method = MethodInfo.forMethod(this.resolveRaw());
			if (this.pure) method = method.pure();
			return this.cachedMethodInfo = method;
		}

		public Builder returnClass(Class<?> clazz) {
			this.returnClass = clazz;
			this.returnType = type(clazz);
			return this.invalidateCache();
		}

		public Builder returnType(TypeInfo type) {
			this.returnType = type;
			this.returnClass = type.toClass();
			return this.invalidateCache();
		}

		public Builder pure() {
			this.pure = true;
			if (this.cachedMethodInfo != null) {
				this.cachedMethodInfo = this.cachedMethodInfo.pure();
			}
			return this;
		}

		public Builder addReceiverArgument(Class<?> clazz) {
			if (this.arguments.isEmpty()) {
				this.arguments.add(new ReceiverArgument(clazz));
			}
			else {
				throw new IllegalArgumentException("Receiver argument must be the first argument.");
			}
			return this.invalidateCache();
		}

		public Builder addReceiverArgument(TypeInfo type) {
			if (this.arguments.isEmpty()) {
				this.arguments.add(new ReceiverArgument(type));
			}
			else {
				throw new IllegalArgumentException("Receiver argument must be the first argument.");
			}
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
				else if (arg instanceof ReceiverArgument argument) this.addReceiverArgument(argument.clazz);
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

		public Builder also(Consumer<CastResult> action) {
			this.also = this.also == null ? action : this.also.andThen(action);
			return this;
		}

		public VariableHandler buildVariable() {
			if (this.usesReceiver() || this.usesArguments()) {
				throw new IllegalStateException("Can't build variable when builder requires receiver or arguments.");
			}
			return new VariableHandler.Named(
				this.toString(),
				(ExpressionParser parser, String name) -> {
					CastResult result = this.getFrom(parser, null, InsnTree.ARRAY_FACTORY.empty());
					return result != null ? result.tree() : null;
				}
			);
		}

		public FieldHandler buildField() {
			if (this.usesArguments()) {
				throw new IllegalStateException("Can't build field when builder requires arguments.");
			}
			return new FieldHandler.Named(
				this.toString(),
				(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
					CastResult result = this.getFrom(parser, receiver, InsnTree.ARRAY_FACTORY.empty());
					if (result == null) return null;
					InvokeStaticInsnTree staticInvoker = (InvokeStaticInsnTree)(result.tree());
					if (staticInvoker instanceof InvokeInsnTree invoker) {
						return mode.makeInstanceGetter(parser, invoker.receiver, invoker.method, invoker.args);
					}
					else {
						InsnTree runtimeReceiver = staticInvoker.args[0];
						InsnTree[] runtimeArgs = Arrays.copyOfRange(staticInvoker.args, 1, staticInvoker.args.length);
						return mode.makeStaticGetter(parser, runtimeReceiver, staticInvoker.method, runtimeArgs);
					}
				}
			);
		}

		public FunctionHandler buildFunction() {
			if (this.usesReceiver()) {
				throw new IllegalStateException("Can't build function when builder requires receiver.");
			}
			return new FunctionHandler.Named(
				this.toString(),
				(ExpressionParser parser, String name, InsnTree... arguments) -> {
					return this.getFrom(parser, null, arguments);
				}
			);
		}

		public MethodHandler buildMethod() {
			return new MethodHandler.Named(
				this.toString(),
				(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) -> {
					return this.getFrom(parser, receiver, arguments);
				}
			);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return type(this.resolveRaw().getReturnType());
		}

		@Override
		public Class<?> getArgumentClass() {
			return this.resolveRaw().getReturnType();
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
				if (this.also != null) this.also.accept(result);
				return result;
			}
			else {
				InsnTree runtimeReceiver = runtimeArgs[0];
				runtimeArgs = Arrays.copyOfRange(runtimeArgs, 1, toLength);
				CastResult result = new CastResult(invokeInstance(runtimeReceiver, resolution, runtimeArgs), requiredCasting);
				if (this.also != null) this.also.accept(result);
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
			return !this.arguments.isEmpty() && this.arguments.get(0).usesReceiver();
		}

		@Override
		public boolean usesArguments() {
			for (Argument argument : this.arguments) {
				if (argument.usesArguments()) return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return "Handlers.Builder: { in: " + this.in + ", name: " + this.name + ", arguments: " + this.arguments + " }";
		}
	}

	public static interface Argument extends Typeable {

		public abstract Class<?> getArgumentClass();

		public abstract @Nullable CastResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs);

		public abstract void addToIndex(int toAdd);

		public abstract boolean usesReceiver();

		public abstract boolean usesArguments();
	}

	public static class RequiredArgument implements Argument {

		public final Class<?> clazz;
		public final TypeInfo type;
		public int requiredIndex;

		public RequiredArgument(Class<?> clazz, int requiredIndex) {
			this.clazz = clazz;
			this.type = type(clazz);
			this.requiredIndex = requiredIndex;
		}

		public RequiredArgument(TypeInfo type, int requiredIndex) {
			this.type = type;
			this.clazz = type.toClass();
			this.requiredIndex = requiredIndex;
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public Class<?> getArgumentClass() {
			return this.clazz;
		}

		@Override
		public @Nullable CastResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			InsnTree argument = providedArgs[this.requiredIndex];
			InsnTree castArgument = argument.cast(parser, this.type, CastMode.IMPLICIT_NULL);
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
			return "RequiredArgument: { type: " + this.type + ", index: " + this.requiredIndex + " }";
		}
	}

	public static class ImplicitArgument implements Argument {

		public final InsnTree tree;
		public final Class<?> clazz;

		public ImplicitArgument(InsnTree tree) {
			this.tree = tree;
			this.clazz = tree.getTypeInfo().toClass();
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.tree.getTypeInfo();
		}

		@Override
		public Class<?> getArgumentClass() {
			return this.clazz;
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
			return "ImplicitArgument: " + this.tree.describe();
		}
	}

	public static class ReceiverArgument implements Argument {

		public final Class<?> clazz;
		public final TypeInfo type;

		public ReceiverArgument(Class<?> clazz) {
			this.clazz = clazz;
			this.type = type(clazz);
		}

		public ReceiverArgument(TypeInfo type) {
			this.type = type;
			this.clazz = type.toClass();
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public Class<?> getArgumentClass() {
			return this.clazz;
		}

		@Override
		public @Nullable CastResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			InsnTree castReceiver = receiver.cast(parser, this.type, CastMode.IMPLICIT_NULL);
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
			return "ReceiverArgument: { type: " + this.type + " }";
		}
	}
}