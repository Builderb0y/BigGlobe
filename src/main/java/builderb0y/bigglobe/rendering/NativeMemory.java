package builderb0y.bigglobe.rendering;

import java.util.Objects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.bigglobe.util.SafeCloseable;

import static org.lwjgl.system.MemoryUtil.*;

@Environment(EnvType.CLIENT)
public class NativeMemory implements SafeCloseable {

	public long address;
	public long capacity;
	public long used;

	public NativeMemory() {}

	public NativeMemory(long capacity) {
		this.address = nmemAllocChecked(capacity);
		if (this.address == 0L) throw new OutOfMemoryError();
		this.capacity = capacity;
	}

	public boolean isEmpty() {
		return this.used == 0;
	}

	public void clear() {
		this.used = 0;
	}

	public byte getByte(long byteOffset) {
		return memGetByte(Objects.checkIndex(byteOffset, this.used) + this.address);
	}

	public short getShort(long byteOffset) {
		return memGetShort(Objects.checkIndex(byteOffset, this.used - (Short.BYTES - 1)) + this.address);
	}

	public int getInt(long byteOffset) {
		return memGetInt(Objects.checkIndex(byteOffset, this.used - (Integer.BYTES - 1)) + this.address);
	}

	public long getLong(long byteOffset) {
		return memGetLong(Objects.checkIndex(byteOffset, this.used - (Long.BYTES - 1)) + this.address);
	}

	public float getFloat(long byteOffset) {
		return memGetFloat(Objects.checkIndex(byteOffset, this.used - (Float.BYTES - 1)) + this.address);
	}

	public double getDouble(long byteOffset) {
		return memGetDouble(Objects.checkIndex(byteOffset, this.used - (Double.BYTES - 1)) + this.address);
	}

	public long ensureCapacity(long minCapacity) {
		if (this.capacity < minCapacity) {
			long newCapacity = this.capacity << 1;
			if (newCapacity < 0L /* overflow */) newCapacity = Long.MAX_VALUE;
			else newCapacity = Math.max(Math.max(newCapacity, 1024L), minCapacity);

			this.address = (
				this.address == 0L
				? nmemAllocChecked(newCapacity)
				: nmemReallocChecked(this.address, newCapacity)
			);
			this.capacity = newCapacity;
		}
		return this.address;
	}

	public void appendEmpty(int dataSize) {
		this.ensureCapacity(this.used = Math.addExact(this.used, dataSize));
	}

	public long addressForAppending(int dataSize) {
		long nextSize = Math.addExact(this.used, dataSize);
		long address = this.ensureCapacity(nextSize) + this.used;
		this.used = nextSize;
		return address;
	}

	public void appendByte(byte value) {
		memPutByte(this.addressForAppending(Byte.BYTES), value);
	}

	public void appendShort(short value) {
		memPutShort(this.addressForAppending(Short.BYTES), value);
	}

	public void appendInt(int value) {
		memPutInt(this.addressForAppending(Integer.BYTES), value);
	}

	public void appendLong(long value) {
		memPutLong(this.addressForAppending(Long.BYTES), value);
	}

	public void appendFloat(float value) {
		memPutFloat(this.addressForAppending(Float.BYTES), value);
	}

	public void appendDouble(double value) {
		memPutDouble(this.addressForAppending(Double.BYTES), value);
	}

	public void appendBytes(byte... values) {
		long address = this.addressForAppending(values.length);
		for (byte value : values) {
			memPutByte(address, value);
			address += Byte.BYTES;
		}
	}

	public void appendShorts(short... values) {
		long address = this.addressForAppending(Math.multiplyExact(values.length, Short.BYTES));
		for (short value : values) {
			memPutShort(address, value);
			address += Byte.BYTES;
		}
	}

	public void appendInts(int... values) {
		long address = this.addressForAppending(Math.multiplyExact(values.length, Integer.BYTES));
		for (int value : values) {
			memPutInt(address, value);
			address += Byte.BYTES;
		}
	}

	public void appendLongs(long... values) {
		long address = this.addressForAppending(Math.multiplyExact(values.length, Long.BYTES));
		for (long value : values) {
			memPutLong(address, value);
			address += Byte.BYTES;
		}
	}

	public void appendFloats(float... values) {
		long address = this.addressForAppending(Math.multiplyExact(values.length, Float.BYTES));
		for (float value : values) {
			memPutFloat(address, value);
			address += Byte.BYTES;
		}
	}

	public void appendDoubles(double... values) {
		long address = this.addressForAppending(Math.multiplyExact(values.length, Double.BYTES));
		for (double value : values) {
			memPutDouble(address, value);
			address += Byte.BYTES;
		}
	}

	@Override
	public void close() {
		long address = this.address;
		if (address != 0L) {
			this.address = 0L;
			this.capacity = 0;
			this.used = 0;
			nmemFree(address);
		}
	}
}