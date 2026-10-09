package builderb0y.bigglobe.rendering;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.*;

import builderb0y.bigglobe.util.SafeCloseable;

import static org.lwjgl.system.MemoryUtil.*;

@Environment(EnvType.CLIENT)
public class NativeMemory implements SafeCloseable {

	//MemoryUtil.memPutWhatever() calls reinterpret() every time,
	//which then calls Reflection.getCallerClass(), which is slow.
	//we avoid this overhead by caching the reinterpreted MemorySegment here.
	public static final MemorySegment MEMORY = MemorySegment.NULL.reinterpret(Long.MAX_VALUE);

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
		return MEMORY.get(ValueLayout.JAVA_BYTE, Objects.checkIndex(byteOffset, this.used) + this.address);
	}

	public short getShort(long byteOffset, ByteOrder order) {
		short value = MEMORY.get(ValueLayout.JAVA_SHORT, Objects.checkIndex(byteOffset, this.used - (Short.BYTES - 1)) + this.address);
		return order == ByteOrder.nativeOrder() ? value : Short.reverseBytes(value);
	}

	public int getInt(long byteOffset, ByteOrder order) {
		int value = MEMORY.get(ValueLayout.JAVA_INT, Objects.checkIndex(byteOffset, this.used - (Integer.BYTES - 1)) + this.address);
		return order == ByteOrder.nativeOrder() ? value : Integer.reverseBytes(value);
	}

	public long getLong(long byteOffset, ByteOrder order) {
		long value = MEMORY.get(ValueLayout.JAVA_LONG, Objects.checkIndex(byteOffset, this.used - (Long.BYTES - 1)) + this.address);
		return order == ByteOrder.nativeOrder() ? value : Long.reverseBytes(value);
	}

	public float getFloat(long byteOffset, ByteOrder order) {
		int value = MEMORY.get(ValueLayout.JAVA_INT, Objects.checkIndex(byteOffset, this.used - (Float.BYTES - 1)) + this.address);
		return Float.intBitsToFloat(order == ByteOrder.nativeOrder() ? value : Integer.reverseBytes(value));
	}

	public double getDouble(long byteOffset, ByteOrder order) {
		long value = MEMORY.get(ValueLayout.JAVA_LONG, Objects.checkIndex(byteOffset, this.used - (Double.BYTES - 1)) + this.address);
		return Double.longBitsToDouble(order == ByteOrder.nativeOrder() ? value : Long.reverseBytes(value));
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

	public long addressForAppending(long dataSize) {
		long nextSize = Math.addExact(this.used, dataSize);
		long address = this.ensureCapacity(nextSize) + this.used;
		this.used = nextSize;
		return address;
	}

	public NativeMemory appendByte(byte value) {
		MEMORY.set(
			ValueLayout.JAVA_BYTE,
			this.addressForAppending(Byte.BYTES),
			value
		);
		return this;
	}

	public NativeMemory appendShort(short value, ByteOrder order) {
		MEMORY.set(
			ValueLayout.JAVA_SHORT,
			this.addressForAppending(Short.BYTES),
			order == ByteOrder.nativeOrder() ? value : Short.reverseBytes(value)
		);
		return this;
	}

	public NativeMemory appendInt(int value, ByteOrder order) {
		MEMORY.set(
			ValueLayout.JAVA_INT,
			this.addressForAppending(Integer.BYTES),
			order == ByteOrder.nativeOrder() ? value : Integer.reverseBytes(value)
		);
		return this;
	}

	public NativeMemory appendLong(long value, ByteOrder order) {
		MEMORY.set(
			ValueLayout.JAVA_LONG,
			this.addressForAppending(Long.BYTES),
			order == ByteOrder.nativeOrder() ? value : Long.reverseBytes(value)
		);
		return this;
	}

	public NativeMemory appendFloat(float value, ByteOrder order) {
		MEMORY.set(
			ValueLayout.JAVA_INT,
			this.addressForAppending(Float.BYTES),
			order == ByteOrder.nativeOrder() ? Float.floatToRawIntBits(value) : Integer.reverseBytes(Float.floatToRawIntBits(value))
		);
		return this;
	}

	public NativeMemory appendDouble(double value, ByteOrder order) {
		MEMORY.set(
			ValueLayout.JAVA_LONG,
			this.addressForAppending(Double.BYTES),
			order == ByteOrder.nativeOrder() ? Double.doubleToRawLongBits(value) : Long.reverseBytes(Double.doubleToRawLongBits(value))
		);
		return this;
	}

	public NativeMemory append(NativeMemory that) {
		memCopy(that.address, this.addressForAppending(that.used), that.used);
		return this;
	}

	public int intUsed() {
		if (this.used <= Integer.MAX_VALUE) {
			return (int)(this.used);
		}
		else {
			throw new IllegalStateException("NativeMemory too big: " + this.used + " / " + this.capacity + " bytes used.");
		}
	}

	public int intCapacity() {
		if (this.capacity <= Integer.MAX_VALUE) {
			return (int)(this.capacity);
		}
		else {
			throw new IllegalStateException("NativeMemory too big: " + this.used + " / " + this.capacity + " bytes used.");
		}
	}

	public ByteBuffer toByteBuffer() {
		return MemoryUtil.memByteBuffer(this.address, this.intUsed());
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