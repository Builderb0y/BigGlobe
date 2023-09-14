package builderb0y.bigglobe.noise;

import java.util.Arrays;
import java.util.Objects;

import org.lwjgl.system.*;

public interface NumberArray extends AutoCloseable {

	public static NumberArray allocateBytesHeap(int bytes) { return new Heap.OfByte(new byte[bytes]); }
	public static NumberArray allocateShortsHeap(int shorts) { return new Heap.OfShort(new short[shorts]); }
	public static NumberArray allocateIntsHeap(int ints) { return new Heap.OfInt(new int[ints]); }
	public static NumberArray allocateLongsHeap(int longs) { return new Heap.OfLong(new long[longs]); }
	public static NumberArray allocateFloatsHeap(int floats) { return new Heap.OfFloat(new float[floats]); }
	public static NumberArray allocateDoublesHeap(int doubles) { return new Heap.OfDouble(new double[doubles]); }

	public static NumberArray allocateBytesDirect(int bytes) { return Direct.Manager.INSTANCES.get().allocateBytes(bytes); }
	public static NumberArray allocateShortsDirect(int shorts) { return Direct.Manager.INSTANCES.get().allocateShorts(shorts); }
	public static NumberArray allocateIntsDirect(int ints) { return Direct.Manager.INSTANCES.get().allocateInts(ints); }
	public static NumberArray allocateLongsDirect(int longs) { return Direct.Manager.INSTANCES.get().allocateLongs(longs); }
	public static NumberArray allocateFloatsDirect(int floats) { return Direct.Manager.INSTANCES.get().allocateFloats(floats); }
	public static NumberArray allocateDoublesDirect(int doubles) { return Direct.Manager.INSTANCES.get().allocateDoubles(doubles); }

	@Override
	public abstract void close();

	public abstract byte   getB(int index);
	public abstract short  getS(int index);
	public abstract int    getI(int index);
	public abstract long   getL(int index);
	public abstract float  getF(int index);
	public abstract double getD(int index);

	public abstract void setB(int index, byte   value);
	public abstract void setS(int index, short  value);
	public abstract void setI(int index, int    value);
	public abstract void setL(int index, long   value);
	public abstract void setF(int index, float  value);
	public abstract void setD(int index, double value);

	public default void fill(byte value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(short value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(int value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(long value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(float value) { this.fillFromTo(0, this.length(), value); }
	public default void fill(double value) { this.fillFromTo(0, this.length(), value); }

	public abstract void fillFromTo(int from, int to, byte value);
	public abstract void fillFromTo(int from, int to, short value);
	public abstract void fillFromTo(int from, int to, int value);
	public abstract void fillFromTo(int from, int to, long value);
	public abstract void fillFromTo(int from, int to, float value);
	public abstract void fillFromTo(int from, int to, double value);

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

		@Override public default short getS(int index) { return this.getB(index); }
		@Override public default int getI(int index) { return this.getB(index); }
		@Override public default long getL(int index) { return this.getB(index); }
		@Override public default float getF(int index) { return this.getB(index); }
		@Override public default double getD(int index) { return this.getB(index); }

		@Override public default void setS(int index, short value) { this.setB(index, (byte)(value)); }
		@Override public default void setI(int index, int value) { this.setB(index, (byte)(value)); }
		@Override public default void setL(int index, long value) { this.setB(index, (byte)(value)); }
		@Override public default void setF(int index, float value) { this.setB(index, (byte)(value)); }
		@Override public default void setD(int index, double value) { this.setB(index, (byte)(value)); }

		@Override default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (byte)(value)); }
		@Override default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (byte)(value)); }
		@Override default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (byte)(value)); }
		@Override default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (byte)(value)); }
		@Override default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (byte)(value)); }
	}

	public static interface OfShort extends NumberArray {

		@Override public default byte getB(int index) { return (byte)(this.getS(index)); }
		@Override public default int getI(int index) { return this.getS(index); }
		@Override public default long getL(int index) { return this.getS(index); }
		@Override public default float getF(int index) { return this.getS(index); }
		@Override public default double getD(int index) { return this.getS(index); }

		@Override public default void setB(int index, byte value) { this.setS(index, value); }
		@Override public default void setI(int index, int value) { this.setS(index, (short)(value)); }
		@Override public default void setL(int index, long value) { this.setS(index, (short)(value)); }
		@Override public default void setF(int index, float value) { this.setS(index, (short)(value)); }
		@Override public default void setD(int index, double value) { this.setS(index, (short)(value)); }

		@Override default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (short)(value)); }
		@Override default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (short)(value)); }
		@Override default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (short)(value)); }
		@Override default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (short)(value)); }
		@Override default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (short)(value)); }
	}

	public static interface OfInt extends NumberArray {

		@Override public default byte getB(int index) { return (byte)(this.getI(index)); }
		@Override public default short getS(int index) { return (short)(this.getI(index)); }
		@Override public default long getL(int index) { return this.getI(index); }
		@Override public default float getF(int index) { return this.getI(index); }
		@Override public default double getD(int index) { return this.getI(index); }

		@Override public default void setB(int index, byte value) { this.setI(index, value); }
		@Override public default void setS(int index, short value) { this.setI(index, value); }
		@Override public default void setL(int index, long value) { this.setI(index, (int)(value)); }
		@Override public default void setF(int index, float value) { this.setI(index, (int)(value)); }
		@Override public default void setD(int index, double value) { this.setI(index, (int)(value)); }

		@Override default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (int)(value)); }
		@Override default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (int)(value)); }
		@Override default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (int)(value)); }
		@Override default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (int)(value)); }
		@Override default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (int)(value)); }
	}

	public static interface OfLong extends NumberArray {

		@Override public default byte getB(int index) { return (byte)(this.getL(index)); }
		@Override public default short getS(int index) { return (short)(this.getL(index)); }
		@Override public default int getI(int index) { return (int)(this.getL(index)); }
		@Override public default float getF(int index) { return this.getL(index); }
		@Override public default double getD(int index) { return this.getL(index); }

		@Override public default void setB(int index, byte value) { this.setL(index, value); }
		@Override public default void setS(int index, short value) { this.setL(index, value); }
		@Override public default void setI(int index, int value) { this.setL(index, value); }
		@Override public default void setF(int index, float value) { this.setL(index, (int)(value)); }
		@Override public default void setD(int index, double value) { this.setL(index, (int)(value)); }

		@Override default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (long)(value)); }
		@Override default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (long)(value)); }
		@Override default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (long)(value)); }
		@Override default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (long)(value)); }
		@Override default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (long)(value)); }
	}

	public static interface OfFloat extends NumberArray {

		@Override public default byte getB(int index) { return (byte)(this.getF(index)); }
		@Override public default short getS(int index) { return (short)(this.getF(index)); }
		@Override public default int getI(int index) { return (int)(this.getF(index)); }
		@Override public default long getL(int index) { return (long)(this.getF(index)); }
		@Override public default double getD(int index) { return this.getF(index); }

		@Override public default void setB(int index, byte value) { this.setF(index, value); }
		@Override public default void setS(int index, short value) { this.setF(index, value); }
		@Override public default void setI(int index, int value) { this.setF(index, value); }
		@Override public default void setL(int index, long value) { this.setF(index, value); }
		@Override public default void setD(int index, double value) { this.setF(index, (float)(value)); }

		@Override default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (float)(value)); }
		@Override default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (float)(value)); }
		@Override default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (float)(value)); }
		@Override default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (float)(value)); }
		@Override default void fillFromTo(int from, int to, double value) { this.fillFromTo(from, to, (float)(value)); }
	}

	public static interface OfDouble extends NumberArray {

		@Override public default byte getB(int index) { return (byte)(this.getD(index)); }
		@Override public default short getS(int index) { return (short)(this.getD(index)); }
		@Override public default int getI(int index) { return (int)(this.getD(index)); }
		@Override public default long getL(int index) { return (long)(this.getD(index)); }
		@Override public default float getF(int index) { return (float)(this.getD(index)); }

		@Override public default void setB(int index, byte value) { this.setD(index, value); }
		@Override public default void setS(int index, short value) { this.setD(index, value); }
		@Override public default void setI(int index, int value) { this.setD(index, value); }
		@Override public default void setL(int index, long value) { this.setD(index, value); }
		@Override public default void setF(int index, float value) { this.setD(index, value); }

		@Override default void fillFromTo(int from, int to, byte value) { this.fillFromTo(from, to, (double)(value)); }
		@Override default void fillFromTo(int from, int to, short value) { this.fillFromTo(from, to, (double)(value)); }
		@Override default void fillFromTo(int from, int to, int value) { this.fillFromTo(from, to, (double)(value)); }
		@Override default void fillFromTo(int from, int to, long value) { this.fillFromTo(from, to, (double)(value)); }
		@Override default void fillFromTo(int from, int to, float value) { this.fillFromTo(from, to, (double)(value)); }
	}

	public static abstract class Heap implements NumberArray {

		@Override
		public void close() {}

		public static class OfByte extends Heap implements NumberArray.OfByte {

			public final byte[] array;

			public OfByte(byte[] array) {
				this.array  = array;
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
				this.array  = array;
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
				this.array  = array;
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
				this.array  = array;
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
				this.array  = array;
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
				this.array  = array;
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
	}

	public static abstract class Direct implements NumberArray {

		public static final int
			BYTE_SHIFT = 0,
			SHORT_SHIFT = 1,
			INT_SHIFT = 2,
			LONG_SHIFT = 3,
			FLOAT_SHIFT = 2,
			DOUBLE_SHIFT = 3;

		public final Manager manager;
		public final long byteOffset, byteLength;
		public final boolean freeable;

		public Direct(Manager manager, long byteLength, int alignment) {
			if ((byteLength & (alignment - 1)) != 0) {
				throw new IllegalStateException("Invalid alignment " + alignment + " for length " + byteLength);
			}
			if (manager.used + byteLength > manager.capacity) {
				throw new IllegalStateException("Manager has insufficient capacity for " + byteLength + " byte(s): " + manager);
			}
			this.manager  = manager;
			this.byteOffset = manager.used;
			this.byteLength = byteLength;
			this.freeable = true;
		}

		public Direct(Manager manager, long byteOffset, long byteLength, int alignment) {
			if ((byteLength & (alignment - 1)) != 0) {
				throw new IllegalStateException("Invalid alignment " + alignment + " for length " + byteLength);
			}
			if (manager.used + byteLength > manager.capacity) {
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
			}
		}

		public long baseAddress() {
			if (this.byteOffset + this.byteLength > this.manager.used) {
				throw new IllegalStateException("Attempt to use direct NumberArray after closing it.");
			}
			return this.manager.base() + this.byteOffset;
		}

		public static class OfByte extends Direct implements NumberArray.OfByte {

			public OfByte(Manager manager, long length) {
				super(manager, length, Byte.BYTES);
			}

			public OfByte(Manager manager, long offset, long length) {
				super(manager, offset, length, Byte.BYTES);
			}

			@Override
			public byte getB(int index) {
				return MemoryUtil.memGetByte(this.baseAddress() + Objects.checkIndex(((long)(index)) << BYTE_SHIFT, this.byteLength));
			}

			@Override
			public void setB(int index, byte value) {
				MemoryUtil.memPutByte(this.baseAddress() + Objects.checkIndex(((long)(index)) << BYTE_SHIFT, this.byteLength), value);
			}

			@Override
			public void fillFromTo(int from, int to, byte value) {
				long addressFrom = ((long)(from)) << BYTE_SHIFT;
				long addressTo = ((long)(to)) << BYTE_SHIFT;
				Objects.checkFromToIndex(addressFrom, addressTo, this.byteLength);
				long base = this.baseAddress();
				addressFrom += base;
				addressTo += base;
				for (long address = addressFrom; address < addressTo; address += Byte.BYTES) {
					MemoryUtil.memPutByte(address, value);
				}
			}

			@Override
			public int length() {
				return Math.toIntExact(this.byteLength >> BYTE_SHIFT);
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfByte(this.manager, this.byteOffset + (((long)(offset)) << BYTE_SHIFT), ((long)(length)) << length);
			}
		}

		public static class OfShort extends Direct implements NumberArray.OfShort {

			public OfShort(Manager manager, long length) {
				super(manager, length, Short.BYTES);
			}

			public OfShort(Manager manager, long offset, long length) {
				super(manager, offset, length, Short.BYTES);
			}

			@Override
			public short getS(int index) {
				return MemoryUtil.memGetShort(this.baseAddress() + Objects.checkIndex(((long)(index)) << SHORT_SHIFT, this.byteLength));
			}

			@Override
			public void setS(int index, short value) {
				MemoryUtil.memPutShort(this.baseAddress() + Objects.checkIndex(((long)(index)) << SHORT_SHIFT, this.byteLength), value);
			}

			@Override
			public void fillFromTo(int from, int to, short value) {
				long addressFrom = ((long)(from)) << SHORT_SHIFT;
				long addressTo = ((long)(to)) << SHORT_SHIFT;
				Objects.checkFromToIndex(addressFrom, addressTo, this.byteLength);
				long base = this.baseAddress();
				addressFrom += base;
				addressTo += base;
				for (long address = addressFrom; address < addressTo; address += Short.BYTES) {
					MemoryUtil.memPutShort(address, value);
				}
			}

			@Override
			public int length() {
				return Math.toIntExact(this.byteLength >> SHORT_SHIFT);
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfShort(this.manager, this.byteOffset + (((long)(offset)) << SHORT_SHIFT), ((long)(length)) << length);
			}
		}

		public static class OfInt extends Direct implements NumberArray.OfInt {

			public OfInt(Manager manager, long length) {
				super(manager, length, Integer.BYTES);
			}

			public OfInt(Manager manager, long offset, long length) {
				super(manager, offset, length, Integer.BYTES);
			}

			@Override
			public int getI(int index) {
				return MemoryUtil.memGetInt(this.baseAddress() + Objects.checkIndex(((long)(index)) << INT_SHIFT, this.byteLength));
			}

			@Override
			public void setI(int index, int value) {
				MemoryUtil.memPutInt(this.baseAddress() + Objects.checkIndex(((long)(index)) << INT_SHIFT, this.byteLength), value);
			}

			@Override
			public void fillFromTo(int from, int to, int value) {
				long addressFrom = ((long)(from)) << INT_SHIFT;
				long addressTo = ((long)(to)) << INT_SHIFT;
				Objects.checkFromToIndex(addressFrom, addressTo, this.byteLength);
				long base = this.baseAddress();
				addressFrom += base;
				addressTo += base;
				for (long address = addressFrom; address < addressTo; address += Integer.BYTES) {
					MemoryUtil.memPutInt(address, value);
				}
			}

			@Override
			public int length() {
				return Math.toIntExact(this.byteLength >> INT_SHIFT);
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfInt(this.manager, this.byteOffset + (((long)(offset)) << INT_SHIFT), ((long)(length)) << length);
			}
		}

		public static class OfLong extends Direct implements NumberArray.OfLong {

			public OfLong(Manager manager, long length) {
				super(manager, length, Long.BYTES);
			}

			public OfLong(Manager manager, long offset, long length) {
				super(manager, offset, length, Long.BYTES);
			}

			@Override
			public long getL(int index) {
				return MemoryUtil.memGetLong(this.baseAddress() + Objects.checkIndex(((long)(index)) << LONG_SHIFT, this.byteLength));
			}

			@Override
			public void setL(int index, long value) {
				MemoryUtil.memPutLong(this.baseAddress() + Objects.checkIndex(((long)(index)) << LONG_SHIFT, this.byteLength), value);
			}

			@Override
			public void fillFromTo(int from, int to, long value) {
				long addressFrom = ((long)(from)) << LONG_SHIFT;
				long addressTo = ((long)(to)) << LONG_SHIFT;
				Objects.checkFromToIndex(addressFrom, addressTo, this.byteLength);
				long base = this.baseAddress();
				addressFrom += base;
				addressTo += base;
				for (long address = addressFrom; address < addressTo; address += Long.BYTES) {
					MemoryUtil.memPutLong(address, value);
				}
			}

			@Override
			public int length() {
				return Math.toIntExact(this.byteLength >> LONG_SHIFT);
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfLong(this.manager, this.byteOffset + (((long)(offset)) << LONG_SHIFT), ((long)(length)) << length);
			}
		}

		public static class OfFloat extends Direct implements NumberArray.OfFloat {

			public OfFloat(Manager manager, long length) {
				super(manager, length, Float.BYTES);
			}

			public OfFloat(Manager manager, long offset, long length) {
				super(manager, offset, length, Float.BYTES);
			}

			@Override
			public float getF(int index) {
				return MemoryUtil.memGetFloat(this.baseAddress() + Objects.checkIndex(((long)(index)) << FLOAT_SHIFT, this.byteLength));
			}

			@Override
			public void setF(int index, float value) {
				MemoryUtil.memPutFloat(this.baseAddress() + Objects.checkIndex(((long)(index)) << FLOAT_SHIFT, this.byteLength), value);
			}

			@Override
			public void fillFromTo(int from, int to, float value) {
				long addressFrom = ((long)(from)) << FLOAT_SHIFT;
				long addressTo = ((long)(to)) << FLOAT_SHIFT;
				Objects.checkFromToIndex(addressFrom, addressTo, this.byteLength);
				long base = this.baseAddress();
				addressFrom += base;
				addressTo += base;
				for (long address = addressFrom; address < addressTo; address += Float.BYTES) {
					MemoryUtil.memPutFloat(address, value);
				}
			}

			@Override
			public int length() {
				return Math.toIntExact(this.byteLength >> FLOAT_SHIFT);
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfFloat(this.manager, this.byteOffset + (((long)(offset)) << FLOAT_SHIFT), ((long)(length)) << length);
			}
		}

		public static class OfDouble extends Direct implements NumberArray.OfDouble {

			public OfDouble(Manager manager, long length) {
				super(manager, length, Double.BYTES);
			}

			public OfDouble(Manager manager, long offset, long length) {
				super(manager, offset, length, Double.BYTES);
			}

			@Override
			public double getD(int index) {
				return MemoryUtil.memGetDouble(this.baseAddress() + Objects.checkIndex(((long)(index)) << DOUBLE_SHIFT, this.byteLength));
			}

			@Override
			public void setD(int index, double value) {
				MemoryUtil.memPutDouble(this.baseAddress() + Objects.checkIndex(((long)(index)) << DOUBLE_SHIFT, this.byteLength), value);
			}

			@Override
			public void fillFromTo(int from, int to, double value) {
				long addressFrom = ((long)(from)) << DOUBLE_SHIFT;
				long addressTo = ((long)(to)) << DOUBLE_SHIFT;
				Objects.checkFromToIndex(addressFrom, addressTo, this.byteLength);
				long base = this.baseAddress();
				addressFrom += base;
				addressTo += base;
				for (long address = addressFrom; address < addressTo; address += Double.BYTES) {
					MemoryUtil.memPutDouble(address, value);
				}
			}

			@Override
			public int length() {
				return Math.toIntExact(this.byteLength >> DOUBLE_SHIFT);
			}

			@Override
			public NumberArray sliceOffsetLength(int offset, int length) {
				Objects.checkFromIndexSize(offset, length, this.length());
				return new Direct.OfDouble(this.manager, this.byteOffset + (((long)(offset)) << DOUBLE_SHIFT), ((long)(length)) << length);
			}
		}

		public static class Manager {

			public static final long MAX_ALLOCATION = Long.getLong("bigglobe.NumberArray.maxAllocation", 1048576L);
			public static final ThreadLocal<Manager> INSTANCES = ThreadLocal.withInitial(Manager::new);

			public long base;
			public long used;
			public long capacity;

			public void ensureCapacity(long capacity) {
				if (capacity > MAX_ALLOCATION) {
					throw new OutOfMemoryError("Requested capacity " + capacity + " exceeds maximum allocation limit " + MAX_ALLOCATION + " as defined by java argument -Dbigglobe.NumberArray.maxAllocation");
				}
				if (this.capacity < capacity) {
					capacity = Math.min(Math.max(capacity, this.base == 0L ? 1024L * Double.BYTES : this.capacity << 1), MAX_ALLOCATION);
					this.base = this.base == 0L ? MemoryUtil.nmemAlloc(capacity) : MemoryUtil.nmemRealloc(this.base, capacity);
				}
			}

			public long beforeAllocate(long bytes) {
				this.ensureCapacity(this.used + bytes);
				return bytes;
			}

			public long base() {
				if (this.base != 0L) return this.base;
				else throw new IllegalStateException("Manager not yet initialized");
			}

			public Direct.OfByte allocateBytes(int bytes) {
				return new Direct.OfByte(this, this.beforeAllocate(((long)(bytes)) << BYTE_SHIFT));
			}

			public Direct.OfShort allocateShorts(int shorts) {
				return new Direct.OfShort(this, this.beforeAllocate(((long)(shorts)) << SHORT_SHIFT));
			}

			public Direct.OfInt allocateInts(int ints) {
				return new Direct.OfInt(this, this.beforeAllocate(((long)(ints)) << INT_SHIFT));
			}

			public Direct.OfLong allocateLongs(int longs) {
				return new Direct.OfLong(this, this.beforeAllocate(((long)(longs)) << LONG_SHIFT));
			}

			public Direct.OfFloat allocateFloats(int floats) {
				return new Direct.OfFloat(this, this.beforeAllocate(((long)(floats)) << FLOAT_SHIFT));
			}

			public Direct.OfDouble allocateDoubles(int doubles) {
				return new Direct.OfDouble(this, this.beforeAllocate(((long)(doubles)) << DOUBLE_SHIFT));
			}

			public char hex(int shift) {
				int number = ((int)(this.base >>> shift)) & 0xF;
				return (char)(number + (number >= 10 ? 'A' - 10 : '0'));
			}

			public String formatPointer() {
				return (
					"0x"
					+ this.hex(60) + this.hex(56) + this.hex(52) + this.hex(48)
					+ this.hex(44) + this.hex(40) + this.hex(36) + this.hex(32)
					+ this.hex(28) + this.hex(24) + this.hex(20) + this.hex(16)
					+ this.hex(12) + this.hex( 8) + this.hex( 4) + this.hex( 0)
				);
			}

			@Override
			public String toString() {
				return this.getClass().getName() + ": { base: " + this.formatPointer() + ", used: " + this.used + ", capacity: " + this.capacity + " }";
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

		@Override public void close() {}

		@Override public byte getB(int index) { return this.delegate.getB(Objects.checkIndex(index, this.length)); }
		@Override public short getS(int index) { return this.delegate.getS(Objects.checkIndex(index, this.length)); }
		@Override public int getI(int index) { return this.delegate.getI(Objects.checkIndex(index, this.length)); }
		@Override public long getL(int index) { return this.delegate.getL(Objects.checkIndex(index, this.length)); }
		@Override public float getF(int index) { return this.delegate.getF(Objects.checkIndex(index, this.length)); }
		@Override public double getD(int index) { return this.delegate.getD(Objects.checkIndex(index, this.length)); }

		@Override public void setB(int index, byte value) { this.delegate.setB(Objects.checkIndex(index, this.length), value); }
		@Override public void setS(int index, short value) { this.delegate.setS(Objects.checkIndex(index, this.length), value); }
		@Override public void setI(int index, int value) { this.delegate.setI(Objects.checkIndex(index, this.length), value); }
		@Override public void setL(int index, long value) { this.delegate.setL(Objects.checkIndex(index, this.length), value); }
		@Override public void setF(int index, float value) { this.delegate.setF(Objects.checkIndex(index, this.length), value); }
		@Override public void setD(int index, double value) { this.delegate.setD(Objects.checkIndex(index, this.length), value); }

		@Override public void fillFromTo(int from, int to, byte value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from, to, value); }
		@Override public void fillFromTo(int from, int to, short value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from, to, value); }
		@Override public void fillFromTo(int from, int to, int value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from, to, value); }
		@Override public void fillFromTo(int from, int to, long value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from, to, value); }
		@Override public void fillFromTo(int from, int to, float value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from, to, value); }
		@Override public void fillFromTo(int from, int to, double value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from, to, value); }

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

		@Override public void close() {}

		@Override public byte getB(int index) { return this.delegate.getB(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public short getS(int index) { return this.delegate.getS(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public int getI(int index) { return this.delegate.getI(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public long getL(int index) { return this.delegate.getL(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public float getF(int index) { return this.delegate.getF(Objects.checkIndex(index, this.length) + this.offset); }
		@Override public double getD(int index) { return this.delegate.getD(Objects.checkIndex(index, this.length) + this.offset); }

		@Override public void setB(int index, byte value) { this.delegate.setB(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setS(int index, short value) { this.delegate.setS(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setI(int index, int value) { this.delegate.setI(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setL(int index, long value) { this.delegate.setL(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setF(int index, float value) { this.delegate.setF(Objects.checkIndex(index, this.length) + this.offset, value); }
		@Override public void setD(int index, double value) { this.delegate.setD(Objects.checkIndex(index, this.length) + this.offset, value); }

		@Override public void fillFromTo(int from, int to, byte value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, short value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, int value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, long value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, float value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from + this.offset, to + this.offset, value); }
		@Override public void fillFromTo(int from, int to, double value) { Objects.checkFromToIndex(from,to, this.length); this.delegate.fillFromTo(from + this.offset, to + this.offset, value); }

		@Override public int length() { return this.length; }

		@Override
		public NumberArray sliceOffsetLength(int offset, int length) {
			Objects.checkFromIndexSize(offset, length, this.length);
			return this.delegate.sliceOffsetLength(offset + this.offset, length);
		}
	}
}