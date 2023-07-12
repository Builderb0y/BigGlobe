package builderb0y.scripting.optimization;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public interface ClassOptimizer {

	public static final ClassOptimizer DEFAULT = new RepeatingOptimizer(
		new MultiOptimizer(
			LineNumberOptimizer.INSTANCE,
			UnreachableCodeOptimizer.INSTANCE,
			DoubleJumpOptimizer.INSTANCE,
			JumpToNextInstructionOptimizer.INSTANCE
		)
	);

	public abstract boolean optimize(ClassNode clazz);

	public static interface MethodOptimizer extends ClassOptimizer {

		public abstract boolean optimize(MethodNode method);

		@Override
		@SuppressWarnings("NonShortCircuitBooleanExpression")
		public default boolean optimize(ClassNode clazz) {
			boolean changedAny = false;
			for (MethodNode method : clazz.methods) {
				changedAny |= this.optimize(method);
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