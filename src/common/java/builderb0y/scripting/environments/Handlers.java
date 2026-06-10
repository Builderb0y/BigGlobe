package builderb0y.scripting.environments;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ObjectArrays;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.fields.NormalInstanceGetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.fields.NullableInstanceGetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.fields.NullableReceiverInstanceGetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.fields.ReceiverInstanceGetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.*;
import builderb0y.scripting.environments.MutableScriptEnvironment.*;
import builderb0y.scripting.environments.ScriptEnvironment.CommonMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class Handlers {

	public static Builder methodBuilder(Class<?> in, String name) {
		return new ReflectiveMethodBasedBuilder(in, name);
	}

	public static Builder methodInCaller(String name) {
		return new ReflectiveMethodBasedBuilder(ConstantFactory.STACK_WALKER.getCallerClass(), name);
	}

	public static Builder methodBuilder(MethodInfo method) {
		return new ManualMethodBasedBuilder(method);
	}

	public static Builder methodWithReceiver(MethodInfo method) {
		Builder builder = methodBuilder(method);
		if (method.isStatic()) {
			builder.addReceiverArgument(method.paramTypes[0]);
			for (int index = 1; index < method.paramTypes.length; index++) {
				builder.addRequiredArgument(method.paramTypes[index]);
			}
		}
		else {
			builder.addReceiverArgument(method.owner).addArguments((Object[])(method.paramTypes));
		}
		return builder;
	}

	public static Builder methodWithoutReceiver(MethodInfo method) {
		return methodBuilder(method).addArguments((Object[])(method.paramTypes));
	}

	public static Builder methodWithReceiver(Class<?> in, String name) {
		return methodWithReceiver(MethodInfo.getMethod(in, name));
	}

	public static Builder methodWithoutReceiver(Class<?> in, String name) {
		return methodWithoutReceiver(MethodInfo.getMethod(in, name));
	}

	public static Builder constructorBuilder(Class<?> in) {
		return methodBuilder(in, "<init>");
	}

	public static Builder constructorInCaller() {
		return constructorBuilder(ConstantFactory.STACK_WALKER.getCallerClass());
	}

	public static Builder fieldBuilder(Class<?> in, String name) {
		return new ReflectiveFieldBasedBuilder(in, name);
	}

	public static Builder fieldInCaller(String name) {
		return new ReflectiveFieldBasedBuilder(ConstantFactory.STACK_WALKER.getCallerClass(), name);
	}

	public static Builder fieldBuilder(FieldInfo field) {
		return new ManualFieldBasedBuilder(field);
	}

	public static Builder fieldWithReceiver(FieldInfo field) {
		return fieldBuilder(field).addReceiverArgument(field.owner);
	}

	public static Builder variableBuilder(String exposedName, InsnTree result) {
		return new VariableBasedBuilder(exposedName, result);
	}

	public static Builder variableBuilder(LoadInsnTree result) {
		return new VariableBasedBuilder(result.variable.name, result);
	}

	public static UsageCallback warnDeprecated(String type) {
		return (ExpressionParser parser, String name) -> {
			BigGlobeMod.LOGGER.warn("A script used a deprecated " + type + ": " + name + "\n" + parser.input.getSourceForError() + " <--- HERE");
		};
	}

	public static abstract class Builder implements Argument {

		public final List<Argument> arguments = new ArrayList<>(8);
		public int currentRequiredIndex;
		public boolean addedAsNested;
		public String exposedName;
		public TypeInfo explicitCast;
		public UsageCallback usageCallback;

		public abstract Builder resultClass(Class<?> clazz);

		public abstract Builder resultType(TypeInfo type);

		public abstract Builder invalidateCache();

		public TypeInfo owner() {
			for (Argument argument : this.arguments) {
				if (argument.usesReceiver()) {
					return argument.getTypeInfo();
				}
			}
			throw new IllegalStateException("Can't find owner!");
		}

		public abstract String defaultExposedName();

		public String exposedName() {
			return this.exposedName != null ? this.exposedName : this.defaultExposedName();
		}

		public Builder exposedName(String exposedName) {
			this.exposedName = exposedName;
			return this;
		}

		public Builder explicitCast(TypeInfo type) {
			this.explicitCast = type;
			return this;
		}

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

		public Builder addImportedArgument(TypeInfo type) {
			this.arguments.add(new ImportedArgument(type));
			return this.invalidateCache();
		}

		public Builder addImportedArgument(Class<?> clazz) {
			this.arguments.add(new ImportedArgument(clazz));
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
				switch (arg) {
					case Class<?> clazz -> this.addRequiredArgument(clazz);
					case TypeInfo type -> this.addRequiredArgument(type);
					case InsnTree tree -> this.addImplicitArgument(tree);
					case Builder builder -> this.addNestedArgument(builder);
					case ReceiverArgument argument -> this.addReceiverArgument(argument.type);
					case Character character -> this.addRequiredArgument(TypeInfo.parse(character.charValue()));
					case CharSequence string -> {
						for (TypeInfo type : TypeInfo.parseAll(string)) {
							this.addRequiredArgument(type);
						}
					}
					case null, default -> throw new IllegalArgumentException("Unrecognized argument: " + arg);
				}
			}
			return this.invalidateCache();
		}

		public Builder onUsed(UsageCallback usageCallback) {
			this.usageCallback = usageCallback;
			return this;
		}

		public abstract UsageCallback callbackWithDeprecation(String type);

		public VariableHandler.Named buildVariable() {
			if (this.usesReceiver() || this.usesArguments()) {
				throw new IllegalStateException("Can't build variable when builder requires receiver or arguments.");
			}
			return new VariableHandler.Named(
				this.exposedName(),
				this.toString(),
				this.callbackWithDeprecation("variable"),
				(ExpressionParser parser, String name) -> {
					GatherResult result = this.getFrom(parser, null, InsnTree.ARRAY_FACTORY.empty());
					return result == null ? null : result.collect(parser, CommonMode.NORMAL);
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
			return new FieldHandler.Named(
				this.owner(),
				this.exposedName(),
				this.toString(),
				this.callbackWithDeprecation("field"),
				(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
					GatherResult result = this.getFrom(parser, receiver, InsnTree.ARRAY_FACTORY.empty());
					return result == null ? null : result.collect(parser, mode.toCommon());
				}
			);
		}

		public FunctionHandler.Named buildFunction() {
			if (this.usesReceiver()) {
				throw new IllegalStateException("Can't build function when builder requires receiver.");
			}
			return new FunctionHandler.Named(
				this.exposedName(),
				this.toString(),
				this.callbackWithDeprecation("function"),
				(ExpressionParser parser, String name, InsnTree... arguments) -> {
					GatherResult result = this.getFrom(parser, null, arguments);
					return result == null ? null : new CastResult(result.collect(parser, CommonMode.NORMAL), result.requiredCasting);
				}
			);
		}

		public MethodHandler.Named buildMethod() {
			if (!this.usesReceiver()) {
				throw new IllegalStateException("Can't build method without receiver.");
			}
			return new MethodHandler.Named(
				this.owner(),
				this.exposedName(),
				this.toString(),
				this.callbackWithDeprecation("method"),
				(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
					GatherResult result = this.getFrom(parser, receiver, arguments);
					return result == null ? null : new CastResult(result.collect(parser, mode.toCommon()), result.requiredCasting);
				}
			);
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

		@Override
		public abstract String toString();
	}

	public static abstract class MethodBasedBuilder extends Builder {

		public abstract MethodInfo resolve();

		@Override
		public UsageCallback callbackWithDeprecation(String type) {
			UsageCallback callback = this.usageCallback;
			if (this.resolve().isDeprecated()) {
				callback = UsageCallback.combine(callback, warnDeprecated(type));
			}
			return callback;
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.explicitCast != null ? this.explicitCast : this.resolve().returnType;
		}

		@Override
		public @Nullable GatherResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			int fromLength = providedArgs.length;
			if (!this.addedAsNested && this.currentRequiredIndex != fromLength) return null;
			int toLength = this.arguments.size();
			InsnTree[] runtimeArgs = new InsnTree[toLength];
			boolean requiredCasting = false;
			for (int index = 0; index < toLength; index++) {
				GatherResult castResult = this.arguments.get(index).getFrom(parser, receiver, providedArgs);
				if (castResult == null) return null;
				try {
					runtimeArgs[index] = castResult.collect(parser, CommonMode.NORMAL);
				}
				catch (ScriptParsingException e) {
					throw new RuntimeException(e);
				}
				requiredCasting |= castResult.requiredCasting;
			}
			MethodInfo resolution = this.resolve();
			return new GatherResult(requiredCasting, runtimeArgs) {

				@Override
				public InsnTree collect(ExpressionParser parser, CommonMode mode) throws ScriptParsingException {
					InsnTree result = switch (mode) {
						case NORMAL -> (
							resolution.isConstructor()
							? new NewInsnTree(resolution, this.trees)
							: resolution.isStatic()
							? new StaticInvokeInsnTree(resolution, this.trees)
							: new NormalInvokeInsnTree(resolution, this.trees)
						);
						case NULLABLE -> (
							resolution.isConstructor()
							? new NewInsnTree(resolution, this.trees)
							: new NullableInvokeInsnTree(resolution, this.trees)
						);
						case RECEIVER -> (
							resolution.isConstructor()
							? notAllowed(parser)
							: new ReceiverInvokeInsnTree(resolution, this.trees)
						);
						case NULLABLE_RECEIVER -> (
							resolution.isConstructor()
							? notAllowed(parser)
							: new NullableReceiverInvokeInsnTree(resolution, this.trees)
						);
					};
					if (MethodBasedBuilder.this.explicitCast != null) {
						result = result.cast(parser, MethodBasedBuilder.this.explicitCast, CastMode.EXPLICIT_THROW, false);
					}
					return result;
				}

				public static InsnTree notAllowed(ExpressionParser parser) throws ScriptParsingException {
					throw new ScriptParsingException("Receiver syntax is not supported for constructors", parser.input);
				}
			};
		}
	}

	public static class ReflectiveMethodBasedBuilder extends MethodBasedBuilder {

		public final Class<?> in;
		public final String name;

		public Class<?> returnClass;
		public TypeInfo returnType;
		public Executable cachedMethod;
		public MethodInfo cachedMethodInfo;

		public ReflectiveMethodBasedBuilder(Class<?> in, String name) {
			this.in = in;
			this.name = name;
		}

		@Override
		public String defaultExposedName() {
			return this.name.equals("<init>") ? "new" : this.name;
		}

		@Override
		public MethodBasedBuilder invalidateCache() {
			this.cachedMethod = null;
			this.cachedMethodInfo = null;
			return this;
		}

		public Executable resolveRaw() {
			return this.cachedMethod != null ? this.cachedMethod : (
				this.cachedMethod = ReflectionData.forClass(this.in).findDeclaredExecutable(
					this.name,
					(Executable executable) -> {
						if (this.returnClass != null && this.returnClass != switch (executable) {
							case Method method -> method.getReturnType();
							case Constructor<?> constructor -> constructor.getDeclaringClass();
						}) {
							return false;
						}
						Class<?>[] actualTypes = executable.getParameterTypes();
						if (executable instanceof Method && !Modifier.isStatic(executable.getModifiers())) {
							actualTypes = ObjectArrays.concat(executable.getDeclaringClass(), actualTypes);
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
			return this.cachedMethodInfo = MethodInfo.forExecutable(this.resolveRaw());
		}

		@Override
		public Builder resultClass(Class<?> clazz) {
			this.returnClass = clazz;
			this.returnType = type(clazz);
			return this.invalidateCache();
		}

		@Override
		public Builder resultType(TypeInfo type) {
			this.returnType = type;
			this.returnClass = type.toClass();
			return this.invalidateCache();
		}

		@Override
		public String toString() {
			return this.in.getName() + '.' + this.name + this.arguments.stream().map(Argument::toString).collect(Collectors.joining(", ", "(", ")"));
		}
	}

	public static class ManualMethodBasedBuilder extends MethodBasedBuilder {

		public final MethodInfo methodInfo;

		public ManualMethodBasedBuilder(MethodInfo methodInfo) {
			this.methodInfo = methodInfo;
		}

		@Override
		public String defaultExposedName() {
			return this.methodInfo.name.equals("<init>") ? "new" : this.methodInfo.name;
		}

		@Override
		public MethodBasedBuilder invalidateCache() {
			return this;
		}

		@Override
		public MethodInfo resolve() {
			return this.methodInfo;
		}

		@Override
		public Builder resultClass(Class<?> clazz) {
			throw new UnsupportedOperationException("You already specified an exact method.");
		}

		@Override
		public Builder resultType(TypeInfo type) {
			throw new UnsupportedOperationException("You already specified an exact method.");
		}

		@Override
		public String toString() {
			return this.methodInfo.owner.getClassName() + '.' + this.methodInfo.name + this.arguments.stream().map(Argument::toString).collect(Collectors.joining(", ", "(", ")"));
		}
	}

	public static abstract class FieldBasedBuilder extends Builder {

		public abstract FieldInfo resolve();

		@Override
		public @Nullable GatherResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			int fromLength = providedArgs.length;
			if (!this.addedAsNested && this.currentRequiredIndex != fromLength) return null;
			if (this.arguments.size() != 1) return null;
			GatherResult castResult = this.arguments.getFirst().getFrom(parser, receiver, providedArgs);
			if (castResult == null) return null;
			InsnTree runtimeArgs;
			try {
				runtimeArgs = castResult.collect(parser, CommonMode.NORMAL);
			}
			catch (ScriptParsingException e) {
				throw new RuntimeException(e);
			}
			boolean requiredCasting = castResult.requiredCasting;
			FieldInfo resolution = this.resolve();
			return new GatherResult(requiredCasting, runtimeArgs) {

				@Override
				public InsnTree collect(ExpressionParser parser, CommonMode mode) throws ScriptParsingException {
					InsnTree result = switch (mode) {
						case NORMAL -> new NormalInstanceGetFieldInsnTree(this.trees[0], resolution);
						case NULLABLE -> new NullableInstanceGetFieldInsnTree(this.trees[0], resolution);
						case RECEIVER -> new ReceiverInstanceGetFieldInsnTree(this.trees[0], resolution);
						case NULLABLE_RECEIVER -> new NullableReceiverInstanceGetFieldInsnTree(this.trees[0], resolution);
					};
					if (FieldBasedBuilder.this.explicitCast != null) {
						result = result.cast(parser, FieldBasedBuilder.this.explicitCast, CastMode.EXPLICIT_THROW, false);
					}
					return result;
				}
			};
		}

		@Override
		public UsageCallback callbackWithDeprecation(String type) {
			UsageCallback callback = this.usageCallback;
			if (this.resolve().isDeprecated()) {
				callback = UsageCallback.combine(callback, warnDeprecated(type));
			}
			return callback;
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.explicitCast != null ? this.explicitCast : this.resolve().type;
		}
	}

	public static class ReflectiveFieldBasedBuilder extends FieldBasedBuilder {

		public final Class<?> in;
		public final String name;

		public Class<?> fieldClass;
		public TypeInfo fieldType;
		public Field cachedField;
		public FieldInfo cachedFieldInfo;

		public ReflectiveFieldBasedBuilder(Class<?> in, String name) {
			this.in = in;
			this.name = name;
		}

		@Override
		public String defaultExposedName() {
			return this.name;
		}

		@Override
		public Builder resultClass(Class<?> type) {
			this.fieldClass = type;
			this.fieldType = type(type);
			return this.invalidateCache();
		}

		@Override
		public Builder resultType(TypeInfo type) {
			this.fieldClass = type.toClass();
			this.fieldType = type;
			return this.invalidateCache();
		}

		public Field resolveRaw() {
			if (this.arguments.size() != 1) {
				throw new IllegalStateException("Must provide exactly one argument for field builders.");
			}
			TypeInfo argType = this.arguments.getFirst().getTypeInfo();
			if (!argType.extendsOrImplements(type(this.in))) {
				throw new IllegalStateException("Leading argument type (" + argType + ") is not assignable to field owner type (" + this.in + ").");
			}
			return this.cachedField != null ? this.cachedField : (
				this.cachedField = ReflectionData.forClass(this.in).findDeclaredField(
					this.name,
					(Field field) -> {
						if (this.fieldClass != null && this.fieldClass != field.getType()) {
							return false;
						}
						return true;
					}
				)
			);
		}

		@Override
		public FieldInfo resolve() {
			if (this.cachedFieldInfo != null) return this.cachedFieldInfo;
			return this.cachedFieldInfo = FieldInfo.forField(this.resolveRaw());
		}

		@Override
		public FieldBasedBuilder invalidateCache() {
			this.cachedField = null;
			this.cachedFieldInfo = null;
			return this;
		}

		@Override
		public String toString() {
			return this.in.getName() + '.' + this.name;
		}
	}

	public static class ManualFieldBasedBuilder extends FieldBasedBuilder {

		public final FieldInfo fieldInfo;

		public ManualFieldBasedBuilder(FieldInfo fieldInfo) {
			this.fieldInfo = fieldInfo;
		}

		@Override
		public String defaultExposedName() {
			return this.fieldInfo.name;
		}

		@Override
		public FieldInfo resolve() {
			return this.fieldInfo;
		}

		@Override
		public Builder resultClass(Class<?> type) {
			throw new UnsupportedOperationException("You already specified an exact field.");
		}

		@Override
		public Builder resultType(TypeInfo type) {
			throw new UnsupportedOperationException("You already specified an exact field.");
		}

		@Override
		public Builder invalidateCache() {
			return this;
		}

		@Override
		public String toString() {
			return this.fieldInfo.owner.getClassName() + "." + this.fieldInfo.name;
		}
	}

	public static class VariableBasedBuilder extends Builder {

		public final InsnTree result;

		public VariableBasedBuilder(String name, InsnTree result) {
			this.exposedName = name;
			this.result = result;
		}

		@Override
		public String defaultExposedName() {
			return this.exposedName;
		}

		@Override
		public Builder resultClass(Class<?> clazz) {
			throw new UnsupportedOperationException("Result already specified");
		}

		@Override
		public Builder resultType(TypeInfo type) {
			throw new UnsupportedOperationException("Result already specified");
		}

		@Override
		public Builder invalidateCache() {
			return this;
		}

		@Override
		public UsageCallback callbackWithDeprecation(String type) {
			if (!this.arguments.isEmpty()) {
				throw new IllegalStateException("Can't provide arguments for variable");
			}
			return this.usageCallback;
		}

		@Override
		public @Nullable GatherResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			InsnTree result = this.result;
			if (this.explicitCast != null) {
				result = result.cast(parser, this.explicitCast, CastMode.EXPLICIT_THROW, false);
			}
			return new SimpleGatherResult(false, result);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.explicitCast != null ? this.explicitCast : this.result.getTypeInfo();
		}

		@Override
		public String toString() {
			return this.result.describe();
		}
	}

	public static interface Argument extends Typeable {

		public abstract @Nullable GatherResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs);

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
		public @Nullable GatherResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			InsnTree argument = providedArgs[this.requiredIndex];
			InsnTree castArgument = argument.cast(parser, this.type, CastMode.IMPLICIT_NULL, false);
			if (castArgument == null) return null;
			return new SimpleGatherResult(castArgument != argument, castArgument);
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
		public @Nullable GatherResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			return new SimpleGatherResult(false, this.tree);
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

	public static class ImportedArgument implements Argument {

		public final TypeInfo type;

		public ImportedArgument(TypeInfo type) {
			this.type = type;
		}

		public ImportedArgument(Class<?> clazz) {
			this(type(clazz));
		}

		@Override
		public @Nullable GatherResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			InsnTree object = parser.environment.getImportedObject(this.type);
			return object != null ? new SimpleGatherResult(false, object) : null;
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
		public TypeInfo getTypeInfo() {
			return this.type;
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
		public @Nullable GatherResult getFrom(ExpressionParser parser, InsnTree receiver, InsnTree[] providedArgs) {
			InsnTree castReceiver = receiver.cast(parser, this.type, CastMode.IMPLICIT_NULL, false);
			if (castReceiver == null) return null;
			return new SimpleGatherResult(castReceiver != receiver, castReceiver);
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

	public static abstract class GatherResult {

		public final boolean requiredCasting;
		public final InsnTree[] trees;

		public GatherResult(boolean casting, InsnTree... trees) {
			this.requiredCasting = casting;
			this.trees = trees;
		}

		public abstract InsnTree collect(ExpressionParser parser, CommonMode mode) throws ScriptParsingException;
	}

	public static class SimpleGatherResult extends GatherResult {

		public SimpleGatherResult(boolean casting, InsnTree tree) {
			super(casting, tree);
		}

		@Override
		public InsnTree collect(ExpressionParser parser, CommonMode mode) throws ScriptParsingException {
			return this.trees[0];
		}
	}
}