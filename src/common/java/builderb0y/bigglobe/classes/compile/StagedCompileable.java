package builderb0y.bigglobe.classes.compile;

import java.util.Arrays;
import java.util.Collection;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.classes.spec.ConstantFieldSpec;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class StagedCompileable<T_Context> {

	public CompileStep compileState = StagedCompileable.CompileStep.FRESH;

	public boolean canProgressTo(CompileStep state) {
		if (this.compileState.ordinal() < state.ordinal() - 1) {
			throw new IllegalStateException("Skipped a step!");
		}
		else if (this.compileState.ordinal() == state.ordinal() - 1) {
			this.compileState = state;
			return true;
		}
		else if (this.compileState.ordinal() == state.ordinal()) {
			return false;
		}
		else {
			throw new IllegalStateException("Progressing backwards!");
		}
	}

	public void tryProgressTo(CompileStep step, T_Context context) throws DetailedException {
		if (this.canProgressTo(step)) {
			step.action.execute(this, context);
		}
	}

	@MustBeInvokedByOverriders
	public void reference(T_Context context) throws DetailedException {}

	@MustBeInvokedByOverriders
	public void createTypeInfo(T_Context context) throws DetailedException {}

	@MustBeInvokedByOverriders
	public void verify(T_Context context) throws DetailedException {}

	@MustBeInvokedByOverriders
	public void createRepresentation(T_Context context) throws DetailedException {}

	@MustBeInvokedByOverriders
	public void compile(T_Context context) throws DetailedException {}

	public static abstract class BulkStagedCompiler<
		T_Context extends BulkStagedCompiler<T_Context, T_Element>,
		T_Element extends StagedCompileable<T_Context>
	>
	extends StagedCompileable<Void> {

		@Override
		public void tryProgressTo(CompileStep step, Void context) throws DetailedException {
			super.tryProgressTo(step, context);
			this.catchAll(this.getElementsToCompileForStep(step), step);
		}

		public void catchAll(Collection<? extends Holder<? extends T_Element>> elements, CompileStep step) throws DetailedException {
			if (!elements.isEmpty()) {
				DetailedException rootException = null;
				for (Holder<? extends T_Element> element : elements) try {
					element.value().tryProgressTo(step, this.getSelf());
				}
				catch (Exception exception) {
					if (rootException == null) rootException = new DetailedException("Exception " + step.description);
					rootException.addSuppressed(DetailedException.adapt(exception, (DetailedException e) -> e.prependLine("Element ID: " + UnregisteredObjectException.getID(element))));
				}
				if (rootException != null) throw rootException;
			}
		}

		public abstract T_Context getSelf();

		public abstract Collection<? extends Holder<? extends T_Element>> getElementsToCompileForStep(CompileStep step);
	}

	public static enum CompileStep {
		FRESH("doing nothing", null),
		REFERENCE("referencing elements", StagedCompileable::reference),
		RESOLVE("resolving types", StagedCompileable::createTypeInfo),
		VERIFY("verifying elements", StagedCompileable::verify),
		REPRESENT("creating representation", StagedCompileable::createRepresentation),
		COMPILE("compiling elements", StagedCompileable::compile);

		public static final CompileStep[]
			VALUES = values(),
			EXCEPT_FRESH = Arrays.copyOfRange(VALUES, 1, VALUES.length);

		public final String description;
		public final CompileAction action;

		CompileStep(String description, CompileAction action) {
			this.description = description;
			this.action = action;
		}

		@FunctionalInterface
		public static interface CompileAction {

			public abstract <T_Context> void execute(StagedCompileable<T_Context> compileable, T_Context context) throws DetailedException;
		}
	}
}