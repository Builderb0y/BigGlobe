package builderb0y.scripting.environments;

import java.lang.reflect.*;
import java.util.*;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.GetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.GetStaticInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassScriptEnvironment implements ScriptEnvironment {

	public final Class<?> clazz;
	public final TypeInfo typeInfo;
	public final String simpleName;
	public final Map<String, IsGeneric<FieldInfo>>
		staticFields = new HashMap<>(),
		instanceFields = new HashMap<>();
	public final Map<String, List<IsGeneric<MethodInfo>>>
		staticMethods = new HashMap<>(),
		instanceMethods = new HashMap<>();
	public final List<MethodInfo> constructors = new ArrayList<>(8);

	public static record IsGeneric<T>(T value, boolean isGeneric) {

		public InsnTree wrap(InsnTree value) {
			return this.isGeneric ? automaticCast(value) : value;
		}
	}

	public ClassScriptEnvironment(Class<?> clazz) {
		this.clazz = clazz;
		this.typeInfo = TypeInfo.of(clazz);
		this.simpleName = clazz.getSimpleName();
		for (Field field : clazz.getDeclaredFields()) {
			if (this.shouldExposeField(field)) {
				(Modifier.isStatic(field.getModifiers()) ? this.staticFields : this.instanceFields)
				.put(field.getName(), new IsGeneric<>(FieldInfo.forField(field), field.getGenericType() instanceof TypeVariable<?>));
			}
		}
		for (Method method : clazz.getDeclaredMethods()) {
			if (this.shouldExposeMethod(method)) {
				(Modifier.isStatic(method.getModifiers()) ? this.staticMethods : this.instanceMethods)
				.computeIfAbsent(method.getName(), $ -> new ArrayList<>(4))
				.add(new IsGeneric<>(MethodInfo.forMethod(method), method.getGenericReturnType() instanceof TypeVariable<?>));
			}
		}
		for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
			if (this.shouldExposeConstructor(constructor)) {
				this.constructors.add(MethodInfo.forConstructor(constructor));
			}
		}
	}

	public boolean shouldExposeField(Field field) {
		return Modifier.isPublic(field.getModifiers());
	}

	public boolean shouldExposeMethod(Method method) {
		return Modifier.isPublic(method.getModifiers());
	}

	public boolean shouldExposeConstructor(Constructor<?> constructor) {
		return Modifier.isPublic(constructor.getModifiers());
	}

	@Override
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		ConstantValue constant = receiver.getConstantValue();
		if (constant.isConstant() && constant.asJavaObject() instanceof TypeInfo type && type.extendsOrImplements(this.typeInfo)) {
			IsGeneric<FieldInfo> field = this.staticFields.get(name);
			if (field != null) return field.wrap(new GetStaticInsnTree(field.value));
		}
		else if (receiver.getTypeInfo().extendsOrImplements(this.typeInfo)) {
			IsGeneric<FieldInfo> field = this.instanceFields.get(name);
			if (field != null) return field.wrap(new GetFieldInsnTree(receiver, field.value));
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		ConstantValue constant = receiver.getConstantValue();
		if (constant.isConstant() && constant.asJavaObject() instanceof TypeInfo type && type.extendsOrImplements(this.typeInfo)) {
			if (name.equals("new")) {
				for (MethodInfo constructor : this.constructors) {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, constructor, CastMode.IMPLICIT_NULL, arguments);
					if (castArguments != null) {
						return newInstance(constructor, castArguments);
					}
				}
			}
			else {
				List<IsGeneric<MethodInfo>> methods = this.staticMethods.get(name);
				if (methods != null) return MutableScriptEnvironment.getBestArgumentsGeneric(
					parser,
					name,
					methods,
					IsGeneric::value,
					arguments,
					(method, castArguments) -> {
						return method.wrap(invokeStatic(method.value, castArguments));
					}
				);
				/*
				for (IsGeneric<MethodInfo> method : this.staticMethods.getOrDefault(name, Collections.emptyList())) {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method.value, CastMode.IMPLICIT_NULL, arguments);
					if (castArguments != null) {
						return method.wrap(invokeStatic(method.value, castArguments));
					}
				}
				*/
			}
		}
		else if (receiver.getTypeInfo().extendsOrImplements(this.typeInfo)) {
			List<IsGeneric<MethodInfo>> methods = this.instanceMethods.get(name);
			if (methods != null) return MutableScriptEnvironment.getBestArgumentsGeneric(
				parser,
				name,
				methods,
				IsGeneric::value,
				arguments,
				(method, castArguments) -> {
					if (this.typeInfo.type.isInterface) {
						return method.wrap(invokeInterface(receiver, method.value, castArguments));
					}
					else {
						return method.wrap(invokeVirtual(receiver, method.value, castArguments));
					}
				}
			);
			/*
			for (IsGeneric<MethodInfo> method : this.instanceMethods.getOrDefault(name, Collections.emptyList())) {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method.value, CastMode.IMPLICIT_NULL, arguments);
				if (castArguments != null) {
					if (this.typeInfo.type.isInterface) {
						return method.wrap(invokeInterface(receiver, method.value, castArguments));
					}
					else {
						return method.wrap(invokeVirtual(receiver, method.value, castArguments));
					}
				}
			}
			*/
		}
		return null;
	}

	@Override
	public @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		return name.equals(this.simpleName) ? this.typeInfo : null;
	}

	@Override
	public String toString() {
		return "ClassScriptEnvironment: { " + this.clazz + " }";
	}
}