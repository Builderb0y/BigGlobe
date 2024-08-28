package builderb0y.bigglobe.scripting.wrappers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.InfoHolder;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ExternalData {

	public static final AbstractConstantFactory CONSTANT_FACTORY = new AbstractConstantFactory(TypeInfos.STRING, TypeInfo.of(ExternalData.class)) {

		public final MethodInfo constantMethod = MethodInfo.getMethod(ExternalData.class, "of");

		@Override
		public InsnTree createConstant(ConstantValue constant) {
			return ldc(this.constantMethod, constant);
		}

		@Override
		public InsnTree createNonConstant(InsnTree tree) {
			throw new UnsupportedOperationException("ExternalData path must be a constant value");
		}
	};

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo
			stride,
			offset;

		public MethodInfo
			withStride,
			withOffset,
			withFormat,
			getByte,
			getShort,
			getChar,
			getInt,
			getLong,
			getFloat,
			getDouble;
	}

	public static final MutableScriptEnvironment ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addType("ExternalData", INFO.type)
		.addCastConstant(CONSTANT_FACTORY, true)
		.addFieldGet(INFO.stride)
		.addFieldGet(INFO.offset)
		.addMethodInvoke("stride", INFO.withStride)
		.addMethodInvoke("offset", INFO.withOffset)
		.addMethodInvoke("format", INFO.withFormat)
		.addMethodInvoke(INFO.getByte)
		.addMethodInvoke(INFO.getShort)
		.addMethodInvoke(INFO.getChar)
		.addMethodInvoke(INFO.getInt)
		.addMethodInvoke(INFO.getLong)
		.addMethodInvoke(INFO.getFloat)
		.addMethodInvoke(INFO.getDouble)
	);

	public static class WeakByteArray extends WeakReference<byte[]> {

		public static final ReferenceQueue<byte[]> QUEUE = new ReferenceQueue<>();

		public final int hashCode;

		public WeakByteArray(byte[] array) {
			super(array, QUEUE);
			this.hashCode = Arrays.hashCode(array);
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof WeakByteArray that && Arrays.equals(this.get(), that.get());
		}
	}

	public static final ObjectOpenHashSet<WeakByteArray> INTERNER = new ObjectOpenHashSet<>(16) {

		@Override
		public WeakByteArray addOrGet(WeakByteArray array) {
			WeakByteArray result = super.addOrGet(array);
			for (WeakByteArray toRemove; (toRemove = (WeakByteArray)(WeakByteArray.QUEUE.poll())) != null;) {
				this.remove(toRemove);
			}
			return result;
		}
	};

	public static final VarHandle
		BYTE_ACCESS   = MethodHandles.arrayElementVarHandle (byte  [].class).withInvokeExactBehavior(),
		SHORT_ACCESS  = MethodHandles.byteArrayViewVarHandle(short [].class, ByteOrder.BIG_ENDIAN).withInvokeExactBehavior(),
		CHAR_ACCESS   = MethodHandles.byteArrayViewVarHandle(char  [].class, ByteOrder.BIG_ENDIAN).withInvokeExactBehavior(),
		INT_ACCESS    = MethodHandles.byteArrayViewVarHandle(int   [].class, ByteOrder.BIG_ENDIAN).withInvokeExactBehavior(),
		LONG_ACCESS   = MethodHandles.byteArrayViewVarHandle(long  [].class, ByteOrder.BIG_ENDIAN).withInvokeExactBehavior(),
		FLOAT_ACCESS  = MethodHandles.byteArrayViewVarHandle(float [].class, ByteOrder.BIG_ENDIAN).withInvokeExactBehavior(),
		DOUBLE_ACCESS = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN).withInvokeExactBehavior();

	public static final Reference2ByteOpenHashMap<Class<?>> STANDARD_STRIDES = new Reference2ByteOpenHashMap<>();
	static {
		STANDARD_STRIDES.defaultReturnValue((byte)(-1));
		STANDARD_STRIDES.put(byte  .class, (byte)(1));
		STANDARD_STRIDES.put(short .class, (byte)(2));
		STANDARD_STRIDES.put(char  .class, (byte)(2));
		STANDARD_STRIDES.put(int   .class, (byte)(4));
		STANDARD_STRIDES.put(float .class, (byte)(4));
		STANDARD_STRIDES.put(long  .class, (byte)(8));
		STANDARD_STRIDES.put(double.class, (byte)(8));
	}

	public final byte[] data;
	public final int stride, offset;

	public ExternalData(byte[] data) {
		this.data = Objects.requireNonNullElse(INTERNER.addOrGet(new WeakByteArray(data)).get(), data);
		this.stride = 1;
		this.offset = 0;
	}

	public ExternalData(byte[] data, int stride, int offset) {
		this.data = data;
		this.stride = stride;
		this.offset = offset;
	}

	public static ExternalData of(MethodHandles.Lookup caller, String name, Class<?> type, String id) throws IOException {
		Identifier identifier = Identifier.of(id);
		identifier = Identifier.of(identifier.getNamespace(), "bigglobe_external/" + identifier.getPath() + ".dat");
		try (InputStream stream = BigGlobeMod.getResourceFactory().open(identifier)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
			stream.transferTo(baos);
			return new ExternalData(baos.toByteArray());
		}
	}

	public ExternalData withStride(int stride) {
		if (stride > 0) return new ExternalData(this.data, stride, this.offset);
		else throw new IllegalArgumentException("Stride must be greater than 0, but it was " + stride);
	}

	public ExternalData withOffset(int offset) {
		if (offset >= 0) return new ExternalData(this.data, this.stride, offset);
		else throw new IllegalArgumentException("Offset must be greater than or equal to 0, but it was " + offset);
	}

	public ExternalData withFormat(Class<?> componentType) {
		int stride = STANDARD_STRIDES.getByte(componentType);
		if (stride > 0) return new ExternalData(this.data, stride, this.offset);
		else throw new IllegalArgumentException("Format must be one of: byte, short, char, int, long, float, or double, but it was " + componentType);
	}

	public byte getByte(int index) {
		return (byte)(BYTE_ACCESS.get(this.data, index * this.stride + this.offset));
	}

	public short getShort(int index) {
		return (short)(SHORT_ACCESS.get(this.data, index * this.stride + this.offset));
	}

	public char getChar(int index) {
		return (char)(CHAR_ACCESS.get(this.data, index * this.stride + this.offset));
	}

	public int getInt(int index) {
		return (int)(INT_ACCESS.get(this.data, index * this.stride + this.offset));
	}

	public long getLong(int index) {
		return (long)(LONG_ACCESS.get(this.data, index * this.stride + this.offset));
	}

	public float getFloat(int index) {
		return (float)(FLOAT_ACCESS.get(this.data, index * this.stride + this.offset));
	}

	public double getDouble(int index) {
		return (double)(DOUBLE_ACCESS.get(this.data, index * this.stride + this.offset));
	}
}