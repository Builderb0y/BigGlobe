package builderb0y.scripting.optimization;

import java.util.stream.Stream;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import builderb0y.bigglobe.BigGlobeMod;

public interface ClassOptimizer {

	public static final ClassOptimizer DEFAULT = new RepeatingOptimizer(
		new MultiOptimizer(
			Stream.of(
				LineNumberOptimizer.INSTANCE,
				UnreachableCodeOptimizer.INSTANCE,
				DoubleJumpOptimizer.INSTANCE,
				JumpToReturnOptimizer.INSTANCE,
				JumpToNextInstructionOptimizer.INSTANCE,
				ConstantJumpOptimizer.INSTANCE,
				PopOptimizer.INSTANCE,
				UnusedLocalVariableRemover.INSTANCE,
				InlineVariableOptimizer.INSTANCE,
				UnnecessaryVariableOptimizer.INSTANCE
			)
			.filter((ClassOptimizer optimizer) -> {
				String key = "bigglobe.bytecode.optimization." + optimizer.getClass().getSimpleName();
				String value = System.getProperty(key);
				if (value != null) {
					if (value.equalsIgnoreCase("false")) return false;
					if (value.equalsIgnoreCase("true")) return true;
					BigGlobeMod.LOGGER.warn("-D" + key + " is set to " + value + " but true or false was expected. Defaulting to true.");
				}
				return true;
			})
			.toArray(ClassOptimizer[]::new)
		)
	);

	public abstract boolean optimize(ClassNode clazz);

	public static interface MethodOptimizer extends ClassOptimizer {

		public abstract boolean optimize(ClassNode clazz, MethodNode method);

		@Override
		@SuppressWarnings("NonShortCircuitBooleanExpression")
		public default boolean optimize(ClassNode clazz) {
			boolean changedAny = false;
			for (MethodNode method : clazz.methods) {
				changedAny |= this.optimize(clazz, method);
			}
			return changedAny;
		}
	}

	public static class MultiOptimizer implements ClassOptimizer {

		public final ClassOptimizer[] optimizers;

		public MultiOptimizer(ClassOptimizer... optimizers) {
			this.optimizers = optimizers;
		}

		@Override
		@SuppressWarnings("NonShortCircuitBooleanExpression")
		public boolean optimize(ClassNode clazz) {
			boolean changedAny = false;
			for (ClassOptimizer optimizer : this.optimizers) {
				changedAny |= optimizer.optimize(clazz);
			}
			return changedAny;
		}
	}

	public static class RepeatingOptimizer implements ClassOptimizer {

		public final ClassOptimizer optimizer;

		public RepeatingOptimizer(ClassOptimizer optimizer) {
			this.optimizer = optimizer;
		}

		@Override
		public boolean optimize(ClassNode clazz) {
			boolean changed = false;
			while (this.optimizer.optimize(clazz)) {
				changed = true;
			}
			return changed;
		}
	}
}