package builderb0y.bigglobe.util;

import java.lang.ScopedValue.CallableOp;

import org.jetbrains.annotations.Nullable;

/** better syntax, and allow nulls. */
public class BetterScopedValue<T> {

	public static final Object NULL = new Object();

	public final ScopedValue<Object> value = ScopedValue.newInstance();

	public static <T> Object wrapNull(T object) {
		return object != null ? object : NULL;
	}

	@SuppressWarnings("unchecked")
	public static <T> T unwrapNull(Object object) {
		return object != NULL ? ((T)(object)) : null;
	}

	public void run(T value, Runnable runnable) {
		ScopedValue.where(this.value, wrapNull(value)).run(runnable);
	}

	public <R, X extends Throwable> R get(T value, CallableOp<R, X> supplier) throws X {
		return ScopedValue.where(this.value, wrapNull(value)).call(supplier);
	}

	public @Nullable T currentValue() {
		return unwrapNull(this.value.orElse(NULL));
	}
}