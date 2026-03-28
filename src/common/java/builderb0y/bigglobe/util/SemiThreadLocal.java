package builderb0y.bigglobe.util;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.ObjectList;

public abstract class SemiThreadLocal<T> {

	public int valueCount;

	public abstract T createValue();

	public abstract T[] values();

	public abstract int maxThreads();

	public T get() {
		synchronized (this) {
			T[] values = this.values();
			if (this.valueCount != 0) {
				return values[--this.valueCount];
			}
		}
		return this.createValue();
	}

	public synchronized void reclaim(T value) {
		T[] values = this.values();
		if (this.valueCount < values.length) {
			values[this.valueCount++] = value;
		}
	}

	public synchronized void reclaimAll(ObjectList<T> values) {
		T[] existingValues = this.values();
		if (this.valueCount < existingValues.length) {
			int moved = Math.min(existingValues.length - this.valueCount, values.size());
			values.getElements(0, existingValues, this.valueCount, moved);
			this.valueCount += moved;
		}
	}

	public static <T> SemiThreadLocal<T> strong(int maxThreads, Supplier<T> supplier) {
		return new StrongSemiThreadLocal<>(maxThreads) {

			@Override
			public T createValue() {
				return supplier.get();
			}
		};
	}

	public static <T> SemiThreadLocal<T> soft(int maxThreads, Supplier<T> supplier) {
		return new SoftSemiThreadLocal<>(maxThreads) {

			@Override
			public T createValue() {
				return supplier.get();
			}
		};
	}

	public static <T> SemiThreadLocal<T> weak(int maxThreads, Supplier<T> supplier) {
		return new WeakSemiThreadLocal<>(maxThreads) {

			@Override
			public T createValue() {
				return supplier.get();
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static <T_Raw, T_Generic extends T_Raw> T_Generic[] castArray(T_Raw[] array) {
		return (T_Generic[])(array);
	}

	public static abstract class StrongSemiThreadLocal<T> extends SemiThreadLocal<T> {

		public final T[] values;

		public StrongSemiThreadLocal(int maxThreads) {
			this.values = castArray(new Object[maxThreads]);
		}

		@Override
		public T[] values() {
			return this.values;
		}

		@Override
		public int maxThreads() {
			return this.values.length;
		}
	}

	public static abstract class SoftSemiThreadLocal<T> extends SemiThreadLocal<T> {

		public final int maxThreads;
		public SoftReference<T[]> reference;

		public SoftSemiThreadLocal(int maxThreads) {
			this.maxThreads = maxThreads;
		}

		@Override
		public T[] values() {
			SoftReference<T[]> reference = this.reference;
			T[] array;
			if (reference == null || (array = reference.get()) == null) {
				array = castArray(new Object[this.maxThreads]);
				this.reference = new SoftReference<>(array);
				this.valueCount = 0;
			}
			return array;
		}

		@Override
		public int maxThreads() {
			return this.maxThreads;
		}
	}

	public static abstract class WeakSemiThreadLocal<T> extends SemiThreadLocal<T> {

		public final int maxThreads;
		public WeakReference<T[]> reference;

		public WeakSemiThreadLocal(int maxThreads) {
			this.maxThreads = maxThreads;
		}

		@Override
		public T[] values() {
			WeakReference<T[]> reference = this.reference;
			T[] array;
			if (reference == null || (array = reference.get()) == null) {
				array = castArray(new Object[this.maxThreads]);
				this.reference = new WeakReference<>(array);
				this.valueCount = 0;
			}
			return array;
		}

		@Override
		public int maxThreads() {
			return this.maxThreads;
		}
	}
}