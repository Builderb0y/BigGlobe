package builderb0y.bigglobe.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
common logic for {@link AsyncRunner} and {@link AsyncConsumer}.
also has a few utility methods for doing common tasks.
*/
public abstract class Async<T_Result> implements AutoCloseable {

	/**
	set to true when using a debugger to make Async... sync.
	in other words, it will execute submitted tasks immediately, on the thread which submitted them.
	*/
	public static final boolean DEBUG_SYNC = false;

	public final Executor executor;
	public final LinkedList<CompletableFuture<T_Result>> waitingOn = new LinkedList<>();

	public Async(Executor executor) {
		this.executor = executor != null ? executor : ForkJoinPool.commonPool();
	}

	//////////////////////////////// utility methods ////////////////////////////////

	public static <T> void forEach(Executor executor, T[] array, Consumer<T> action) {
		if (array.length == 0) return;
		try (AsyncRunner async = new AsyncRunner(executor)) {
			for (T element : array) {
				async.submit(() -> action.accept(element));
			}
		}
	}

	public static <T> void forEach(Executor executor, List<T> list, Consumer<T> action) {
		if (list.isEmpty()) return;
		try (AsyncRunner async = new AsyncRunner(executor)) {
			for (T element : list) {
				async.submit(() -> action.accept(element));
			}
		}
	}

	public static void loop(Executor executor, int times, IntConsumer action) {
		loop(executor, 0, times, 1, action);
	}

	public static void loop(Executor executor, int startInclusive, int endExclusive, int step, IntConsumer action) {
		if (startInclusive >= endExclusive) return;
		try (AsyncRunner async = new AsyncRunner(executor)) {
			for (int number = startInclusive; number < endExclusive; number += step) {
				int number_ = number;
				async.submit(() -> action.accept(number_));
			}
		}
	}

	public static void repeat(Executor executor, int times, Runnable action) {
		if (times <= 0) return;
		try (AsyncRunner async = new AsyncRunner(executor)) {
			for (int time = 0; time < times; time++) {
				async.submit(action);
			}
		}
	}

	public static <T> void setEach(Executor executor, T[] array, IntFunction<T> supplier) {
		if (array.length == 0) return;
		try (AsyncRunner async = new AsyncRunner(executor)) {
			for (int index = 0, length = array.length; index < length; index++) {
				int index_ = index;
				async.submit(() -> array[index_] = supplier.apply(index_));
			}
		}
	}

	public static <T> void setEach(Executor executor, List<T> list, IntFunction<T> supplier) {
		if (list.isEmpty()) return;
		try (AsyncRunner async = new AsyncRunner(executor)) {
			for (int index = 0, size = list.size(); index < size; index++) {
				int index_ = index;
				async.submit(() -> list.set(index_, supplier.apply(index_)));
			}
		}
	}

	//////////////////////////////// shared logic ////////////////////////////////

	public void begin(CompletableFuture<T_Result> future) {
		this.waitingOn.add(future);
	}

	@Override
	public void close() {
		CompletionException exception = null;
		for (CompletableFuture<T_Result> future; (future = this.waitingOn.pollFirst()) != null;) {
			try {
				this.finish(future);
			}
			catch (Throwable throwable) {
				try {
					if (exception == null) {
						exception = new CompletionException("Some tasks failed to complete, see below.", null);
					}
					exception.addSuppressed(throwable);
				}
				catch (Throwable ignored) {
					//better to swallow the exception than to risk race conditions
					//caused by aborting the enclosing loop too early.
				}
			}
		}
		if (exception != null) throw exception;
	}

	public abstract void finish(CompletableFuture<T_Result> future);
}