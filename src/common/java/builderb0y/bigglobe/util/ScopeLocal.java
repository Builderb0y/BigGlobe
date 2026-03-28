package builderb0y.bigglobe.util;

public class ScopeLocal<T> {

	public final ThreadLocal<T> threadLocal = new ThreadLocal<>();

	public T getCurrent() {
		return this.threadLocal.get();
	}

	public <X extends Throwable> void run(T value, ThrowingRunnable<X> runnable) throws X {
		T old = this.threadLocal.get();
		this.threadLocal.set(value);
		try {
			runnable.run();
		}
		finally {
			this.threadLocal.set(old);
		}
	}

	public <R, X extends Throwable> R get(T value, ThrowingSupplier<R, X> supplier) throws X {
		T old = this.threadLocal.get();
		this.threadLocal.set(value);
		try {
			return supplier.get();
		}
		finally {
			this.threadLocal.set(old);
		}
	}

	public <R, X extends Throwable> void accept(T value, R object, ThrowingConsumer<R, X> consumer) throws X {
		T old = this.threadLocal.get();
		this.threadLocal.set(value);
		try {
			consumer.accept(object);
		}
		finally {
			this.threadLocal.set(old);
		}
	}

	public <R extends T, X extends Throwable> void accept(R value, ThrowingConsumer<R, X> consumer) throws X {
		this.accept(value, value, consumer);
	}

	public <In, Out, X extends Throwable> Out apply(T value, In object, ThrowingFunction<In, Out, X> function) throws X {
		T old = this.threadLocal.get();
		this.threadLocal.set(value);
		try {
			return function.apply(object);
		}
		finally {
			this.threadLocal.set(old);
		}
	}

	public <In extends T, Out, X extends Throwable> Out apply(In value, ThrowingFunction<In, Out, X> function) throws X {
		return this.apply(value, value, function);
	}
}