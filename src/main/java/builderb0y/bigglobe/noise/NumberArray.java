package builderb0y.bigglobe.noise;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

import org.intellij.lang.annotations.MagicConstant;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

/**
an abstraction layer above arrays of numbers.
all get and set methods will auto-cast to and from the underlying array component type.

supported number types include bytes, shorts, ints, longs, floats, doubles, and booleans.
supported number types do NOT include chars, despite the fact that they can be used for arithmetic.

NumberArray's can be created in 2 primary ways:
option 1: via the heap. this will allocate a new array to hold the internal data for every NumberArray created.
option 2: directly. this will use a shared, thread-local array for all NumberArray's created this way.

historically, "direct" NumberArray's would use a raw pointer to point to their data,
and use MemoryUtil to access and manipulate that pointer.
I found out the hard way that MemoryUtil isn't available on dedicated servers,
and I didn't want to jar-in-jar it. so I switched to using a byte[] for direct NumberArray's.
later, I discovered that all the different types of NumberArray's with
all their polymorphism actually had a noticeable effect on profiling data.
so now, the current version has monomorphized everything, and the
internal storage for both types of NumberArray's is identical.
the way the NumberArray was created no longer affects how it works internally.

direct number arrays are fundamentally temporary objects.
they are intended to be allocated, used, and closed
in the reverse order that they are allocated in.
this will be handled for you if you allocate them in try-with-resources statements.

instances of NumberArray are NOT thread-safe.
any thread may allocate NumberArray's, but each NumberArray
should ONLY be closed from the thread that allocated it.
*/
@SuppressWarnings({ "OverloadedMethodsWithSameNumberOfParameters", "NumericCastThatLosesPrecision", "ImplicitNumericConversion" })
public class NumberArray implements AutoCloseable {

	public static final boolean TRACE_ALLOCATIONS = Boolean.getBoolean("bigglobe.traceNumberArrayAllocations");
	public static final byte
		BYTE_TYPE     = 0,
		SHORT_TYPE    = 1,
		INT_TYPE      = 2,
		LONG_TYPE     = 3,
		FLOAT_TYPE    = 4,
		DOUBLE_TYPE   = 5,
		BOOLEAN_TYPE  = 6;
	public static final int
		BYTE_SHIFT    = 0,
		SHORT_SHIFT   = 1,
		INT_SHIFT     = 2,
		LONG_SHIFT    = 3,
		FLOAT_SHIFT   = 2,
		DOUBLE_SHIFT  = 3;
	public static final VarHandle
		BYTE_ACCESS   = MethodHandles.arrayElementVarHandle (byte  [].class).withInvokeExactBehavior(),
		SHORT_ACCESS  = MethodHandles.byteArrayViewVarHandle(short [].class, ByteOrder.nativeOrder()).withInvokeExactBehavior(),
		INT_ACCESS    = MethodHandles.byteArrayViewVarHandle(int   [].class, ByteOrder.nativeOrder()).withInvokeExactBehavior(),
		LONG_ACCESS   = MethodHandles.byteArrayViewVarHandle(long  [].class, ByteOrder.nativeOrder()).withInvokeExactBehavior(),
		FLOAT_ACCESS  = MethodHandles.byteArrayViewVarHandle(float [].class, ByteOrder.nativeOrder()).withInvokeExactBehavior(),
		DOUBLE_ACCESS = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.nativeOrder()).withInvokeExactBehavior();

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public MethodInfo
			allocateBytesHeap,
			allocateShortsHeap,
			allocateIntsHeap,
			allocateLongsHeap,
			allocateFloatsHeap,
			allocateDoublesHeap,
			allocateBooleansHeap,
			allocateBytesDirect,
			allocateShortsDirect,
			allocateIntsDirect,
			allocateLongsDirect,
			allocateFloatsDirect,
			allocateDoublesDirect,
			allocateBooleansDirect,
			getB,
			getS,
			getI,
			getL,
			getF,
			getD,
			getZ,
			setB,
			setS,
			setI,
			setL,
			setF,
			setD,
			setZ,
			prefix,
			sliceFromTo,
			sliceOffsetLength;
	}

	public static final NumberArray
		EMPTY_BYTE    = new NumberArray(   BYTE_TYPE),
		EMPTY_SHORT   = new NumberArray(  SHORT_TYPE),
		EMPTY_INT     = new NumberArray(    INT_TYPE),
		EMPTY_LONG    = new NumberArray(   LONG_TYPE),
		EMPTY_FLOAT   = new NumberArray(  FLOAT_TYPE),
		EMPTY_DOUBLE  = new NumberArray( DOUBLE_TYPE),
		EMPTY_BOOLEAN = new NumberArray(BOOLEAN_TYPE);

	@MagicConstant(
		intValues = {
			BYTE_TYPE,
			SHORT_TYPE,
			INT_TYPE,
			LONG_TYPE,
			FLOAT_TYPE,
			DOUBLE_TYPE,
			BOOLEAN_TYPE
		}
	)
	public final byte type;
	public Manager manager; //set to null when closed.
	public final int byteOffset, byteLength, elementOffset, elementCount;
	public final boolean freeable;
	public final Throwable allocator;

	/** creates a zero-sized, already-closed NumberArray. */
	public NumberArray(byte type) {
		this.type = type;
		this.byteOffset = this.byteLength = this.elementOffset = this.elementCount = 0;
		this.freeable = false;
		this.allocator = null;
	}

	public NumberArray(
		@MagicConstant(
			intValues = {
				BYTE_TYPE,
				SHORT_TYPE,
				INT_TYPE,
				LONG_TYPE,
				FLOAT_TYPE,
				DOUBLE_TYPE,
				BOOLEAN_TYPE
			}
		)
		byte type,
		Manager manager,
		int byteOffset,
		int byteLength,
		int elementOffset,
		int elementCount,
		boolean freeable
	) {
		if ((byteLength & 7) != 0) {
			throw new IllegalArgumentException("Invalid alignment for length " + byteLength);
		}
		if (freeable && manager.used + byteLength > manager.base.length) {
			throw new IllegalArgumentException("Manager has insufficient capacity for " + byteLength + " byte(s): " + manager);
		}
		if (byteLength < 0) {
			throw new IllegalArgumentException("byteLength < 0: " + byteLength);
		}
		if (elementCount < 0) {
			throw new IllegalArgumentException("elementCount < 0: " + elementCount);
		}
		this.type = type;
		this.manager = manager;
		this.byteOffset = byteOffset;
		this.byteLength = byteLength;
		this.elementOffset = elementOffset;
		this.elementCount = elementCount;
		this.freeable = freeable;
		if (freeable) manager.used += byteLength;
		this.allocator = TRACE_ALLOCATIONS && freeable ? new Throwable("Allocation site:") : null;
	}

	//////////////////////////////// allocation ////////////////////////////////

	public static NumberArray allocateBytesHeap     (int bytes   ) { return new Manager(bytes    << BYTE_SHIFT  ).allocateBytesHeap   (        ); }
	public static NumberArray allocateShortsHeap    (int shorts  ) { return new Manager(shorts   << SHORT_SHIFT ).allocateShortsHeap  (        ); }
	public static NumberArray allocateIntsHeap      (int ints    ) { return new Manager(ints     << INT_SHIFT   ).allocateIntsHeap    (        ); }
	public static NumberArray allocateLongsHeap     (int longs   ) { return new Manager(longs    << LONG_SHIFT  ).allocateLongsHeap   (        ); }
	public static NumberArray allocateFloatsHeap    (int floats  ) { return new Manager(floats   << FLOAT_SHIFT ).allocateFloatsHeap  (        ); }
	public static NumberArray allocateDoublesHeap   (int doubles ) { return new Manager(doubles  << DOUBLE_SHIFT).allocateDoublesHeap (        ); }
	public static NumberArray allocateBooleansHeap  (int booleans) { return new Manager(booleans >> 3           ).allocateBooleansHeap(booleans); }

	public static NumberArray allocateBytesDirect   (int bytes   ) { return Manager.INSTANCES.get().allocateBytesDirect   (bytes   ); }
	public static NumberArray allocateShortsDirect  (int shorts  ) { return Manager.INSTANCES.get().allocateShortsDirect  (shorts  ); }
	public static NumberArray allocateIntsDirect    (int ints    ) { return Manager.INSTANCES.get().allocateIntsDirect    (ints    ); }
	public static NumberArray allocateLongsDirect   (int longs   ) { return Manager.INSTANCES.get().allocateLongsDirect   (longs   ); }
	public static NumberArray allocateFloatsDirect  (int floats  ) { return Manager.INSTANCES.get().allocateFloatsDirect  (floats  ); }
	public static NumberArray allocateDoublesDirect (int doubles ) { return Manager.INSTANCES.get().allocateDoublesDirect (doubles ); }
	public static NumberArray allocateBooleansDirect(int booleans) { return Manager.INSTANCES.get().allocateBooleansDirect(booleans); }

	//////////////////////////////// util ////////////////////////////////

	public int   byteIndexUnchecked(int index) { return ((index + this.elementOffset) <<   BYTE_SHIFT) + this.byteOffset; }
	public int  shortIndexUnchecked(int index) { return ((index + this.elementOffset) <<  SHORT_SHIFT) + this.byteOffset; }
	public int    intIndexUnchecked(int index) { return ((index + this.elementOffset) <<    INT_SHIFT) + this.byteOffset; }
	public int   longIndexUnchecked(int index) { return ((index + this.elementOffset) <<   LONG_SHIFT) + this.byteOffset; }
	public int  floatIndexUnchecked(int index) { return ((index + this.elementOffset) <<  FLOAT_SHIFT) + this.byteOffset; }
	public int doubleIndexUnchecked(int index) { return ((index + this.elementOffset) << DOUBLE_SHIFT) + this.byteOffset; }

	public int   byteIndex         (int index) { return this.  byteIndexUnchecked(Objects.checkIndex(index, this.elementCount)); }
	public int  shortIndex         (int index) { return this. shortIndexUnchecked(Objects.checkIndex(index, this.elementCount)); }
	public int    intIndex         (int index) { return this.   intIndexUnchecked(Objects.checkIndex(index, this.elementCount)); }
	public int   longIndex         (int index) { return this.  longIndexUnchecked(Objects.checkIndex(index, this.elementCount)); }
	public int  floatIndex         (int index) { return this. floatIndexUnchecked(Objects.checkIndex(index, this.elementCount)); }
	public int doubleIndex         (int index) { return this.doubleIndexUnchecked(Objects.checkIndex(index, this.elementCount)); }

	//////////////////////////////// get ////////////////////////////////

	public byte    implGetB(int index) { return (byte  )(  BYTE_ACCESS.get(this.manager.base, this.  byteIndex(index))); }
	public short   implGetS(int index) { return (short )( SHORT_ACCESS.get(this.manager.base, this. shortIndex(index))); }
	public int     implGetI(int index) { return (int   )(   INT_ACCESS.get(this.manager.base, this.   intIndex(index))); }
	public long    implGetL(int index) { return (long  )(  LONG_ACCESS.get(this.manager.base, this.  longIndex(index))); }
	public float   implGetF(int index) { return (float )( FLOAT_ACCESS.get(this.manager.base, this. floatIndex(index))); }
	public double  implGetD(int index) { return (double)(DOUBLE_ACCESS.get(this.manager.base, this.doubleIndex(index))); }
	public boolean implGetZ(int index) {
		index = Objects.checkIndex(index, this.elementCount) + this.elementOffset;
		return ((this.manager.base[(index >>> 3) + this.byteOffset] >>> (index & 7)) & 1) != 0;
	}

	public byte getB(int index) {
		return switch (this.type) {
			case    BYTE_TYPE -> (byte)(this.implGetB(index));
			case   SHORT_TYPE -> (byte)(this.implGetS(index));
			case     INT_TYPE -> (byte)(this.implGetI(index));
			case    LONG_TYPE -> (byte)(this.implGetL(index));
			case   FLOAT_TYPE -> (byte)(this.implGetF(index));
			case  DOUBLE_TYPE -> (byte)(this.implGetD(index));
			case BOOLEAN_TYPE ->        this.implGetZ(index) ? (byte)(1) : (byte)(0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		};
	}

	public short getS(int index) {
		return switch (this.type) {
			case    BYTE_TYPE -> (short)(this.implGetB(index));
			case   SHORT_TYPE -> (short)(this.implGetS(index));
			case     INT_TYPE -> (short)(this.implGetI(index));
			case    LONG_TYPE -> (short)(this.implGetL(index));
			case   FLOAT_TYPE -> (short)(this.implGetF(index));
			case  DOUBLE_TYPE -> (short)(this.implGetD(index));
			case BOOLEAN_TYPE ->         this.implGetZ(index) ? (short)(1) : (short)(0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		};
	}

	public int getI(int index) {
		return switch (this.type) {
			case    BYTE_TYPE -> (int)(this.implGetB(index));
			case   SHORT_TYPE -> (int)(this.implGetS(index));
			case     INT_TYPE -> (int)(this.implGetI(index));
			case    LONG_TYPE -> (int)(this.implGetL(index));
			case   FLOAT_TYPE -> (int)(this.implGetF(index));
			case  DOUBLE_TYPE -> (int)(this.implGetD(index));
			case BOOLEAN_TYPE ->       this.implGetZ(index) ? 1 : 0;
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		};
	}

	public long getL(int index) {
		return switch (this.type) {
			case    BYTE_TYPE -> (long)(this.implGetB(index));
			case   SHORT_TYPE -> (long)(this.implGetS(index));
			case     INT_TYPE -> (long)(this.implGetI(index));
			case    LONG_TYPE -> (long)(this.implGetL(index));
			case   FLOAT_TYPE -> (long)(this.implGetF(index));
			case  DOUBLE_TYPE -> (long)(this.implGetD(index));
			case BOOLEAN_TYPE ->        this.implGetZ(index) ? 1L : 0L;
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		};
	}

	public float getF(int index) {
		return switch (this.type) {
			case    BYTE_TYPE -> (float)(this.implGetB(index));
			case   SHORT_TYPE -> (float)(this.implGetS(index));
			case     INT_TYPE -> (float)(this.implGetI(index));
			case    LONG_TYPE -> (float)(this.implGetL(index));
			case   FLOAT_TYPE -> (float)(this.implGetF(index));
			case  DOUBLE_TYPE -> (float)(this.implGetD(index));
			case BOOLEAN_TYPE ->         this.implGetZ(index) ? 1.0F : 0.0F;
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		};
	}

	public double getD(int index) {
		return switch (this.type) {
			case    BYTE_TYPE -> (double)(this.implGetB(index));
			case   SHORT_TYPE -> (double)(this.implGetS(index));
			case     INT_TYPE -> (double)(this.implGetI(index));
			case    LONG_TYPE -> (double)(this.implGetL(index));
			case   FLOAT_TYPE -> (double)(this.implGetF(index));
			case  DOUBLE_TYPE -> (double)(this.implGetD(index));
			case BOOLEAN_TYPE ->          this.implGetZ(index) ? 1.0D : 0.0D;
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		};
	}

	public boolean getZ(int index) {
		return switch (this.type) {
			case    BYTE_TYPE -> this.implGetB(index) != 0;
			case   SHORT_TYPE -> this.implGetS(index) != 0;
			case     INT_TYPE -> this.implGetI(index) != 0;
			case    LONG_TYPE -> this.implGetL(index) != 0L;
			case   FLOAT_TYPE -> this.implGetF(index) != 0.0F;
			case  DOUBLE_TYPE -> this.implGetD(index) != 0.0D;
			case BOOLEAN_TYPE -> this.implGetZ(index);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		};
	}

	//////////////////////////////// set ////////////////////////////////

	public void implSetB(int index, byte    value) {   BYTE_ACCESS.set(this.manager.base, this.  byteIndex(index), value); }
	public void implSetS(int index, short   value) {  SHORT_ACCESS.set(this.manager.base, this. shortIndex(index), value); }
	public void implSetI(int index, int     value) {    INT_ACCESS.set(this.manager.base, this.   intIndex(index), value); }
	public void implSetL(int index, long    value) {   LONG_ACCESS.set(this.manager.base, this.  longIndex(index), value); }
	public void implSetF(int index, float   value) {  FLOAT_ACCESS.set(this.manager.base, this. floatIndex(index), value); }
	public void implSetD(int index, double  value) { DOUBLE_ACCESS.set(this.manager.base, this.doubleIndex(index), value); }
	public void implSetZ(int index, boolean value) {
		index = Objects.checkIndex(index, this.elementCount) + this.elementOffset;
		if (value) {
			this.manager.base[(index >>> 3) + this.byteOffset] |=  (1 << (index & 7));
		}
		else {
			this.manager.base[(index >>> 3) + this.byteOffset] &= ~(1 << (index & 7));
		}
	}

	public void setB(int index, byte value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implSetB(index, (byte  )(value));
			case   SHORT_TYPE -> this.implSetS(index, (short )(value));
			case     INT_TYPE -> this.implSetI(index, (int   )(value));
			case    LONG_TYPE -> this.implSetL(index, (long  )(value));
			case   FLOAT_TYPE -> this.implSetF(index, (float )(value));
			case  DOUBLE_TYPE -> this.implSetD(index, (double)(value));
			case BOOLEAN_TYPE -> this.implSetZ(index, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void setS(int index, short value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implSetB(index, (byte  )(value));
			case   SHORT_TYPE -> this.implSetS(index, (short )(value));
			case     INT_TYPE -> this.implSetI(index, (int   )(value));
			case    LONG_TYPE -> this.implSetL(index, (long  )(value));
			case   FLOAT_TYPE -> this.implSetF(index, (float )(value));
			case  DOUBLE_TYPE -> this.implSetD(index, (double)(value));
			case BOOLEAN_TYPE -> this.implSetZ(index, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void setI(int index, int value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implSetB(index, (byte  )(value));
			case   SHORT_TYPE -> this.implSetS(index, (short )(value));
			case     INT_TYPE -> this.implSetI(index, (int   )(value));
			case    LONG_TYPE -> this.implSetL(index, (long  )(value));
			case   FLOAT_TYPE -> this.implSetF(index, (float )(value));
			case  DOUBLE_TYPE -> this.implSetD(index, (double)(value));
			case BOOLEAN_TYPE -> this.implSetZ(index, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void setL(int index, long value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implSetB(index, (byte  )(value));
			case   SHORT_TYPE -> this.implSetS(index, (short )(value));
			case     INT_TYPE -> this.implSetI(index, (int   )(value));
			case    LONG_TYPE -> this.implSetL(index, (long  )(value));
			case   FLOAT_TYPE -> this.implSetF(index, (float )(value));
			case  DOUBLE_TYPE -> this.implSetD(index, (double)(value));
			case BOOLEAN_TYPE -> this.implSetZ(index, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void setF(int index, float value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implSetB(index, (byte  )(value));
			case   SHORT_TYPE -> this.implSetS(index, (short )(value));
			case     INT_TYPE -> this.implSetI(index, (int   )(value));
			case    LONG_TYPE -> this.implSetL(index, (long  )(value));
			case   FLOAT_TYPE -> this.implSetF(index, (float )(value));
			case  DOUBLE_TYPE -> this.implSetD(index, (double)(value));
			case BOOLEAN_TYPE -> this.implSetZ(index, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void setD(int index, double value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implSetB(index, (byte  )(value));
			case   SHORT_TYPE -> this.implSetS(index, (short )(value));
			case     INT_TYPE -> this.implSetI(index, (int   )(value));
			case    LONG_TYPE -> this.implSetL(index, (long  )(value));
			case   FLOAT_TYPE -> this.implSetF(index, (float )(value));
			case  DOUBLE_TYPE -> this.implSetD(index, (double)(value));
			case BOOLEAN_TYPE -> this.implSetZ(index, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void setZ(int index, boolean value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implSetB(index, value ? (byte )(1)   : (byte )(0)  );
			case   SHORT_TYPE -> this.implSetS(index, value ? (short)(1)   : (short)(0)  );
			case     INT_TYPE -> this.implSetI(index, value ?         1    :         0   );
			case    LONG_TYPE -> this.implSetL(index, value ?         1L   :         0L  );
			case   FLOAT_TYPE -> this.implSetF(index, value ?         1.0F :         0.0F);
			case  DOUBLE_TYPE -> this.implSetD(index, value ?         1.0D :         0.0D);
			case BOOLEAN_TYPE -> this.implSetZ(index, value                              );
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	//////////////////////////////// fill ////////////////////////////////

	public void implFillFromTo(int from, int to, byte value) {
		byte[] base = this.manager.base;
		Objects.checkFromToIndex(from, to, this.elementCount);
		if (from == to) return;
		from = this.byteIndexUnchecked(from);
		to = this.byteIndexUnchecked(to);
		for (int index = from; index < to; index += Byte.BYTES) {
			BYTE_ACCESS.set(base, index, value);
		}
	}

	public void implFillFromTo(int from, int to, short value) {
		byte[] base = this.manager.base;
		Objects.checkFromToIndex(from, to, this.elementCount);
		if (from == to) return;
		from = this.shortIndexUnchecked(from);
		to = this.shortIndexUnchecked(to);
		for (int index = from; index < to; index += Short.BYTES) {
			SHORT_ACCESS.set(base, index, value);
		}
	}

	public void implFillFromTo(int from, int to, int value) {
		byte[] base = this.manager.base;
		Objects.checkFromToIndex(from, to, this.elementCount);
		if (from == to) return;
		from = this.intIndexUnchecked(from);
		to = this.intIndexUnchecked(to);
		for (int index = from; index < to; index += Integer.BYTES) {
			INT_ACCESS.set(base, index, value);
		}
	}

	public void implFillFromTo(int from, int to, long value) {
		byte[] base = this.manager.base;
		Objects.checkFromToIndex(from, to, this.elementCount);
		if (from == to) return;
		from = this.longIndexUnchecked(from);
		to = this.longIndexUnchecked(to);
		for (int index = from; index < to; index += Long.BYTES) {
			LONG_ACCESS.set(base, index, value);
		}
	}

	public void implFillFromTo(int from, int to, float value) {
		byte[] base = this.manager.base;
		Objects.checkFromToIndex(from, to, this.elementCount);
		if (from == to) return;
		from = this.floatIndexUnchecked(from);
		to = this.floatIndexUnchecked(to);
		for (int index = from; index < to; index += Float.BYTES) {
			FLOAT_ACCESS.set(base, index, value);
		}
	}

	public void implFillFromTo(int from, int to, double value) {
		byte[] base = this.manager.base;
		Objects.checkFromToIndex(from, to, this.elementCount);
		if (from == to) return;
		from = this.doubleIndexUnchecked(from);
		to = this.doubleIndexUnchecked(to);
		for (int index = from; index < to; index += Double.BYTES) {
			DOUBLE_ACCESS.set(base, index, value);
		}
	}

	public void implFillFromTo(int from, int to, boolean value) {
		byte[] base = this.manager.base;
		Objects.checkFromToIndex(from, to, this.elementCount);
		if (from == to) return;
		from += this.elementOffset;
		to   += this.elementOffset;
		int firstByteIndex = ( (from)      >>> 3) + this.byteOffset;
		int  lastByteIndex = (((to  ) - 1) >>> 3) + this.byteOffset;
		byte firstByteMask = (byte)(255 << (from & 7));
		byte  lastByteMask = (byte)(255 >>> ((-to) & 7));
		if (value) {
			if (firstByteIndex == lastByteIndex) {
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
			if (firstByteIndex == lastByteIndex) {
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

	public void fill(byte    value) { this.fillFromTo(0, this.elementCount, value); }
	public void fill(short   value) { this.fillFromTo(0, this.elementCount, value); }
	public void fill(int     value) { this.fillFromTo(0, this.elementCount, value); }
	public void fill(long    value) { this.fillFromTo(0, this.elementCount, value); }
	public void fill(float   value) { this.fillFromTo(0, this.elementCount, value); }
	public void fill(double  value) { this.fillFromTo(0, this.elementCount, value); }
	public void fill(boolean value) { this.fillFromTo(0, this.elementCount, value); }

	public void fillFromTo(int from, int to, byte value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implFillFromTo(from, to, (byte)(value));
			case   SHORT_TYPE -> this.implFillFromTo(from, to, (short)(value));
			case     INT_TYPE -> this.implFillFromTo(from, to, (int)(value));
			case    LONG_TYPE -> this.implFillFromTo(from, to, (long)(value));
			case   FLOAT_TYPE -> this.implFillFromTo(from, to, (float)(value));
			case  DOUBLE_TYPE -> this.implFillFromTo(from, to, (double)(value));
			case BOOLEAN_TYPE -> this.implFillFromTo(from, to, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void fillFromTo(int from, int to, short value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implFillFromTo(from, to, (byte)(value));
			case   SHORT_TYPE -> this.implFillFromTo(from, to, (short)(value));
			case     INT_TYPE -> this.implFillFromTo(from, to, (int)(value));
			case    LONG_TYPE -> this.implFillFromTo(from, to, (long)(value));
			case   FLOAT_TYPE -> this.implFillFromTo(from, to, (float)(value));
			case  DOUBLE_TYPE -> this.implFillFromTo(from, to, (double)(value));
			case BOOLEAN_TYPE -> this.implFillFromTo(from, to, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void fillFromTo(int from, int to, int value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implFillFromTo(from, to, (byte)(value));
			case   SHORT_TYPE -> this.implFillFromTo(from, to, (short)(value));
			case     INT_TYPE -> this.implFillFromTo(from, to, (int)(value));
			case    LONG_TYPE -> this.implFillFromTo(from, to, (long)(value));
			case   FLOAT_TYPE -> this.implFillFromTo(from, to, (float)(value));
			case  DOUBLE_TYPE -> this.implFillFromTo(from, to, (double)(value));
			case BOOLEAN_TYPE -> this.implFillFromTo(from, to, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void fillFromTo(int from, int to, long value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implFillFromTo(from, to, (byte)(value));
			case   SHORT_TYPE -> this.implFillFromTo(from, to, (short)(value));
			case     INT_TYPE -> this.implFillFromTo(from, to, (int)(value));
			case    LONG_TYPE -> this.implFillFromTo(from, to, (long)(value));
			case   FLOAT_TYPE -> this.implFillFromTo(from, to, (float)(value));
			case  DOUBLE_TYPE -> this.implFillFromTo(from, to, (double)(value));
			case BOOLEAN_TYPE -> this.implFillFromTo(from, to, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void fillFromTo(int from, int to, float value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implFillFromTo(from, to, (byte)(value));
			case   SHORT_TYPE -> this.implFillFromTo(from, to, (short)(value));
			case     INT_TYPE -> this.implFillFromTo(from, to, (int)(value));
			case    LONG_TYPE -> this.implFillFromTo(from, to, (long)(value));
			case   FLOAT_TYPE -> this.implFillFromTo(from, to, (float)(value));
			case  DOUBLE_TYPE -> this.implFillFromTo(from, to, (double)(value));
			case BOOLEAN_TYPE -> this.implFillFromTo(from, to, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void fillFromTo(int from, int to, double value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implFillFromTo(from, to, (byte)(value));
			case   SHORT_TYPE -> this.implFillFromTo(from, to, (short)(value));
			case     INT_TYPE -> this.implFillFromTo(from, to, (int)(value));
			case    LONG_TYPE -> this.implFillFromTo(from, to, (long)(value));
			case   FLOAT_TYPE -> this.implFillFromTo(from, to, (float)(value));
			case  DOUBLE_TYPE -> this.implFillFromTo(from, to, (double)(value));
			case BOOLEAN_TYPE -> this.implFillFromTo(from, to, value != 0);
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void fillFromTo(int from, int to, boolean value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implFillFromTo(from, to, value ? (byte )(1)   : (byte )(0)  );
			case   SHORT_TYPE -> this.implFillFromTo(from, to, value ? (short)(1)   : (short)(0)  );
			case     INT_TYPE -> this.implFillFromTo(from, to, value ?         1    :         0   );
			case    LONG_TYPE -> this.implFillFromTo(from, to, value ?         1L   :         0L  );
			case   FLOAT_TYPE -> this.implFillFromTo(from, to, value ?         1.0F :         0.0F);
			case  DOUBLE_TYPE -> this.implFillFromTo(from, to, value ?         1.0D :         0.0D);
			case BOOLEAN_TYPE -> this.implFillFromTo(from, to, value                              );
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	//////////////////////////////// add ////////////////////////////////

	public void implAdd(int index, byte value) {
		byte[] base = this.manager.base;
		index = this.byteIndex(index);
		BYTE_ACCESS.set(base, index, ((byte)(((byte)(BYTE_ACCESS.get(base, index))) + value)));
	}

	public void implAdd(int index, short value) {
		byte[] base = this.manager.base;
		index = this.shortIndex(index);
		SHORT_ACCESS.set(base, index, ((short)(((short)(SHORT_ACCESS.get(base, index))) + value)));
	}

	public void implAdd(int index, int value) {
		byte[] base = this.manager.base;
		index = this.intIndex(index);
		INT_ACCESS.set(base, index, ((int)(INT_ACCESS.get(base, index))) + value);
	}

	public void implAdd(int index, long value) {
		byte[] base = this.manager.base;
		index = this.longIndex(index);
		LONG_ACCESS.set(base, index, ((long)(LONG_ACCESS.get(base, index))) + value);
	}

	public void implAdd(int index, float value) {
		byte[] base = this.manager.base;
		index = this.floatIndex(index);
		FLOAT_ACCESS.set(base, index, ((float)(FLOAT_ACCESS.get(base, index))) + value);
	}

	public void implAdd(int index, double value) {
		byte[] base = this.manager.base;
		index = this.doubleIndex(index);
		DOUBLE_ACCESS.set(base, index, ((double)(DOUBLE_ACCESS.get(base, index))) + value);
	}

	public void add(int index, byte value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implAdd(index, (byte  )(value));
			case   SHORT_TYPE -> this.implAdd(index, (short )(value));
			case     INT_TYPE -> this.implAdd(index, (int   )(value));
			case    LONG_TYPE -> this.implAdd(index, (long  )(value));
			case   FLOAT_TYPE -> this.implAdd(index, (float )(value));
			case  DOUBLE_TYPE -> this.implAdd(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't add booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void add(int index, short value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implAdd(index, (byte  )(value));
			case   SHORT_TYPE -> this.implAdd(index, (short )(value));
			case     INT_TYPE -> this.implAdd(index, (int   )(value));
			case    LONG_TYPE -> this.implAdd(index, (long  )(value));
			case   FLOAT_TYPE -> this.implAdd(index, (float )(value));
			case  DOUBLE_TYPE -> this.implAdd(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't add booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void add(int index, int value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implAdd(index, (byte  )(value));
			case   SHORT_TYPE -> this.implAdd(index, (short )(value));
			case     INT_TYPE -> this.implAdd(index, (int   )(value));
			case    LONG_TYPE -> this.implAdd(index, (long  )(value));
			case   FLOAT_TYPE -> this.implAdd(index, (float )(value));
			case  DOUBLE_TYPE -> this.implAdd(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't add booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void add(int index, long value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implAdd(index, (byte  )(value));
			case   SHORT_TYPE -> this.implAdd(index, (short )(value));
			case     INT_TYPE -> this.implAdd(index, (int   )(value));
			case    LONG_TYPE -> this.implAdd(index, (long  )(value));
			case   FLOAT_TYPE -> this.implAdd(index, (float )(value));
			case  DOUBLE_TYPE -> this.implAdd(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't add booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void add(int index, float value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implAdd(index, (byte  )(value));
			case   SHORT_TYPE -> this.implAdd(index, (short )(value));
			case     INT_TYPE -> this.implAdd(index, (int   )(value));
			case    LONG_TYPE -> this.implAdd(index, (long  )(value));
			case   FLOAT_TYPE -> this.implAdd(index, (float )(value));
			case  DOUBLE_TYPE -> this.implAdd(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't add booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void add(int index, double value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implAdd(index, (byte  )(value));
			case   SHORT_TYPE -> this.implAdd(index, (short )(value));
			case     INT_TYPE -> this.implAdd(index, (int   )(value));
			case    LONG_TYPE -> this.implAdd(index, (long  )(value));
			case   FLOAT_TYPE -> this.implAdd(index, (float )(value));
			case  DOUBLE_TYPE -> this.implAdd(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't add booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	//////////////////////////////// mul ////////////////////////////////

	public void implMul(int index, byte value) {
		byte[] base = this.manager.base;
		index = this.byteIndex(index);
		BYTE_ACCESS.set(base, index, ((byte)(((byte)(BYTE_ACCESS.get(base, index))) * value)));
	}

	public void implMul(int index, short value) {
		byte[] base = this.manager.base;
		index = this.shortIndex(index);
		SHORT_ACCESS.set(base, index, ((short)(((short)(SHORT_ACCESS.get(base, index))) * value)));
	}

	public void implMul(int index, int value) {
		byte[] base = this.manager.base;
		index = this.intIndex(index);
		INT_ACCESS.set(base, index, ((int)(INT_ACCESS.get(base, index))) * value);
	}

	public void implMul(int index, long value) {
		byte[] base = this.manager.base;
		index = this.longIndex(index);
		LONG_ACCESS.set(base, index, ((long)(LONG_ACCESS.get(base, index))) * value);
	}

	public void implMul(int index, float value) {
		byte[] base = this.manager.base;
		index = this.floatIndex(index);
		FLOAT_ACCESS.set(base, index, ((float)(FLOAT_ACCESS.get(base, index))) * value);
	}

	public void implMul(int index, double value) {
		byte[] base = this.manager.base;
		index = this.doubleIndex(index);
		DOUBLE_ACCESS.set(base, index, ((double)(DOUBLE_ACCESS.get(base, index))) * value);
	}

	public void mul(int index, byte value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMul(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMul(index, (short )(value));
			case     INT_TYPE -> this.implMul(index, (int   )(value));
			case    LONG_TYPE -> this.implMul(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMul(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMul(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't mul booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void mul(int index, short value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMul(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMul(index, (short )(value));
			case     INT_TYPE -> this.implMul(index, (int   )(value));
			case    LONG_TYPE -> this.implMul(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMul(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMul(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't mul booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void mul(int index, int value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMul(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMul(index, (short )(value));
			case     INT_TYPE -> this.implMul(index, (int   )(value));
			case    LONG_TYPE -> this.implMul(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMul(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMul(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't mul booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void mul(int index, long value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMul(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMul(index, (short )(value));
			case     INT_TYPE -> this.implMul(index, (int   )(value));
			case    LONG_TYPE -> this.implMul(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMul(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMul(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't mul booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void mul(int index, float value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMul(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMul(index, (short )(value));
			case     INT_TYPE -> this.implMul(index, (int   )(value));
			case    LONG_TYPE -> this.implMul(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMul(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMul(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't mul booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void mul(int index, double value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMul(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMul(index, (short )(value));
			case     INT_TYPE -> this.implMul(index, (int   )(value));
			case    LONG_TYPE -> this.implMul(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMul(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMul(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't mul booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	//////////////////////////////// min (used by worley noise) ////////////////////////////////

	public void implMin(int index, byte value) {
		byte[] base = this.manager.base;
		index = this.byteIndex(index);
		BYTE_ACCESS.set(base, index, (byte)(Math.min((byte)(BYTE_ACCESS.get(base, index)), value)));
	}

	public void implMin(int index, short value) {
		byte[] base = this.manager.base;
		index = this.shortIndex(index);
		SHORT_ACCESS.set(base, index, (short)(Math.min((short)(SHORT_ACCESS.get(base, index)), value)));
	}

	public void implMin(int index, int value) {
		byte[] base = this.manager.base;
		index = this.intIndex(index);
		INT_ACCESS.set(base, index, Math.min(((int)(INT_ACCESS.get(base, index))), value));
	}

	public void implMin(int index, long value) {
		byte[] base = this.manager.base;
		index = this.longIndex(index);
		LONG_ACCESS.set(base, index, Math.min(((long)(LONG_ACCESS.get(base, index))), value));
	}

	public void implMin(int index, float value) {
		byte[] base = this.manager.base;
		index = this.floatIndex(index);
		FLOAT_ACCESS.set(base, index, Math.min(((float)(FLOAT_ACCESS.get(base, index))), value));
	}

	public void implMin(int index, double value) {
		byte[] base = this.manager.base;
		index = this.doubleIndex(index);
		DOUBLE_ACCESS.set(base, index, Math.min(((double)(DOUBLE_ACCESS.get(base, index))), value));
	}

	public void min(int index, byte value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMin(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMin(index, (short )(value));
			case     INT_TYPE -> this.implMin(index, (int   )(value));
			case    LONG_TYPE -> this.implMin(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMin(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMin(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't min booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void min(int index, short value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMin(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMin(index, (short )(value));
			case     INT_TYPE -> this.implMin(index, (int   )(value));
			case    LONG_TYPE -> this.implMin(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMin(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMin(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't min booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void min(int index, int value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMin(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMin(index, (short )(value));
			case     INT_TYPE -> this.implMin(index, (int   )(value));
			case    LONG_TYPE -> this.implMin(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMin(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMin(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't min booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void min(int index, long value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMin(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMin(index, (short )(value));
			case     INT_TYPE -> this.implMin(index, (int   )(value));
			case    LONG_TYPE -> this.implMin(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMin(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMin(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't min booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void min(int index, float value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMin(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMin(index, (short )(value));
			case     INT_TYPE -> this.implMin(index, (int   )(value));
			case    LONG_TYPE -> this.implMin(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMin(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMin(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't min booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	public void min(int index, double value) {
		switch (this.type) {
			case    BYTE_TYPE -> this.implMin(index, (byte  )(value));
			case   SHORT_TYPE -> this.implMin(index, (short )(value));
			case     INT_TYPE -> this.implMin(index, (int   )(value));
			case    LONG_TYPE -> this.implMin(index, (long  )(value));
			case   FLOAT_TYPE -> this.implMin(index, (float )(value));
			case  DOUBLE_TYPE -> this.implMin(index, (double)(value));
			case BOOLEAN_TYPE -> throw new IllegalStateException("Can't min booleans");
			default -> throw new IllegalStateException("Invalid type: " + this.type);
		}
	}

	//////////////////////////////// misc ////////////////////////////////

	public int length() {
		return this.elementCount;
	}

	public NumberArray prefix(int length) {
		return this.sliceOffsetLength(0, length);
	}

	public NumberArray sliceFromTo(int from, int to) {
		Objects.checkFromToIndex(from, to, this.elementCount);
		return new NumberArray(this.type, this.manager, this.byteOffset, this.byteLength, from, to - from, false);
	}

	public NumberArray sliceOffsetLength(int offset, int length) {
		Objects.checkFromIndexSize(offset, length, this.elementCount);
		return new NumberArray(this.type, this.manager, this.byteOffset, this.byteLength, offset, length, false);
	}

	@Override
	public void close() {
		if (this.freeable && this.manager != null /* ensure not already closed */) {
			if (this.manager.used == this.byteOffset + this.byteLength) {
				this.manager.used = this.byteOffset;
			}
			else {
				throw new IllegalStateException("Attempt to close NumberArray in wrong order!", this.allocator);
			}
		}
		this.manager = null;
	}

	@Override
	public String toString() {
		if (this.manager == null) return "NumberArray (closed)";
		byte[] base = this.manager.base;
		int elementCount = this.elementCount;
		StringBuilder builder;
		int from, to, index;
		return switch (this.type) {
			case BYTE_TYPE -> {
				builder = new StringBuilder(elementCount * 6).append('[');
				from = this.byteIndexUnchecked(0);
				to = this.byteIndexUnchecked(elementCount);
				for (index = from; index < to; index += Byte.BYTES) {
					builder.append((byte)(BYTE_ACCESS.get(base, index)));
					if (index + 1 != to) builder.append(", ");
				}
				yield builder.append(']').toString();
			}
			case SHORT_TYPE -> {
				builder = new StringBuilder(elementCount * 8).append('[');
				from = this.shortIndexUnchecked(0);
				to = this.shortIndexUnchecked(elementCount);
				for (index = from; index < to; index += Short.BYTES) {
					builder.append((short)(SHORT_ACCESS.get(base, index)));
					if (index + 1 != to) builder.append(", ");
				}
				yield builder.append(']').toString();
			}
			case INT_TYPE -> {
				builder = new StringBuilder(elementCount * 11).append('[');
				from = this.intIndexUnchecked(0);
				to = this.intIndexUnchecked(elementCount);
				for (index = from; index < to; index += Integer.BYTES) {
					builder.append((int)(INT_ACCESS.get(base, index)));
					if (index + 1 != to) builder.append(", ");
				}
				yield builder.append(']').toString();
			}
			case LONG_TYPE -> {
				builder = new StringBuilder(elementCount * 22).append('[');
				from = this.intIndexUnchecked(0);
				to = this.intIndexUnchecked(elementCount);
				for (index = from; index < to; index += Long.BYTES) {
					builder.append((long)(LONG_ACCESS.get(base, index)));
					if (index + 1 != to) builder.append(", ");
				}
				yield builder.append(']').toString();
			}
			case FLOAT_TYPE -> {
				builder = new StringBuilder(elementCount * 22).append('[');
				from = this.intIndexUnchecked(0);
				to = this.intIndexUnchecked(elementCount);
				for (index = from; index < to; index += Float.BYTES) {
					builder.append((float)(FLOAT_ACCESS.get(base, index)));
					if (index + 1 != to) builder.append(", ");
				}
				yield builder.append(']').toString();
			}
			case DOUBLE_TYPE -> {
				builder = new StringBuilder(elementCount * 22).append('[');
				from = this.intIndexUnchecked(0);
				to = this.intIndexUnchecked(elementCount);
				for (index = from; index < to; index += Double.BYTES) {
					builder.append((double)(DOUBLE_ACCESS.get(base, index)));
					if (index + 1 != to) builder.append(", ");
				}
				yield builder.append(']').toString();
			}
			case BOOLEAN_TYPE -> {
				builder = new StringBuilder(elementCount * 7).append('[');
				for (index = 0; index < elementCount; index++) {
					int offsetIndex = index + this.elementOffset;
					builder.append(((base[(offsetIndex >>> 3) + this.byteOffset] >>> (offsetIndex & 7)) & 1) != 0);
					if (index + 1 != elementCount) builder.append(", ");
				}
				yield builder.append(']').toString();
			}
			default -> {
				yield "NumberArray: (invalid type: " + this.type + ')';
			}
		};
	}

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
		public static final int FIRST_ALIGNED_INDEX;
		static {
			FIRST_ALIGNED_INDEX = -ByteBuffer.wrap(new byte[0]).alignmentOffset(0, Long.BYTES) & (Long.BYTES - 1);
		}

		public static final ThreadLocal<Manager> INSTANCES = ThreadLocal.withInitial(Manager::new);

		/** the beginning of the region of memory this Manager keeps track of. */
		public byte[] base;
		/** the number of bytes used in our region of memory. */
		public int used;

		public Manager() {
			this(MIN_SIZE);
		}

		public Manager(int size) {
			if (size < 0) throw new IllegalArgumentException("Initial capacity must be non-negative: " + size);
			size += (-size) & 7;
			this.base = new byte[size + FIRST_ALIGNED_INDEX];
			this.used = FIRST_ALIGNED_INDEX;
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
			bytes += -bytes & 7;
			this.ensureCapacity(this.used + bytes);
			return bytes;
		}

		public int checkUnusedAndGetRemaining() {
			if (this.used != FIRST_ALIGNED_INDEX) {
				throw new IllegalStateException("Multiple heap allocations from the same Manager");
			}
			return this.base.length - FIRST_ALIGNED_INDEX;
		}

		public static int rshiftExact(int bytes, int shift) {
			assert bytes >= FIRST_ALIGNED_INDEX;
			int result = bytes >>> shift;
			if (result << shift != bytes) {
				throw new IllegalStateException("Incorrect tail alignment on Manager: " + bytes + " not divisible by " + (1 << shift));
			}
			return result;
		}

		public NumberArray allocateBytesHeap() {
			int remainingBytes = this.checkUnusedAndGetRemaining();
			return new NumberArray(BYTE_TYPE, this, FIRST_ALIGNED_INDEX, remainingBytes, 0, rshiftExact(remainingBytes, BYTE_SHIFT), true);
		}

		public NumberArray allocateShortsHeap() {
			int remainingBytes = this.checkUnusedAndGetRemaining();
			return new NumberArray(SHORT_TYPE, this, FIRST_ALIGNED_INDEX, remainingBytes, 0, rshiftExact(remainingBytes, SHORT_SHIFT), true);
		}

		public NumberArray allocateIntsHeap() {
			int remainingBytes = this.checkUnusedAndGetRemaining();
			return new NumberArray(INT_TYPE, this, FIRST_ALIGNED_INDEX, remainingBytes, 0, rshiftExact(remainingBytes, INT_SHIFT), true);
		}

		public NumberArray allocateLongsHeap() {
			int remainingBytes = this.checkUnusedAndGetRemaining();
			return new NumberArray(LONG_TYPE, this, FIRST_ALIGNED_INDEX, remainingBytes, 0, rshiftExact(remainingBytes, LONG_SHIFT), true);
		}

		public NumberArray allocateFloatsHeap() {
			int remainingBytes = this.checkUnusedAndGetRemaining();
			return new NumberArray(FLOAT_TYPE, this, FIRST_ALIGNED_INDEX, remainingBytes, 0, rshiftExact(remainingBytes, FLOAT_SHIFT), true);
		}

		public NumberArray allocateDoublesHeap() {
			int remainingBytes = this.checkUnusedAndGetRemaining();
			return new NumberArray(DOUBLE_TYPE, this, FIRST_ALIGNED_INDEX, remainingBytes, 0, rshiftExact(remainingBytes, DOUBLE_SHIFT), true);
		}

		public NumberArray allocateBooleansHeap(int exactNumber) {
			if (exactNumber < 0) {
				throw new IllegalArgumentException("Attempt to allocate negative booleans");
			}
			int remainingBytes = this.checkUnusedAndGetRemaining();
			return new NumberArray(BOOLEAN_TYPE, this, FIRST_ALIGNED_INDEX, remainingBytes, 0, exactNumber, true);
		}

		public NumberArray allocateBytesDirect(int bytes) {
			return new NumberArray(BYTE_TYPE, this, this.used, this.beforeAllocate(bytes << BYTE_SHIFT), 0, bytes, true);
		}

		public NumberArray allocateShortsDirect(int shorts) {
			return new NumberArray(SHORT_TYPE, this, this.used, this.beforeAllocate(shorts << SHORT_SHIFT), 0, shorts, true);
		}

		public NumberArray allocateIntsDirect(int ints) {
			return new NumberArray(INT_TYPE, this, this.used, this.beforeAllocate(ints << INT_SHIFT), 0, ints, true);
		}

		public NumberArray allocateLongsDirect(int longs) {
			return new NumberArray(LONG_TYPE, this, this.used, this.beforeAllocate(longs << LONG_SHIFT), 0, longs, true);
		}

		public NumberArray allocateFloatsDirect(int floats) {
			return new NumberArray(FLOAT_TYPE, this, this.used, this.beforeAllocate(floats << FLOAT_SHIFT), 0, floats, true);
		}

		public NumberArray allocateDoublesDirect(int doubles) {
			return new NumberArray(DOUBLE_TYPE, this, this.used, this.beforeAllocate(doubles << DOUBLE_SHIFT), 0, doubles, true);
		}

		public NumberArray allocateBooleansDirect(int booleans) {
			if (booleans < 0) {
				throw new IllegalArgumentException("Attempt to allocate negative booleans: " + booleans);
			}
			return new NumberArray(BOOLEAN_TYPE, this, this.used, this.beforeAllocate(((booleans - 1) >> 3) + 1), 0, booleans, true);
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