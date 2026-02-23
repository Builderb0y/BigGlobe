package builderb0y.bigglobe.util;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.Nullable;

public class TimestampedComputingCache<T_Key, T_Value> {

	/** @see Units */
	public final double retainMillisecondsPerByte;
	public final ReentrantLock globalLock;
	public final BackingMap<T_Key, ValueHolder<T_Value>> values;
	public final AtomicInteger presentCount;

	/**
	@param retainTime one of
		{@link Units#nanoseconds(double)},
		{@link Units#microseconds(double)},
		{@link Units#milliseconds(double)}, or
		{@link Units#seconds(double)}.
	@param retainData one of
		{@link Units#bytes(double)},
		{@link Units#kilobytes(double)},
		{@link Units#megabytes(double)}, or
		{@link Units#gigabytes(double)}.
	@see Units
	*/
	public TimestampedComputingCache(double retainTime, double retainData) {
		this.retainMillisecondsPerByte = retainTime / retainData;
		this.globalLock = new ReentrantLock();
		this.values = new BackingMap<>(256);
		this.presentCount = new AtomicInteger();
	}

	public ValueHolder<T_Value> getHolder(T_Key key, boolean create) {
		this.globalLock.lock();
		try {
			ValueHolder<T_Value> holder = this.values.getAndMoveToLast(key);
			if (holder != null) {
				holder.timestamp = System.currentTimeMillis();
			}
			else if (create) {
				this.values.putAndMoveToLast(key, holder = new ValueHolder<>());
			}
			this.doPurge();
			return holder;
		}
		finally {
			this.globalLock.unlock();
		}
	}

	public int size() {
		return this.values.size();
	}

	public T_Value computeIfUnknown(T_Key key, Function<? super T_Key, ? extends T_Value> computer) {
		return this.checkOrCompute(key, this.getHolder(key, true), computer);
	}

	public T_Value check(T_Key key) {
		ValueHolder<T_Value> holder = this.getHolder(key, false);
		return holder != null ? this.checkOrCompute(key, holder, null) : null;
	}

	public CompletableFuture<T_Value> getAsync(Executor executor, T_Key key, Function<? super T_Key, ? extends T_Value> computer) {
		ValueHolder<T_Value> holder = this.getHolder(key, true);
		return CompletableFuture.supplyAsync(
			() -> this.checkOrCompute(key, holder, computer),
			executor
		);
	}

	public T_Value checkOrCompute(T_Key key, ValueHolder<T_Value> holder, Function<? super T_Key, ? extends T_Value> computer) {
		assert !this.globalLock.isHeldByCurrentThread() : "Attempting to acquire local and global lock at the same time!";
		holder.lock.lock();
		try {
			if (holder.state != ValueHolder.UNKNOWN) {
				return holder.value;
			}
			else if (computer != null) {
				//assume that computing the value will take a small
				//amount of time compared to our retainMillisecondsPerByte.
				//if this assumption holds, then it is not worthwhile
				//to move the holder to the end of the map again.
				T_Value oldValue = holder.get();
				T_Value newValue = computer.apply(key);
				holder.set(newValue);
				if ((oldValue != null) != (newValue != null)) {
					this.presentCount.addAndGet(newValue != null ? 1 : -1);
				}
				return newValue;
			}
			else {
				return null;
			}
		}
		finally {
			holder.lock.unlock();
		}
	}

	public void purge() {
		this.globalLock.lock();
		try {
			this.doPurge();
		}
		finally {
			this.globalLock.unlock();
		}
	}

	public void doPurge() {
		assert this.globalLock.isHeldByCurrentThread();
		long deadline = System.currentTimeMillis() - ((long)(this.retainMillisecondsPerByte * Runtime.getRuntime().freeMemory()));
		int removed = 0;
		while (!this.values.isEmpty()) {
			ValueHolder<T_Value> value = this.values.firstValue();
			if (value.timestamp >= deadline) break;
			this.values.removeFirst();
			if (value.state == ValueHolder.PRESENT) removed++;
		}
		this.presentCount.addAndGet(-removed);
	}

	public void invalidate(T_Key key) {
		ValueHolder<T_Value> holder;
		this.globalLock.lock();
		try {
			holder = this.values.get(key);
		}
		finally {
			this.globalLock.unlock();
		}
		if (holder != null) {
			holder.lock.lock();
			try {
				if (holder.state == ValueHolder.PRESENT) {
					this.presentCount.decrementAndGet();
				}
				holder.clear();
			}
			finally {
				holder.lock.unlock();
			}
		}
	}

	/**
	returns true if this key is guaranteed to be
	invalidated after this call returns, false otherwise.
	note that true is returned if this cache does not contain the specified key.
	false may be returned if we could not acquire a lock immediately.
	*/
	public boolean tryInvalidate(T_Key key) {
		ValueHolder<T_Value> holder;
		if (this.globalLock.tryLock()) try {
			holder = this.values.get(key);
		}
		finally {
			this.globalLock.unlock();
		}
		else {
			return false;
		}
		if (holder == null) {
			return true;
		}
		else if (holder.lock.tryLock()) try {
			if (holder.state == ValueHolder.PRESENT) {
				this.presentCount.decrementAndGet();
			}
			holder.clear();
			return true;
		}
		finally {
			holder.lock.unlock();
		}
		else {
			return false;
		}
	}

	public void remove(T_Key key) {
		this.globalLock.lock();
		try {
			ValueHolder<T_Value> removed = this.values.remove(key);
			if (removed != null && removed.state == ValueHolder.PRESENT) {
				this.presentCount.decrementAndGet();
			}
		}
		finally {
			this.globalLock.unlock();
		}
	}

	public void clear() {
		this.globalLock.lock();
		try {
			this.values.clear();
			this.presentCount.set(0);
		}
		finally {
			this.globalLock.unlock();
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " holding " + this.values.size() + " values for " + this.retainMillisecondsPerByte + " milliseconds per byte of free memory";
	}

	public static class ValueHolder<V> {

		public static final byte
			ABSENT  = 0,
			PRESENT = 1,
			UNKNOWN = 2;

		public final ReentrantLock lock = new ReentrantLock();
		public long timestamp = System.currentTimeMillis();
		public V value;
		public byte state = UNKNOWN;

		public @Nullable V get() {
			return this.value;
		}

		public void set(@Nullable V value) {
			this.value = value;
			this.state = value != null ? PRESENT : ABSENT;
		}

		public void clear() {
			this.value = null;
			this.state = UNKNOWN;
		}
	}

	public static class BackingMap<T_Key, T_Value> extends Object2ObjectLinkedOpenHashMap<T_Key, T_Value> {

		public BackingMap() {}

		public BackingMap(int expected) {
			super(expected);
		}

		public T_Value firstValue() {
			if (this.size == 0) throw new NoSuchElementException();
			return this.value[this.first];
		}
	}

	public static class Units {

		public static double nanoseconds(double nanoseconds) {
			return nanoseconds / 1_000_000.0D;
		}

		public static double microseconds(double microseconds) {
			return microseconds / 1_000.0D;
		}

		public static double milliseconds(double milliseconds) {
			return milliseconds;
		}

		public static double seconds(double seconds) {
			return seconds * 1_000.0D;
		}

		public static double bytes(double bytes) {
			return bytes;
		}

		public static double kilobytes(double kilobytes) {
			return kilobytes * 1024.0D;
		}

		public static double megabytes(double megabytes) {
			return megabytes * (1024.0D * 1024.0D);
		}

		public static double gigabytes(double gigabytes) {
			return gigabytes * (1024.0D * 1024.0D * 1024.0D);
		}
	}
}