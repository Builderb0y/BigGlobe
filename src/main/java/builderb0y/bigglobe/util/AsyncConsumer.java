package builderb0y.bigglobe.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
an AsyncConsumer is similar to an {@link AsyncRunner},
but the tasks are allowed to return a result.
the processing of that result is done by the {@link #terminator},
which is supplied in the constructor.
upon finishing, the terminator will be invoked on
all tasks' results in the order they were submitted.
example usage: {@code
	try (AsyncConsumer<String> async = new AsyncConsumer<>(System.out::println)) {
		async.submit(() -> expensiveOperation(1));
		async.submit(() -> expensiveOperation(2));
		async.submit(() -> expensiveOperation(3));
		//all 3 tasks are worked on in parallel.
	}
	//closing waits for all 3 tasks to finish running,
	//and in this case prints their results to System.out in order.
}
note: while this class assists with parallel computation,
it is not itself thread safe. do not submit new tasks concurrently!

see also: {@link AsyncRunner}.
*/
public class AsyncConsumer<T> extends Async<T> {

	public final Consumer<T> terminator;

	public AsyncConsumer(Consumer<T> terminator) {
		this.terminator = terminator;
	}

	public AsyncConsumer(Executor executor, Consumer<T> terminator) {
		super(executor);
		this.terminator = terminator;
	}

	public void submit(Supplier<T> supplier) {
		if (DEBUG_SYNC) {
			this.begin(CompletableFuture.completedFuture(supplier.get()));
		}
		else {
			this.begin(CompletableFuture.supplyAsync(supplier, this.executor));
		}
	}

	@Override
	public void finish(CompletableFuture<T> future) {
		this.terminator.accept(future.join());
	}
}