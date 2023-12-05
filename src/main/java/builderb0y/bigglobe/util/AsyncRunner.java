package builderb0y.bigglobe.util;

import java.util.concurrent.CompletableFuture;

/**
an AsyncRunner runs tasks asynchronously and concurrently,
then waits for them to finish.
this allows expensive computations to be done in parallel,
while still enforcing something similar to flow control for task completion.
tasks can be submitted at any time,
and tasks will be finished when {@link #close()} is called.
any tasks which are not finished when {@link #close()} is called
will be waited for before the calling thread resumes execution.
example usage: {@code
	try (AsyncRunner async = new AsyncRunner()) {
		async.submit(() -> expensiveOperation(1));
		async.submit(() -> expensiveOperation(2));
		async.submit(() -> expensiveOperation(3));
		//all 3 tasks are worked on in parallel.
	}
	//closing waits for all 3 tasks to finish running.
}
note: while this class assists with parallel computation,
it is not itself thread safe. do not submit new tasks concurrently!

see also: {@link AsyncConsumer}.
*/
public class AsyncRunner extends Async<Void> {

	public void submit(Runnable runnable) {
		this.begin(CompletableFuture.runAsync(runnable));
	}

	@Override
	public void finish(CompletableFuture<Void> future) {
		future.join();
	}
}