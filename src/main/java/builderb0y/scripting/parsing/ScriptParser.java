package builderb0y.scripting.parsing;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.function.Consumer;

import org.objectweb.asm.Type;

import builderb0y.autocodec.util.TypeFormatter;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.ClassType;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/** implementation of ExpressionParser which compiles into functional interfaces. */
public class ScriptParser<I> extends ExpressionParser {

	public Class<I> implementingClass;
	public Method implementingMethod;

	public ScriptParser(
		Class<I> implementingClass,
		Method implementingMethod,
		String input,
		ClassCompileContext clazz,
		MethodCompileContext method
	) {
		super(input, clazz, method);
		this.implementingClass  = implementingClass;
		this.implementingMethod = implementingMethod;

		clazz.addNoArgConstructor(ACC_PUBLIC);

		clazz
		.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING)
		.scopes
		.withScope(return_(ldc(input))::emitBytecode);

		clazz.addToString(TypeFormatter.getSimpleClassName(implementingClass) + '.' + implementingMethod.getName() + "(): " + input);
	}

	public ScriptParser(
		Class<I> implementingClass,
		Method implementingMethod,
		String input,
		ClassCompileContext clazz
	) {
		this(
			implementingClass,
			implementingMethod,
			input,
			clazz,
			clazz.newMethod(
				ACC_PUBLIC,
				implementingMethod.getName(),
				type(implementingMethod.getReturnType()),
				types(implementingMethod.getParameterTypes())
			)
		);
	}

	public ScriptParser(
		Class<I> implementingClass,
		Method implementingMethod,
		String input
	) {
		this(
			implementingClass,
			implementingMethod,
			input,
			new ClassCompileContext(
				ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
				ClassType.CLASS,
				Type.getInternalName(ScriptParser.class) + "$Generated_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				TypeInfos.OBJECT,
				Script.class.isAssignableFrom(implementingClass)
				? new TypeInfo[] { type(implementingClass) }
				: new TypeInfo[] { type(implementingClass), type(Script.class) }
			)
		);
	}

	public ScriptParser(Class<I> implementingClass, String input) {
		this(implementingClass, findImplementingMethod(implementingClass), input);
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
				Modifier.isAbstract(method.getModifiers()) && !(
					method.getName() == "getSource" &&
					method.getParameterCount() == 0
				)
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
		this.method.scopes.pushScope();
		this.method.addThis();
		for (Parameter parameter : this.implementingMethod.getParameters()) {
			this.method.newParameter(parameter.getName(), type(parameter.getType()));
		}
		this.parseEntireInput().emitBytecode(this.method);
		this.method.scopes.popScope();
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