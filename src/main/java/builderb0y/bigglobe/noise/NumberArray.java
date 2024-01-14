package builderb0y.bigglobe.noise;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

import org.lwjgl.system.*;

/**
an abstraction above arrays of numbers.
supported number types include bytes, shorts, ints, longs, floats, and doubles.
supported number types do NOT include chars (even though they can be used in arithmetic)
or booleans (even though they are ints on the stack).

each NumberArray has a specific internal type, and attempts to get and set
elements in the array will automatically cast to and from that internal type.

NumberArray's come in 2 primary variants: heap number arrays, and direct number arrays.
heap number arrays are backed by an actual java array of the corresponding number type.
direct number arrays used to be backed by a pointer allocated by {@link MemoryUtil}, which lives off-heap.
however, I found out the hard way that {@link MemoryUtil} isn't available on dedicated servers.
so, direct number arrays are now backed by a byte[] too, just handled differently.
nevertheless, the name "direct" will stay, even though it is now a misnomer.

direct number arrays are fundamentally temporary objects.
they are intended to be allocated, used, and de-allocated in reverse order that they are allocated in.
direct number arrays also all use a shared, thread-local region of memory.
shared in this case means that all direct number arrays allocated
on the same thread will use the same region of memory.
the usage of this region is tracked automatically. see {@link Direct.Manager} for more info.

instances of NumberArray are NOT thread-safe.
attempting to use a heap NumberArray concurrently is prone
to race conditions just as much as a regular array would be,
and closing a direct NumberArray on a different thread than the
one it was allocated on can corrupt the internal state of the
manager it belongs to, possibly leading to exceptions,
including exceptions thrown from other locations than where the NumberArray is closed!
*/
//todo: test booleans, add toString() for all types.
@SuppressWarnings({ "ClassNameSameAsAncestorName", "unused", "ImplicitNumericConversion", "OverloadedMethodsWithSameNumberOfParameters", "SameParameterValue", "NumericCastThatLosesPrecision" })
public interface NumberArray extends AutoCloseable {

	public static final NumberArray
		EMPTY_BYTE    = allocateBytesHeap(0),
		EMPTY_SHORT   = allocateShortsHeap(0),
		EMPTY_INT     = allocateIntsHeap(0),
		EMPTY_LONG    = allocateLongsHeap(0),
		EMPTY_FLOAT   = allocateFloatsHeap(0),
		EMPTY_DOUbLE  = allocateDoublesHeap(0),
		EMPTY_BOOLEAN = allocateBooleansHeap(0);

	public static NumberArray allocateBytesHeap(int bytes) { return new Heap.OfByte(new byte[bytes]); }
	public static NumberArray allocateShortsHeap(int shorts) { return new Heap.OfShort(new short[shorts]); }
	public static NumberArray allocateIntsHeap(int ints) { return new Heap.OfInt(new int[ints]); }
	public static NumberArray allocateLongsHeap(int longs) { return new Heap.OfLong(new long[longs]); }
	public static NumberArray allocateFloatsHeap(int floats) { return new Heap.OfFloat(new float[floats]); }
	public static NumberArray allocateDoublesHeap(int doubles) { return new Heap.OfDouble(new double[doubles]); }
	public static NumberArray allocateBooleansHeap(int booleans) { return new Heap.OfBoolean(new byte[(booleans + 7) >> 3], booleans); }

	public static NumberArray allocateBytesDirect(int bytes) { return Direct.Manager.INSTANCES.get().allocateBytes(bytes); }
	public static NumberArray allocateShortsDirect(int shorts) { return Direct.Manager.INSTANCES.get().allocateShorts(shorts); }
	public static NumberArray allocateIntsDirect(int ints) { return Direct.Manager.INSTANCES.get().allocateInts(ints); }
	public static NumberArray allocateLongsDirect(int longs) { return Direct.Manager.INSTANCES.get().allocateLongs(longs); }
	public static NumberArray allocateFloatsDirect(int floats) { return Direct.Manager.INSTANCES.get().allocateFloats(floats); }
	public static NumberArray allocateDoublesDirect(int doubles) { return Direct.Manager.INSTANCES.get().allocateDoubles(doubles); }
	public static NumberArray allocateBooleansDirect(int booleans) { return Direct.Manager.INSTANCES.get().allocateBooleans(booleans); }

	public abstract Precision getPrecision();

	@Override
	public abstract void close();

	public abstract byte    getB(int index);
	public abstract short   getS(int index);
	public abstract int     getI(int index);
	public abstract long    getL(int index);
	public abstract float   getF(int index);
	public abstract double  getD(int index);
	public abstract boolean getZ(int index);

	public abstract void setB(int index, byte    value);
	public abstract void setS(int index, short   value);
	public abstract void setI(int index, int     value);
	public abstract void setL(int index, long    value);
	public abstract void setF(int index, float   value);
	public abstract void setD(int index, double  value);
	public abstract void setZ(int index, boolean value);

	public default void fill(byte    value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(short   value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(int     value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(long    value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(float   value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(double  value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(boolean value) { this.fillFromTo(0, this.length(), value); }

	public abstract void fillFromTo(int from, int to, byte value);
	public abstract void fillFromTo(int from, int to, short value);
	public abstract void fillFromTo(int from, int to, int value);
	public abstract void fillFromTo(int from, int to, long value);
	public abstract void fillFromTo(int from, int to, float value);
	public abstract void fillFromTo(int from, int to, double value);
	public abstract void fillFromTo(int from, int to, boolean value);

	public default void add(int index, byte   value) { this.setB(index, (byte)(this.getB(index) + value)); }
	public default void add(int index, short  value) { this.setS(index, (short)(this.getB(index) + value)); }
	public default void add(int index, int    value) { this.setI(index, this.getI(index) + value); }
	public default void add(int index, long   value) { this.setL(index, this.getL(index) + value); }
	public default void add(int index, float  value) { this.setF(index, this.getF(index) + value); }
	public default void add(int index, double value) { this.setD(index, this.getD(index) + value); }

	public default void sub(int index, byte   value) { this.setB(index, (byte)(this.getB(index) - value)); }
	public default void sub(int index, short  value) { this.setS(index, (short)(this.getB(index) - value)); }
	public default void sub(int index, int    value) { this.setI(index, this.getI(index) - value); }
	public default void sub(int index, long   value) { this.setL(index, this.getL(index) - value); }
	public default void sub(int index, float  value) { this.setF(index, this.getF(index) - value); }
	public default void sub(int index, double value) { this.setD(index, this.getD(index) - value); }

	public default void mul(int index, byte   value) { this.setB(index, (byte)(this.getB(index) * value)); }
	public default void mul(int index, short  value) { this.setS(index, (short)(this.getB(index) * value)); }
	public default void mul(int index, int    value) { this.setI(index, this.getI(index) * value); }
	public default void mul(int index, long   value) { this.setL(index, this.getL(index) * value); }
	public default void mul(int index, float  value) { this.setF(index, this.getF(index) * value); }
	public default void mul(int index, double value) { this.setD(index, this.getD(index) * value); }

	public default void div(int index, byte   value) { this.setB(index, (byte)(this.getB(index) / value)); }
	public default void div(int index, short  value) { this.setS(index, (short)(this.getB(index) / value)); }
	public default void div(int index, int    value) { this.setI(index, this.getI(index) / value); }
	public default void div(int index, long   value) { this.setL(index, this.getL(index) / value); }
	public default void div(int index, float  value) { this.setF(index, this.getF(index) / value); }
	public default void div(int index, double value) { this.setD(index, this.getD(index) / value); }

	public default void min(int index, byte   value) { this.setB(index, (byte)(Math.min(this.getB(index), value))); }
	public default void min(int index, short  value) { this.setS(index, (short)(Math.min(this.getS(index), value))); }
	public default void min(int index, int    value) { this.setI(index, Math.min(this.getI(index), value)); }
	public default void min(int index, long   value) { this.setL(index, Math.min(this.getL(index), value)); }
	public default void min(int index, float  value) { this.setF(index, Math.min(this.getF(index), value)); }
	public default void min(int index, double value) { this.setD(index, Math.min(this.getD(index), value)); }

	public default void max(int index, byte   value) { this.setB(index, (byte)(Math.max(this.getB(index), value))); }
	public default void max(int index, short  value) { this.setS(index, (short)(Math.max(this.getS(index), value))); }
	public default void max(int index, int    value) { this.setI(index, Math.max(this.getI(index), value)); }
	public default void max(int index, long   value) { this.setL(index, Math.max(this.getL(index), value)); }
	public default void max(int index, float  value) { this.setF(index, Math.max(this.getF(index), value)); }
	public default void max(int index, double value) { this.setD(index, Math.max(this.getD(index), value)); }

	public default void and(int index, boolean value) { this.setZ(index, this.getZ(index) & value); }
	public default void or (int index, boolean value) { this.setZ(index, this.getZ(index) | value); }

	public abstract int length();

	public default NumberArray prefix(int length) {
		return this.sliceOffsetLength(0, length);
	}

	public default NumberArray sliceFromTo(int from, int to) {
		return this.sliceOffsetLength(from, to - from);
	}

	public default NumberArray sliceOffsetLength(int offset, int length) {
		Objects.checkFromIndexSize(offset, length, this.length());
		if (offset == 0) {
			if (length == this.length()) return this;
			else return new Prefix(this, length);
		}
		else {
			return new Slice(this, offset, length);
		}
	}

	public static interface OfByte extends NumberArray {

		@Override default Precision getPrecision() { return Precision.BYTE; }

		@Override public default short getS(int index) { return this.getB(index); }
		@Override public default int getI(int index) { return this.getB(index); }
		@Override public default long getL(int index) { return this.getB(index); }
		@Override public default float getF(int index) { return this.getB(index); }
		@Override public default double getD(int index) { return this.getB(index); }
		@Override public default boolean getZ(int index) { return this.getB(index) != 0; }

		@Override public default void setS(int index, short value) { this.setB(index, (byte)(value)); }
		@Override public default void setI(int index, int value) { this.setB(index, (byte)(value)); }
		@Override public default void setL(int index, long value) { this.setB(index, (byte)(value)); }
		@Override public default void setF(int index, float value) { this.setB(index, (byte)(value)); }
		@Override public default void setD(int index, double value) { this.setB(index, (byte)(value)); }
		@Override public default void setZ(int index, boolean value) { this.setB(index, value ? ((byte)(1)) : ((byte)(0))); }

		@Override public default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (byte)(value)); }
		@Override public default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (byte)(value)); }
		@Override public default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (byte)(value)); }
		@Override public default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (byte)(value)); }
		@Override public default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (byte)(value)); }
		@Override public default void fillFromTo(int from, int to, boolean value) { this.fillFromTo(from, to, value ? ((byte)(1)) : ((byte)(0))); }
	}

	public static interface OfShort extends NumberArray {

		@Override default Precision getPrecision() { return Precision.SHORT; }

		@Override public default byte getB(int index) { return (byte)(this.getS(index)); }
		@Override public default int getI(int index) { return this.getS(index); }
		@Override public default long getL(int index) { return this.getS(index); }
		@Override public default float getF(int index) { return this.getS(index); }
		@Override public default double getD(int index) { return this.getS(index); }
		@Override public default boolean getZ(int index) { return this.getS(index) != 0; }

		@Override public default void setB(int index, byte value) { this.setS(index, value); }
		@Override public default void setI(int index, int value) { this.setS(index, (short)(value)); }
		@Override public default void setL(int index, long value) { this.setS(index, (short)(value)); }
		@Override public default void setF(int index, float value) { this.setS(index, (short)(value)); }
		@Override public default void setD(int index, double value) { this.setS(index, (short)(value)); }
		@Override public default void setZ(int index, boolean value) { this.setS(index, value ? ((short)(1)) : ((short)(0))); }

		@Override public default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (short)(value)); }
		@Override public default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (short)(value)); }
		@Override public default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (short)(value)); }
		@Override public default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (short)(value)); }
		@Override public default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (short)(value)); }
		@Override public default void fillFromTo(int from, int to, boolean value) { this.fillFromTo(from, to, value ? ((short)(1)) : ((short)(0))); }
	}

	public static interface OfInt extends NumberArray {

		@Override default Precision getPrecision() { return Precision.INT; }

		@Override public default byte getB(int index) { return (byte)(this.getI(index)); }
		@Override public default short getS(int index) { return (short)(this.getI(index)); }
		@Override public default long getL(int index) { return this.getI(index); }
		@Override public default float getF(int index) { return this.getI(index); }
		@Override public default double getD(int index) { return this.getI(index); }
		@Override public default boolean getZ(int index) { return this.getI(index) != 0; }

		@Override public default void setB(int index, byte value) { this.setI(index, value); }
		@Override public default void setS(int index, short value) { this.setI(index, value); }
		@Override public default void setL(int index, long value) { this.setI(index, (int)(value)); }
		@Override public default void setF(int index, float value) { this.setI(index, (int)(value)); }
		@Override public default void setD(int index, double value) { this.setI(index, (int)(value)); }
		@Override public default void setZ(int index, boolean value) { this.setI(index, value ? 1 : 0); }

		@Override public default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (int)(value)); }
		@Override public default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (int)(value)); }
		@Override public default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (int)(value)); }
		@Override public default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (int)(value)); }
		@Override public default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (int)(value)); }
		@Override public default void fillFromTo(int from, int to, boolean value) { this.fillFromTo(from, to, value ? 1 : 0); }
	}

	public static interface OfLong extends NumberArray {

		@Override default Precision getPrecision() { return Precision.LONG; }

		@Override public default byte getB(int index) { return (byte)(this.getL(index)); }
		@Override public default short getS(int index) { return (short)(this.getL(index)); }
		@Override public default int getI(int index) { return (int)(this.getL(index)); }
		@Override public default float getF(int index) { return this.getL(index); }
		@Override public default double getD(int index) { return this.getL(index); }
		@Override public default boolean getZ(int index) { return this.getL(index) != 0L; }

		@Override public default void setB(int index, byte value) { this.setL(index, value); }
		@Override public default void setS(int index, short value) { this.setL(index, value); }
		@Override public default void setI(int index, int value) { this.setL(index, value); }
		@Override public default void setF(int index, float value) { this.setL(index, (int)(value)); }
		@Override public default void setD(int index, double value) { this.setL(index, (int)(value)); }
		@Override public default void setZ(int index, boolean value) { this.setL(index, value ? 1L : 0L); }

		@Override public default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (long)(value)); }
		@Override public default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (long)(value)); }
		@Override public default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (long)(value)); }
		@Override public default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (long)(value)); }
		@Override public default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (long)(value)); }
		@Override public default void fillFromTo(int from, int to, boolean value) { this.fillFromTo(from, to, value ? 1L : 0L); }
	}

	public static interface OfFloat extends NumberArray {

		@Override default Precision getPrecision() { return Precision.FLOAT; }

		@Override public default byte getB(int index) { return (byte)(this.getF(index)); }
		@Override public default short getS(int index) { return (short)(this.getF(index)); }
		@Override public default int getI(int index) { return (int)(this.getF(index)); }
		@Override public default long getL(int index) { return (long)(this.getF(index)); }
		@Override public default double getD(int index) { return this.getF(index); }
		@Override public default boolean getZ(int index) { return toZ(this.getF(index)); }

		@Override public default void setB(int index, byte value) { this.setF(index, value); }
		@Override public default void setS(int index, short value) { this.setF(index, value); }
		@Override public default void setI(int index, int value) { this.setF(index, value); }
		@Override public default void setL(int index, long value) { this.setF(index, value); }
		@Override public default void setD(int index, double value) { this.setF(index, (float)(value)); }
		@Override public default void setZ(int index, boolean value) { this.setF(index, value ? 1.0F : 0.0F); }

		@Override public default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (float)(value)); }
		@Override public default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (float)(value)); }
		@Override public default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (float)(value)); }
		@Override public default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (float)(value)); }
		@Override public default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (float)(value)); }
		@Override public default void fillFromTo(int from, int to, boolean value) { this.fillFromTo(from, to, value ? 1.0F : 0.0F); }

		public static boolean toZ(float value) {
			return value != 0.0F && value == value;
		}
	}

	public static interface OfDouble extends NumberArray {

		@Override default Precision getPrecision() { return Precision.DOUBLE; }

		@Override public default byte getB(int index) { return (byte)(this.getD(index)); }
		@Override public default short getS(int index) { return (short)(this.getD(index)); }
		@Override public default int getI(int index) { return (int)(this.getD(index)); }
		@Override public default long getL(int index) { return (long)(this.getD(index)); }
		@Override public default float getF(int index) { return (float)(this.getD(index)); }
		@Override public default boolean getZ(int index) { return toZ(this.getD(index)); }

		@Override public default void setB(int index, byte value) { this.setD(index, value); }
		@Override public default void setS(int index, short value) { this.setD(index, value); }
		@Override public default void setI(int index, int value) { this.setD(index, value); }
		@Override public default void setL(int index, long value) { this.setD(index, value); }
		@Override public default void setF(int index, float value) { this.setD(index, value); }
		@Override public default void setZ(int index, boolean value) { this.setD(index, value ? 1.0D : 0.0D); }

		@Override public default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (double)(value)); }
		@Override public default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (double)(value)); }
		@Override public default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (double)(value)); }
		@Override public default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (double)(value)); }
		@Override public default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (double)(value)); }
		@Override public default void fillFromTo(int from, int to, boolean value) { this.fillFromTo(from, to, value ? 1.0D : 0.0D); }

		public static boolean toZ(double value) {
			return value != 0.0D && value == value;
		}
	}

	public static interface OfBoolean extends NumberArray {

		@Override public default Precision getPrecision() { return Precision.BOOLEAN; }

		@Override public default byte getB(int index) { return this.getZ(index) ? ((byte)(1)) : ((byte)(0)); }
		@Override public default short getS(int index) { return this.getZ(index) ? ((short)(1)) : ((short)(0)); }
		@Override public default int getI(int index) { return this.getZ(index) ? 1 : 0; }
		@Override public default long getL(int index) { return this.getZ(index) ? 1L : 0L; }
		@Override public default float getF(int index) { return this.getZ(index) ? 1.0F : 0.0F; }
		@Override public default double getD(int index) { return this.getZ(index) ? 1.0D : 0.0D; }

		@Override public default void setB(int index, byte value) { this.setZ(index, value != 0); }
		@Override public default void setS(int index, short value) { this.setZ(index, value != 0); }
		@Override public default void setI(int index, int value) { this.setZ(index, value != 0); }
		@Override public default void setL(int index, long value) { this.setZ(index, value != 0L); }
		@Override public default void setF(int index, float value) { this.setZ(index, OfFloat.toZ(value)); }
		@Override public default void setD(int index, double value) { this.setZ(index, OfDouble.toZ(value)); }

		@Override public default void fillFromTo(int from, int to, byte value) {this.fillFromTo(from, to, value != 0); }
		@Override public default void fillFromTo(int from, int to, short value) {this.fillFromTo(from, to, value != 0); }
		@Override public default void fillFromTo(int from, int to, int value) {this.fillFromTo(from, to, value != 0); }
		@Override public default void fillFromTo(int from, int to, long value) {this.fillFromTo(from, to, value != 0L); }
		@Override public default void fillFromTo(int from, int to, float value) {this.fillFromTo(from, to, OfFloat.toZ(value)); }
		@Override public default void fillFromTo(int from, int to, double value) {this.fillFromTo(from, to, OfDouble.toZ(value)); }
	}

	public static abstract class Heap implements NumberArray {

		@Override
		public void close() {}

		public static class OfByte extends Heap implements NumberArray.OfByte {

			public final byte[] array;

			public OfByte(byte[] array) {
				this.array = array;
			}

			@Override
			public byte getB(int index) {
				return this.array[index];
			}

			@Override
			public void setB(int index, byte value) {
				this.array[index] = value;
			}

			@Override
			public void fillFromTo(int from, int to, byte value) {
				Arrays.fill(this.array, from, to, value);
			}

			@Override
			public int length() {
				return this.array.length;
			}
		}

		public static class OfShort extends Heap implements NumberArray.OfShort {

			public final short[] array;

			public OfShort(short[] array) {
				this.array = array;
			}

			@Override
			public short getS(int index) {
				return this.array[index];
			}

			@Override
			public void setS(int index, short value) {
				this.array[index] = value;
			}

			@Override
			public void fillFromTo(int from, int to, short value) {
				Arrays.fill(this.array, from, to, value);
			}

			@Override
			public int length() {
				return this.array.length;
			}
		}

		public static class OfInt extends Heap implements NumberArray.OfInt {

			public final int[] array;

			public OfInt(int[] array) {
				this.array = array;
			}

			@Override
			public int getI(int index) {
				return this.array[index];
			}

			@Override
			public void setI(int index, int value) {
				this.array[index] = value;
			}

			@Override
			public void fillFromTo(int from, int to, int value) {
				Arrays.fill(this.array, from, to, value);
			}

			@Override
			public int length() {
				return this.array.length;
			}
		}

		public static class OfLong extends Heap implements NumberArray.OfLong {

			public final long[] array;

			public OfLong(long[] array) {
				this.array = array;
			}

			@Override
			public long getL(int index) {
				return this.array[index];
			}

			@Override
			public void setL(int index, long value) {
				this.array[index] = value;
			}

			@Override
			public void fillFromTo(int from, int to, long value) {
				Arrays.fill(this.array, from, to, value);
			}

			@Override
			public int length() {
				return this.array.length;
			}
		}

		public static class OfFloat extends Heap implements NumberArray.OfFloat {

			public final float[] array;

			public OfFloat(float[] array) {
				this.array = array;
			}

			@Override
			public float getF(int index) {
				return this.array[index];
			}

			@Override
			public void setF(int index, float value) {
				this.array[index] = value;
			}

			@Override
			public void fillFromTo(int from, int to, float value) {
				Arrays.fill(this.array, from, to, value);
			}

			@Override
			public int length() {
				return this.array.length;
			}
		}

		public static class OfDouble extends Heap implements NumberArray.OfDouble {

			public final double[] array;

			public OfDouble(double[] array) {
				this.array = array;
			}

			@Override
			public double getD(int index) {
				return this.array[index];
			}

			@Override
			public void setD(int index, double value) {
				this.array[index] = value;
			}

			@Override
			public void fillFromTo(int from, int to, double value) {
				Arrays.fill(this.array, from, to, value);
			}

			@Override
			public int length() {
				return this.array.length;
			}
		}

		public static class OfBoolean extends Heap implements NumberArray.OfBoolean {

			public final byte[] array;
			public final int bitLength;

			public OfBoolean(byte[] array, int length) {
				this.array = array;
				this.bitLength = length;
			}

			@Override
			public boolean getZ(int index) {
				return ((this.array[Objects.checkIndex(index, this.bitLength) >>> 3] >>> (index & 7)) & 1) != 0;
			}

			@Override
			public void setZ(int index, boolean value) {
				if (value) {
					this.array[Objects.checkIndex(index, this.bitLength) >> 3] |= (byte)(1 << (index & 7));
				}
				else {
					this.array[Objects.checkIndex(index, this.bitLength) >> 3] &= (byte)(~(1 << (index & 7)));
				}
			}

			@Override
			public void fillFromTo(int from, int to, boolean value) {
				//similar logic as BitSet, but working with a byte[] instead of a long[].
				int firstByteIndex = Objects.checkFromToIndex(from, to, this.bitLength) >> 3;
				int lastByteIndex = (to - 1) >> 3;
				byte firstByteMask = (byte)(255 << from);
				byte lastByteMask  = (byte)(255 >>> -to);
				byte[] base = this.array;
				if (value) {
					if (firstByteMask == lastByteMask) {
						base[firstByteIndex] |= (byte)(firstByteMask & lastByteMask);
					}
					else {
						base[firstByteIndex] |= firstByteMask;
						for (int index = firstByteIndex; ++index < lastByteIndex;) {
							base[index] = (byte)(-1);
						}
						base[lastByteIndex] |= lastByteMask;
					}
				}
				else {
					if (firstByteMask == lastByteMask) {
						base[firstByteIndex] &= (byte)(~(firstByteMask & lastByteMask));
					}
					else {
						base[firstByteIndex] &= (byte)(~firstByteMask);
						for (int index = firstByteIndex; ++index < lastByteIndex;) {
							base[index] = (byte)(0);
						}
						base[lastByteIndex] &= (byte)(~lastByteMask);
					}
				}
			}

			@Override
			public int length() {
				return this.bitLength;
			}
		}
	}

	public static abstract class Direct implements NumberArray {

		public static final int
			BYTE_SHIFT   = 0,
			SHORT_SHIFT  = 1,
			INT_SHIFT    = 2,
			LONG_SHIFT   = 3,
			FLOAT_SHIFT  = 2,
			DOUBLE_SHIFT = 3;
		public static final VarHandle
			BYTE_ACCESS   = MethodHandles.arrayElementVarHandle (byte  [].class).withInvokeExactBehavior(),
			SHORT_ACCESS  = MethodHandles.byteArrayViewVarHandle(short [].class, ByteOrder.nativeOrder()).withInvokeExactBehavior(),
			INT_ACCESS    = MethodHandles.byteArrayViewVarHandle(int   [].class, ByteOrder.nativeOrder()).withInvokeExactBehavior(),
			LONG_ACCESS   = MethodHandles.byteArrayViewVarHandle(long  [].class, ByteOrder.nativeOrder()).withInvokeExactBehavior(),
			FLOAT_ACCESS  = MethodHandles.byteArrayViewVarHandle(float [].class, ByteOrder.nativeOrder()).withInvokeExactBehavior(),
			DOUBLE_ACCESS = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.nativeOrder()).withInvokeExactBehavior();

		public Manager manager;
		public int byteOffset, byteLength;
		public boolean freeable;

		public Direct(Manager manager, int byteLength, int alignment) {
			if ((byteLength & (alignment - 1)) != 0) {
				throw new IllegalStateException("Invalid alignment " + alignment + " for length " + byteLength);
			}
			if (manager.used + byteLength > manager.base.length) {
				throw new IllegalStateException("Manager has insufficient capacity for " + byteLength + " byte(s): " + manager);
			}
			this.manager    = manager;
			this.byteOffset = manager.used;
			this.byteLength = byteLength;
			this.freeable   = true;
			manager.used += byteLength;
		}

		public Direct(Manager manager, int byteOffset, int byteLength, int alignment) {
			if ((byteLength & (alignment - 1)) != 0) {
				throw new IllegalStateException("Invalid alignment " + alignment + " for length " + byteLength);
			}
			if (manager.used + byteLength > manager.base.length) {
				throw new IllegalStateException("Manager has insufficient capacity for " + byteLength + " byte(s): " + manager);
			}
			this.manager    = manager;
			this.byteOffset = byteOffset;
			this.byteLength = byteLength;
			this.freeable   = false;
		}

		@Override
		public void close() {
			if (this.freeable) {
				if (this.manager.used == this.byteOffset + this.byteLength) {
					this.manager.used = this.byteOffset;
				}
				else {
					throw new IllegalStateException("Attempt to close NumberArray in wrong order!");
				}
				this.byteOffset = 0;
				this.byteLength = 0;
				this.freeable = false;
			}
			this.manager = null;
		}

		public static class OfByte extends Direct implements NumberArray.OfByte {

			public OfByte(Manager manager, int length) {
				super(manager, length, Byte.BYTES);
			}

			public OfByte(Manager manager, int offset, int length) {
				super(manager, offset, length, Byte.BYTES);
			}

			@Override
			public byte getB(int index) {
				return (byte)(BYTE_ACCESS.get(this.manager.base, Objects.checkIndex(index << BYTE_SHIFT, this.byteLength) + this.byteOffset));
			}

			@Override
			public void setB(int index, byte value) {
				BYTE_ACCESS.set(this.manager.base, Objects.checkIndex(index << BYTE_SHIFT, this.byteLength) + this.byteOffset, value);
			}

			@Override
			public void fillFromTo(int from, int to, byte value) {
				int indexFrom = from << BYTE_SHIFT;
				int indexTo = to << BYTE_SHIFT;
				Objects.checkFromToIndex(indexFrom, indexTo, this.byteLength);
				byte[] base = this.manager.base;
				indexFrom += this.byteOffset;
				indexTo += this.byteOffset;
				for (int index = indexFrom; index < indexTo; index += Byte.BYTES) {
					BYTE_ACCESS.set(base, index, value);
				}
			}

			@Override
			public int length() {
				return this.byteLength >> BYTE_SHIFT;
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfByte(this.manager, (offset << BYTE_SHIFT) + this.byteOffset, length << BYTE_SHIFT);
			}
		}

		public static class OfShort extends Direct implements NumberArray.OfShort {

			public OfShort(Manager manager, int length) {
				super(manager, length, Short.BYTES);
			}

			public OfShort(Manager manager, int offset, int length) {
				super(manager, offset, length, Short.BYTES);
			}

			@Override
			public short getS(int index) {
				return (short)(SHORT_ACCESS.get(this.manager.base, Objects.checkIndex(index << SHORT_SHIFT, this.byteLength) + this.byteOffset));
			}

			@Override
			public void setS(int index, short value) {
				SHORT_ACCESS.set(this.manager.base, Objects.checkIndex(index << SHORT_SHIFT, this.byteLength) + this.byteOffset, value);
			}

			@Override
			public void fillFromTo(int from, int to, short value) {
				int indexFrom = from << SHORT_SHIFT;
				int indexTo = to << SHORT_SHIFT;
				Objects.checkFromToIndex(indexFrom, indexTo, this.byteLength);
				byte[] base = this.manager.base;
				indexFrom += this.byteOffset;
				indexTo += this.byteOffset;
				for (int index = indexFrom; index < indexTo; index += Short.BYTES) {
					SHORT_ACCESS.set(base, index, value);
				}
			}

			@Override
			public int length() {
				return this.byteLength >> SHORT_SHIFT;
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfShort(this.manager, (offset << SHORT_SHIFT) + this.byteOffset, length << SHORT_SHIFT);
			}
		}

		public static class OfInt extends Direct implements NumberArray.OfInt {

			public OfInt(Manager manager, int length) {
				super(manager, length, Integer.BYTES);
			}

			public OfInt(Manager manager, int offset, int length) {
				super(manager, offset, length, Integer.BYTES);
			}

			@Override
			public int getI(int index) {
				return (int)(INT_ACCESS.get(this.manager.base, Objects.checkIndex(index << INT_SHIFT, this.byteLength) + this.byteOffset));
			}

			@Override
			public void setI(int index, int value) {
				INT_ACCESS.set(this.manager.base, Objects.checkIndex(index << INT_SHIFT, this.byteLength) + this.byteOffset, value);
			}

			@Override
			public void fillFromTo(int from, int to, int value) {
				int indexFrom = from << INT_SHIFT;
				int indexTo = to << INT_SHIFT;
				Objects.checkFromToIndex(indexFrom, indexTo, this.byteLength);
				byte[] base = this.manager.base;
				indexFrom += this.byteOffset;
				indexTo += this.byteOffset;
				for (int index = indexFrom; index < indexTo; index += Integer.BYTES) {
					INT_ACCESS.set(base, index, value);
				}
			}

			@Override
			public int length() {
				return this.byteLength >> INT_SHIFT;
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfInt(this.manager, (offset << INT_SHIFT) + this.byteOffset, length << INT_SHIFT);
			}
		}

		public static class OfLong extends Direct implements NumberArray.OfLong {

			public OfLong(Manager manager, int length) {
				super(manager, length, Long.BYTES);
			}

			public OfLong(Manager manager, int offset, int length) {
				super(manager, offset, length, Long.BYTES);
			}

			@Override
			public long getL(int index) {
				return (long)(LONG_ACCESS.get(this.manager.base, Objects.checkIndex(index << LONG_SHIFT, this.byteLength) + this.byteOffset));
			}

			@Override
			public void setL(int index, long value) {
				LONG_ACCESS.set(this.manager.base, Objects.checkIndex(index << LONG_SHIFT, this.byteLength) + this.byteOffset, value);
			}

			@Override
			public void fillFromTo(int from, int to, long value) {
				int indexFrom = from << LONG_SHIFT;
				int indexTo = to << LONG_SHIFT;
				Objects.checkFromToIndex(indexFrom, indexTo, this.byteLength);
				byte[] base = this.manager.base;
				indexFrom += this.byteOffset;
				indexTo += this.byteOffset;
				for (int index = indexFrom; index < indexTo; index += Long.BYTES) {
					LONG_ACCESS.set(base, index, value);
				}
			}

			@Override
			public int length() {
				return this.byteLength >> LONG_SHIFT;
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfLong(this.manager, (offset << LONG_SHIFT) + this.byteOffset, length << LONG_SHIFT);
			}
		}

		public static class OfFloat extends Direct implements NumberArray.OfFloat {

			public OfFloat(Manager manager, int length) {
				super(manager, length, Float.BYTES);
			}

			public OfFloat(Manager manager, int offset, int length) {
				super(manager, offset, length, Float.BYTES);
			}

			@Override
			public float getF(int index) {
				return (float)(FLOAT_ACCESS.get(this.manager.base, Objects.checkIndex(index << FLOAT_SHIFT, this.byteLength) + this.byteOffset));
			}

			@Override
			public void setF(int index, float value) {
				FLOAT_ACCESS.set(this.manager.base, Objects.checkIndex(index << FLOAT_SHIFT, this.byteLength) + this.byteOffset, value);
			}

			@Override
			public void fillFromTo(int from, int to, float value) {
				int indexFrom = from << FLOAT_SHIFT;
				int indexTo = to << FLOAT_SHIFT;
				Objects.checkFromToIndex(indexFrom, indexTo, this.byteLength);
				byte[] base = this.manager.base;
				indexFrom += this.byteOffset;
				indexTo += this.byteOffset;
				for (int index = indexFrom; index < indexTo; index += Float.BYTES) {
					FLOAT_ACCESS.set(base, index, value);
				}
			}

			@Override
			public int length() {
				return this.byteLength >> FLOAT_SHIFT;
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfFloat(this.manager, (offset << FLOAT_SHIFT) + this.byteOffset, length << FLOAT_SHIFT);
			}
		}

		public static class OfDouble extends Direct implements NumberArray.OfDouble {

			public OfDouble(Manager manager, int length) {
				super(manager, length, Double.BYTES);
			}

			public OfDouble(Manager manager, int offset, int length) {
				super(manager, offset, length, Double.BYTES);
			}

			@Override
			public double getD(int index) {
				return (double)(DOUBLE_ACCESS.get(this.manager.base, Objects.checkIndex(index << DOUBLE_SHIFT, this.byteLength) + this.byteOffset));
			}

			@Override
			public void setD(int index, double value) {
				DOUBLE_ACCESS.set(this.manager.base, Objects.checkIndex(index << DOUBLE_SHIFT, this.byteLength) + this.byteOffset, value);
			}

			@Override
			public void fillFromTo(int from, int to, double value) {
				int indexFrom = from << DOUBLE_SHIFT;
				int indexTo = to << DOUBLE_SHIFT;
				Objects.checkFromToIndex(indexFrom, indexTo, this.byteLength);
				byte[] base = this.manager.base;
				indexFrom += this.byteOffset;
				indexTo += this.byteOffset;
				for (int index = indexFrom; index < indexTo; index += Double.BYTES) {
					DOUBLE_ACCESS.set(base, index, value);
				}
			}

			@Override
			public int length() {
				return this.byteLength >> DOUBLE_SHIFT;
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfDouble(this.manager, (offset << DOUBLE_SHIFT) + this.byteOffset, length << DOUBLE_SHIFT);
			}
		}

		public static class OfBoolean extends Direct implements NumberArray.OfBoolean {

			public final int bitLength;

			public OfBoolean(Manager manager, int byteLength, int bitLength) {
				super(manager, byteLength, Byte.BYTES);
				this.bitLength = bitLength;
			}

			public OfBoolean(Manager manager, int byteOffset, int byteLength, int bitLength) {
				super(manager, byteOffset, byteLength, Byte.BYTES);
				this.bitLength = bitLength;
			}

			@Override
			public boolean getZ(int index) {
				int byteIndex = (Objects.checkIndex(index, this.bitLength) >> 3) + this.byteOffset;
				return ((this.manager.base[byteIndex] >>> (index & 7)) & 1) != 0;
			}

			@Override
			public void setZ(int index, boolean value) {
				int byteIndex = (Objects.checkIndex(index, this.bitLength) >> 3) + this.byteOffset;
				if (value) {
					this.manager.base[byteIndex] |= (byte)(1 << (index & 7));
				}
				else {
					this.manager.base[byteIndex] &= (byte)(~(1 << index & 7));
				}
			}

			@Override
			public void fillFromTo(int from, int to, boolean value) {
				Objects.checkFromToIndex(from, to, this.bitLength);
				//similar logic as BitSet, but working with a byte[] instead of a long[].
				int firstByteIndex = (from >> 3) + this.byteOffset;
				int lastByteIndex  = ((to - 1) >> 3) + this.byteOffset;
				byte firstByteMask = (byte)(255 << from);
				byte lastByteMask  = (byte)(255 >>> -to);
				byte[] base = this.manager.base;
				if (value) {
					if (firstByteMask == lastByteMask) {
						base[firstByteIndex] |= (byte)(firstByteMask & lastByteMask);
					}
					else {
						base[firstByteIndex] |= firstByteMask;
						for (int index = firstByteIndex; ++index < lastByteIndex;) {
							base[index] = (byte)(-1);
						}
						base[lastByteIndex] |= lastByteMask;
					}
				}
				else {
					if (firstByteMask == lastByteMask) {
						base[firstByteIndex] &= (byte)(~(firstByteMask & lastByteMask));
					}
					else {
						base[firstByteIndex] &= (byte)(~firstByteMask);
						for (int index = firstByteIndex; ++index < lastByteIndex;) {
							base[index] = (byte)(0);
						}
						base[lastByteIndex] &= (byte)(~lastByteMask);
					}
				}
			}

			@Override
			public int length() {
				return this.bitLength;
			}
		}

		/**
		manages a region of memory backed by a byte[],
		keeping track of how much of it is used at any given time.

		every direct NumberArray has an associated Manager.
		when a direct NumberArray is allocated,
		the Manager is notified and the usage of its region of memory increases.
		when a direct NumberArray is {@link #close()}'d, the usage of the region of memory decreases.
		however, the algorithm for de-allocation is quite naive,
		and only works if direct NumberArray's are closed
		in the reverse order that they are opened in.

		if enough direct NumberArray's are allocated that they collectively
		use more memory than our {@link #base}'s length, then the backing array
		is copied as a bigger size. this is why each direct NumberArray
		has a reference to the manager, not a reference to its backing array.
		this makes it possible to safely re-allocate memory without affecting
		any direct NumberArrays which have already been allocated with this Manager.
		*/
		public static class Manager {

			public static final String
				MIN_PROP = "bigglobe.NumberArray.Direct.minSize",
				MAX_PROP = "bigglobe.NumberArray.Direct.maxSize";
			public static final int
				MIN_SIZE = Integer.getInteger(MIN_PROP, 1024 * Double.BYTES),
				MAX_SIZE = Integer.getInteger(MAX_PROP, 1048576);

			static {
				if (MIN_SIZE <= 0L) throw new IllegalStateException("-D" + MIN_PROP + " must be positive.");
				if (MAX_SIZE < MIN_SIZE) throw new IllegalStateException("-D" + MAX_PROP + " must be greater than or equal to -D" + MIN_PROP);
			}

			public static final ThreadLocal<Manager> INSTANCES = ThreadLocal.withInitial(Manager::new);

			/** the beginning of the region of memory this Manager keeps track of. */
			public byte[] base;
			/** the number of bytes used in our region of memory. */
			public int used;

			public Manager() {
				this.base = new byte[MIN_SIZE];
			}

			public void ensureCapacity(int capacity) {
				if (capacity > MAX_SIZE) {
					throw new OutOfMemoryError("Requested capacity " + capacity + " exceeds maximum allocation limit " + MAX_SIZE + " as defined by java argument -D" + MAX_PROP);
				}
				if (this.base.length < capacity) {
					capacity = Math.min(
						Math.max(capacity, this.base.length << 1),
						MAX_SIZE
					);
					this.base = Arrays.copyOf(this.base, capacity);
				}
			}

			public int beforeAllocate(int bytes) {
				if (bytes < 0) {
					throw new IllegalArgumentException("Attempt to allocate negative bytes: " + bytes);
				}
				this.ensureCapacity(this.used + bytes);
				return bytes;
			}

			public Direct.OfByte allocateBytes(int bytes) {
				return new Direct.OfByte(this, this.beforeAllocate(bytes << BYTE_SHIFT));
			}

			public Direct.OfShort allocateShorts(int shorts) {
				return new Direct.OfShort(this, this.beforeAllocate(shorts << SHORT_SHIFT));
			}

			public Direct.OfInt allocateInts(int ints) {
				return new Direct.OfInt(this, this.beforeAllocate(ints << INT_SHIFT));
			}

			public Direct.OfLong allocateLongs(int longs) {
				return new Direct.OfLong(this, this.beforeAllocate(longs << LONG_SHIFT));
			}

			public Direct.OfFloat allocateFloats(int floats) {
				return new Direct.OfFloat(this, this.beforeAllocate(floats << FLOAT_SHIFT));
			}

			public Direct.OfDouble allocateDoubles(int doubles) {
				return new Direct.OfDouble(this, this.beforeAllocate(doubles << DOUBLE_SHIFT));
			}

			public Direct.OfBoolean allocateBooleans(int booleans) {
				return new Direct.OfBoolean(this, this.beforeAllocate((booleans + 7) >> 3), booleans);
			}

			public char hex(int shift) {
				int number = (System.identityHashCode(this.base) >>> shift) & 0xF;
				return (char)(number + (number >= 10 ? 'A' - 10 : '0'));
			}

			public String formatPointer() {
				return (
					"0x"
					+ this.hex(28) + this.hex(24)
					+ this.hex(20) + this.hex(16)
					+ this.hex(12) + this.hex( 8)
					+ this.hex( 4) + this.hex( 0)
				);
			}

			@Override
			public String toString() {
				return this.getClass().getName() + ": { base: " + this.formatPointer() + ", used: " + this.used + ", capacity: " + this.base.length + " }";
			}
		}
	}

	public static class Prefix implements NumberArray {

		public final NumberArray delegate;
		public final int length;

		public Prefix(NumberArray delegate, int length) {
			this.delegate = delegate;
			this.length   = length;
		}

		@Override public Precision getPrecision() { return this.delegate.getPrecision(); }

		@Override public void close() {}

		@Override public byte getB(int index) { return this.delegate.getB(Objects.checkIndex(index, this.length)); }
		@Override public short getS(int index) { return this.delegate.getS(Objects.checkIndex(index, this.length)); }
		@Override public int getI(int index) { return this.delegate.getI(Objects.checkIndex(index, this.length)); }
		@Override public long getL(int index) { return this.delegate.getL(Objects.checkIndex(index, this.length)); }
		@Override public float getF(int index) { return this.delegate.getF(Objects.checkIndex(index, this.length)); }
		@Override public double getD(int index) { return this.delegate.getD(Objects.checkIndex(index, this.length)); }
		@Override public boolean getZ(int index) { return this.delegate.getZ(Objects.checkIndex(index, this.length)); }

		@Override public void setB(int index, byte value) { this.delegate.setB(Objects.checkIndex(index, this.length), value); }
		@Override public void setS(int index, short value) { this.delegate.setS(Objects.checkIndex(index, this.length), value); }
		@Override public void setI(int index, int value) { this.delegate.setI(Objects.checkIndex(index, this.length), value); }
		@Override public void setL(int index, long value) { this.delegate.setL(Objects.checkIndex(index, this.length), value); }
		@Override public void setF(int index, float value) { this.delegate.setF(Objects.checkIndex(index, this.length), value); }
		@Override public void setD(int index, double value) { this.delegate.setD(Objects.checkIndex(index, this.length), value); }
		@Override public void setZ(int index, boolean value) { this.delegate.setZ(Objects.checkIndex(index, this.length), value); }

		@Override public void fillFromTo(int from, int to, byte value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length), to, value); }
		@Override public void fillFromTo(int from, int to, short value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length), to, value); }
		@Override public void fillFromTo(int from, int to, int value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length), to, value); }
		@Override public void fillFromTo(int from, int to, long value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length), to, value); }
		@Override public void fillFromTo(int from, int to, float value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length), to, value); }
		@Override public void fillFromTo(int from, int to, double value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length), to, value); }
		@Override public void fillFromTo(int from, int to, boolean value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from, to, this.length), to, value); }

		@Override public int length() { return this.length; }

		@Override
		public NumberArray sliceOffsetLength(int offset, int length) {
			Objects.checkFromIndexSize(offset, length, this.length);
			return this.delegate.sliceOffsetLength(offset, length);
		}
	}

	public static class Slice implements NumberArray {

		public final NumberArray delegate;
		public final int offset, length;

		public Slice(NumberArray delegate, int offset, int length) {
			this.delegate = delegate;
			this.offset   = offset;
			this.length   = length;
		}

		@Override public Precision getPrecision() { return this.delegate.getPrecision(); }

		@Override public void close() {}

		@Override public byte getB(int index) { return this.delegate.getB(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public short getS(int index) { return this.delegate.getS(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public int getI(int index) { return this.delegate.getI(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public long getL(int index) { return this.delegate.getL(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public float getF(int index) { return this.delegate.getF(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public double getD(int index) { return this.delegate.getD(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public boolean getZ(int index) { return this.delegate.getZ(Objects.checkIndex(index, this.length) + this.offset); }

		@Override public void setB(int index, byte value) { this.delegate.setB(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setS(int index, short value) { this.delegate.setS(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setI(int index, int value) { this.delegate.setI(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setL(int index, long value) { this.delegate.setL(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setF(int index, float value) { this.delegate.setF(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setD(int index, double value) { this.delegate.setD(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setZ(int index, boolean value) { this.delegate.setZ(Objects.checkIndex(index, this.length) + this.offset, value); }

		@Override public void fillFromTo(int from, int to, byte value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length) + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, short value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length) + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, int value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length) + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, long value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length) + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, float value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length) + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, double value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from,to, this.length) + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, boolean value) { this.delegate.fillFromTo(Objects.checkFromToIndex(from, to, this.length) + this.offset, to + this.offset, value); }

		@Override public int length() { return this.length; }

		@Override
		public NumberArray sliceOffsetLength(int offset, int length) {
			Objects.checkFromIndexSize(offset, length, this.length);
			return this.delegate.sliceOffsetLength(offset + this.offset, length);
		}
	}

	public static enum Precision {
		BYTE,
		SHORT,
		INT,
		LONG,
		FLOAT,
		DOUBLE,
		BOOLEAN;
	}
}