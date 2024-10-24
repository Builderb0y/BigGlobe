package builderb0y.scripting.environments;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.ScriptInterfaces.IntSupplier;
import builderb0y.scripting.TestCommon;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;
import static org.junit.jupiter.api.Assertions.*;

public class HandlerBuilderTest extends TestCommon {

	@Test
	public void test() throws ScriptParsingException {
		assertEquals(
			2,
			new ScriptParser<>(IntSupplier.class, "addOne(1i)")
			.configureEnvironment((MutableScriptEnvironment environment) -> {
				environment.addFunction(
					"addOne",
					Handlers.inCaller("add1").addRequiredArgument(int.class).buildFunction()
				);
			})
			.parse(new ScriptClassLoader())
			.getAsInt()
		);
		assertEquals(
			2,
			new ScriptParser<>(IntSupplier.class, "1i .addOne()")
			.configureEnvironment((MutableScriptEnvironment environment) -> {
				environment.addMethod(
					type(int.class),
					"addOne",
					Handlers.inCaller("add1").addReceiverArgument(int.class).buildMethod()
				);
			})
			.parse(new ScriptClassLoader())
			.getAsInt()
		);
		assertEquals(
			2,
			new ScriptParser<>(IntSupplier.class, "addOne(1i)")
			.configureEnvironment((MutableScriptEnvironment environment) -> {
				environment.addFunction(
					"addOne",
					Handlers.inCaller("add1").addNestedArgument(
						Handlers.inCaller("newBox").addRequiredArgument(int.class)
					)
					.buildFunction()
				);
			})
			.parse(new ScriptClassLoader())
			.getAsInt()
		);
		assertEquals(
			2,
			new ScriptParser<>(IntSupplier.class, "1i .plusOne")
			.configureEnvironment((MutableScriptEnvironment environment) -> {
				environment.addField(
					TypeInfos.INT,
					"plusOne",
					Handlers.inCaller("add1").addReceiverArgument(TypeInfos.INT).buildField()
				);
			})
			.parse(new ScriptClassLoader())
			.getAsInt()
		);
		assertEquals(
			2,
			new ScriptParser<>(IntSupplier.class, "two")
			.configureEnvironment((MutableScriptEnvironment environment) -> {
				environment.addVariable(
					"two",
					Handlers.inCaller("add1").addImplicitArgument(ldc(1)).buildVariable()
				);
			})
			.parse(new ScriptClassLoader())
			.getAsInt()
		);
	}

	public static int add1(int x) {
		return x + 1;
	}

	public static class Box {

		public int value;

		public Box(int value) {
			this.value = value;
		}
	}

	public static Box newBox(int value) {
		return new Box(value);
	}

	public static int add1(Box box) {
		return box.value + 1;
	}
}