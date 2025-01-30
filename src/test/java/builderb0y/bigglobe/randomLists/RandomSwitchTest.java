package builderb0y.bigglobe.randomLists;

import java.util.Locale;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.util.TypeInfos;

import static org.junit.jupiter.api.Assertions.*;

public class RandomSwitchTest {

	@FunctionalInterface
	public static interface RandomToIntScript {

		public abstract int get(RandomGenerator random);
	}

	public void test(RandomGenerator random, String source) throws Throwable {
		RandomToIntScript script = (
			new ScriptParser<>(RandomToIntScript.class, source)
			.configureEnvironment(RandomScriptEnvironment.create(InsnTrees.load("random", InsnTrees.type(RandomGenerator.class))))
			.parse(new ScriptClassLoader())
		);
		int sum = 0;
		for (int i = 0; i < 1_000_000; i++) {
			int next = script.get(random);
			if (next < 0) fail("next < 0");
			sum += next;
		}
		if (Math.abs(sum - 250_000) > 5000) fail("Incorrect distribution");
	}

	@FunctionalInterface
	public static interface SeedToIntScript {

		public abstract int get(long seed);
	}

	public void test(String source) throws Throwable {
		SeedToIntScript script = (
			new ScriptParser<>(SeedToIntScript.class, source)
			.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
			.configureEnvironment((MutableScriptEnvironment environment) -> environment.addVariableLoad("seed", TypeInfos.LONG))
			.parse(new ScriptClassLoader())
		);
		int sum = 0;
		for (int i = 0; i < 1_000_000; i++) {
			int next = script.get(i * Permuter.PHI64);
			if (next < 0) fail("next < 0");
			sum += next;
		}
		if (Math.abs(sum - 250_000) > 5000) fail("Incorrect distribution");
	}

	@Test
	public void testAll() throws Throwable {
		boolean[] falseTrue = { false, true };
		for (TypeInfo.Sort sort : new TypeInfo.Sort[] { TypeInfo.Sort.INT, TypeInfo.Sort.LONG, TypeInfo.Sort.FLOAT, TypeInfo.Sort.DOUBLE }) {
			String typeName = sort.name().toLowerCase(Locale.ROOT);
			String suffix = switch (sort) {
				case INT -> "I";
				case LONG -> "L";
				case FLOAT -> ".0I";
				case DOUBLE -> ".0L";
				default -> throw new AssertionError();
			};
			for (boolean useSeed : falseTrue) {
				for (boolean secondVariable : falseTrue) {
					for (boolean firstVariable : falseTrue) {
						StringBuilder source = new StringBuilder();
						if (firstVariable) source.append(typeName).append(" a = 3").append(suffix).append('\n');
						if (secondVariable) source.append(typeName).append(" b = 1").append(suffix).append('\n');
						source.append(useSeed ? "seed" : "random").append(".switch (\n");
						source.append('\t');
						if (firstVariable) source.append('a');
						else source.append('3').append(suffix);
						source.append(": 0,\n");
						source.append('\t');
						if (secondVariable) source.append('b');
						else source.append('1').append(suffix);
						source.append(": 1,\n");
						source.append("\tdefault: -1\n");
						source.append(')');
						if (useSeed) this.test(source.toString());
						else this.test(new Permuter(12345L), source.toString());
					}
				}
			}
		}
	}
}