package builderb0y.bigglobe.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import builderb0y.autocodec.util.AutoCodecUtil;

@SuppressWarnings("unchecked")
public class BudgetStableValue<T> {

	public static final VarHandle VALUE;
	static {
		try {
			VALUE = MethodHandles.lookup().findVarHandle(BudgetStableValue.class, "value", Object.class).withInvokeExactBehavior();
		}
		catch (Exception exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public T value;
	public final ReentrantLock lock = new ReentrantLock();

	public BudgetStableValue() {}

	public BudgetStableValue(T value) {
		this.value = value;
	}

	public State getState() {
		if (this.value != null) return State.COMPUTED;
		if (VALUE.getVolatile(this) != null) return State.COMPUTED;
		if (this.lock.isLocked()) return State.COMPUTING;
		return State.UNCOMPUTED;
	}

	public void _set(T value) {
		VALUE.setVolatile(this, value);
	}

	public T getBlocking() {
		T value = this.value;
		if (value == null) {
			this.lock.lock();
			try {
				value = (T)(VALUE.getVolatile(this));
			}
			finally {
				this.lock.unlock();
			}
			if (value == null) {
				throw new NoSuchElementException();
			}
		}
		return value;
	}

	public T getNonBlocking() {
		T value = this.value;
		if (value == null) {
			value = (T)(VALUE.getVolatile(this));
			if (value == null) {
				throw new NoSuchElementException();
			}
		}
		return value;
	}

	public void setBlocking(T contents) {
		Objects.requireNonNull(contents, "contents");
		if (this.value == null) {
			this.lock.lock();
			try {
				if (VALUE.getVolatile(this) == null) {
					this._set( contents);
				}
			}
			finally {
				this.lock.unlock();
			}
		}
	}

	public void setNonBlocking(T contents) {
		Objects.requireNonNull(contents, "contents");
		if (this.value == null && this.lock.tryLock()) try {
			if (VALUE.getVolatile(this) == null) {
				this._set(contents);
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	public void setBlocking(Supplier<? extends T> supplier) {
		Objects.requireNonNull(supplier, "supplier");
		if (this.value == null) {
			this.lock.lock();
			try {
				if (VALUE.getVolatile(this) == null) {
					this._set( Objects.requireNonNull(supplier.get(), "supplier.get() returned null."));
				}
			}
			finally {
				this.lock.unlock();
			}
		}
	}

	public void setNonBlocking(Supplier<? extends T> supplier) {
		Objects.requireNonNull(supplier, "supplier");
		if (this.value == null && this.lock.tryLock()) try {
			if (VALUE.getVolatile(this) == null) {
				this._set(Objects.requireNonNull(supplier.get(), "supplier.get() returned null."));
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	public T getOrSetBlocking(Supplier<? extends T> supplier) {
		Objects.requireNonNull(supplier, "supplier");
		T value = this.value;
		if (value == null) {
			this.lock.lock();
			try {
				value = (T)(VALUE.getVolatile(this));
				if (value == null) {
					this._set(value = Objects.requireNonNull(supplier.get(), "supplier.get() returned null"));
				}
			}
			finally {
				this.lock.unlock();
			}
		}
		return value;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + this.value;
	}

	public static enum State {
		UNCOMPUTED,
		COMPUTING,
		COMPUTED;
	}
}