package builderb0y.scripting.parsing;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.objectweb.asm.Type;

import builderb0y.autocodec.util.TypeFormatter;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.optimization.ClassOptimizer;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/** implementation of ExpressionParser which compiles into functional interfaces. */
public class ScriptParser<I> extends ExpressionParser {

	public Class<I> implementingClass;
	public Method implementingMethod;
	public @Nullable String debugName;

	public ScriptParser(
		Class<I> implementingClass,
		Method implementingMethod,
		String input,
		@Nullable String debugName,
		ClassCompileContext clazz,
		MethodCompileContext method
	) {
		super(input, clazz, method);
		this.implementingClass  = implementingClass;
		this.implementingMethod = implementingMethod;
		this.debugName          = debugName;

		clazz.addNoArgConstructor(ACC_PUBLIC);

		MethodCompileContext getSource = clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING);
		return_(ldc(input)).emitBytecode(getSource);
		getSource.endCode();

		MethodCompileContext getDebugName = clazz.newMethod(ACC_PUBLIC, "getDebugName", TypeInfos.STRING);
		return_(ldc(debugName)).emitBytecode(getDebugName);
		getDebugName.endCode();

		StringBuilder toString = new StringBuilder(input.length() + 128).append(TypeFormatter.getSimpleClassName(implementingClass)).append("::").append(implementingMethod.getName());
		if (debugName != null) toString.append(" (").append(debugName).append(')');
		clazz.addToString(toString.toString());
	}

	public ScriptParser(
		Class<I> implementingClass,
		Method implementingMethod,
		String input,
		@Nullable String debugName,
		ClassCompileContext clazz
	) {
		this(
			implementingClass,
			implementingMethod,
			input,
			debugName,
			clazz,
			clazz.newMethod(
				ACC_PUBLIC,
				implementingMethod.getName(),
				type(implementingMethod.getReturnType()),
				Arrays.stream(implementingMethod.getParameters())
				.peek((Parameter parameter) -> {
					if (!parameter.isNamePresent()) {
						throw new IllegalArgumentException("Attempt to create script from interface with unnamed parameters: " + implementingClass);
					}
				})
				.map((Parameter parameter) -> new LazyVarInfo(parameter.getName(), type(parameter.getType())))
				.toArray(LazyVarInfo.ARRAY_FACTORY)
			)
		);
	}

	public ScriptParser(
		Class<I> implementingClass,
		Method implementingMethod,
		String input,
		@Nullable String debugName
	) {
		this(
			implementingClass,
			implementingMethod,
			input,
			debugName,
			new ClassCompileContext(
				ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
				ClassType.CLASS,
				Type.getInternalName(ScriptParser.class) + '$' + (debugName != null ? debugName : "Generated") + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				TypeInfos.OBJECT,
				Script.class.isAssignableFrom(implementingClass)
				? new TypeInfo[] { type(implementingClass) }
				: new TypeInfo[] { type(implementingClass), type(Script.class) }
			)
		);
	}

	public ScriptParser(Class<I> implementingClass, String input, String debugName) {
		this(implementingClass, findImplementingMethod(implementingClass), input, debugName);
	}

	@TestOnly
	public ScriptParser(Class<I> implementingClass, String input) {
		this(implementingClass, input, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ScriptParser<I> addEnvironment(ScriptEnvironment environment) {
		return (ScriptParser<I>)(super.addEnvironment(environment));
	}

	@Override
	@SuppressWarnings("unchecked")
	public ScriptParser<I> configureEnvironment(Consumer<MutableScriptEnvironment> configurator) {
		return (ScriptParser<I>)(super.configureEnvironment(configurator));
	}

	public static Method findImplementingMethod(Class<?> implementingClass) {
		Method implementingMethod = null;
		for (Method method : implementingClass.getMethods()) {
			if (
				Modifier.isAbstract(method.getModifiers()) &&
				method.getDeclaringClass() != Script.class
			) {
				if (implementingMethod == null) implementingMethod = method;
				else throw new IllegalArgumentException("implementingClass must have exactly 1 abstract method.");
			}
		}
		if (implementingMethod == null) {
			throw new IllegalArgumentException("implementingClass must have exactly 1 abstract method.");
		}
		return implementingMethod;
	}

	public I parse() throws ScriptParsingException {
		this.toBytecode();
		return this.toScript();
	}

	public void toBytecode() throws ScriptParsingException {
		this.parseEntireInput().emitBytecode(this.method);
		this.method.endCode();
	}

	public I toScript() throws ScriptParsingException {
		try {
			return (
				this
				.compile()
				.asSubclass(this.implementingClass)
				.getDeclaredConstructor((Class<?>[])(null))
				.newInstance((Object[])(null))
			);
		}
		catch (Throwable throwable) {
			throw new ScriptParsingException(this.fatalError().toString(), throwable, null);
		}
	}

	@Override
	public StringBuilder fatalError() {
		return (
			super.fatalError()
			.append("Implementation interface: ").append(this.implementingClass.getName()).append('\n')
			.append("Implementation method: ").append(this.implementingMethod).append('\n')
		);
	}
}