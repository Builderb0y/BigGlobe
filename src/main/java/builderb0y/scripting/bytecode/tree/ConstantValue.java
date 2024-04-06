package builderb0y.scripting.bytecode.tree;

import java.util.Arrays;
import java.util.Objects;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;

import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ConstantValue extends Typeable, BytecodeEmitter {

	public static final ObjectArrayFactory<ConstantValue> ARRAY_FACTORY = new ObjectArrayFactory<>(ConstantValue.class);

	public static ConstantValue notConstant() {
		return NonConstantValue.INSTANCE;
	}

	public static ConstantValue of(byte     value) { return new    IntConstantValue(value); }
	public static ConstantValue of(short    value) { return new    IntConstantValue(value); }
	public static ConstantValue of(int      value) { return new    IntConstantValue(value); }
	public static ConstantValue of(long     value) { return new   LongConstantValue(value); }
	public static ConstantValue of(float    value) { return new  FloatConstantValue(value); }
	public static ConstantValue of(double   value) { return new DoubleConstantValue(value); }
	public static ConstantValue of(char     value) { return new    IntConstantValue(value); }
	public static ConstantValue of(boolean  value) { return new    IntConstantValue(value); }
	public static ConstantValue of(String   value) { return value == null ? new NullConstantValue(TypeInfos.STRING) : new StringConstantValue(value); }
	public static ConstantValue of(TypeInfo value) { return new  ClassConstantValue(Objects.requireNonNull(value, "Attempt to LDC null.class")); }
	public static ConstantValue ofNull(TypeInfo type) { return type.isPrimitive() ? constantAbsent(type) : new NullConstantValue(type); }
	public static ConstantValue ofManual(Object object, TypeInfo type) { return object == null ? ofNull(type) : new ManualConstantValue(object, type); }

	public static ConstantValue of(Object object, TypeInfo type) {
		if (object == null) {
			return ofNull(type);
		}
		else if (object instanceof String s && type.equals(TypeInfos.STRING)) {
			return of(s);
		}
		else if (object instanceof TypeInfo t && type.equals(TypeInfos.CLASS)) {
			return of(t);
		}
		else if (object instanceof Character c) {
			object = Integer.valueOf(c.charValue());
		}
		if (object instanceof Number number && type.isNumber()) {
			return switch (type.getSort()) {
				case BYTE    -> of(number.byteValue());
				case SHORT   -> of(number.shortValue());
				case INT     -> of(number.intValue());
				case LONG    -> of(number.longValue());
				case FLOAT   -> of(number.floatValue());
				case DOUBLE  -> of(number.doubleValue());
				case CHAR    -> of((char)(number.intValue()));
				case VOID, OBJECT, ARRAY, BOOLEAN -> throw new IllegalArgumentException(type.toString());
			};
		}
		else if (object instanceof Boolean bool && type.getSort() == Sort.BOOLEAN) {
			return of(bool.booleanValue());
		}
		else {
			if (TypeInfo.of(object.getClass()).extendsOrImplements(type)) {
				return new ManualConstantValue(object, type);
			}
			else {
				throw new ClassCastException("Cannot create constant of type " + type + " from " + object);
			}
		}
	}

	public static ConstantValue dynamic(MethodInfo bootstrapMethod, ConstantValue... bootstrapArgs) {
		return new DynamicConstantValue(bootstrapMethod.returnType, bootstrapMethod, bootstrapArgs);
	}

	public static ConstantValue dynamic(TypeInfo type, MethodInfo bootstrapMethod, ConstantValue... bootstrapArgs) {
		return new DynamicConstantValue(type, bootstrapMethod, bootstrapArgs);
	}

	public abstract byte    asByte   ();
	public abstract short   asShort  ();
	public abstract int     asInt    ();
	public abstract long    asLong   ();
	public abstract float   asFloat  ();
	public abstract double  asDouble ();
	public abstract char    asChar   ();
	public abstract boolean asBoolean();
	@Override
	public abstract TypeInfo getTypeInfo();

	public default Number asNumber() {
		return switch (this.getTypeInfo().getSort()) {
			case BYTE    -> Byte     .valueOf(this.asByte   ());
			case SHORT   -> Short    .valueOf(this.asShort  ());
			case INT     -> Integer  .valueOf(this.asInt    ());
			case LONG    -> Long     .valueOf(this.asLong   ());
			case FLOAT   -> Float    .valueOf(this.asFloat  ());
			case DOUBLE  -> Double   .valueOf(this.asDouble ());
			case CHAR    -> Integer  .valueOf(this.asChar   ());
			case BOOLEAN -> Byte     .valueOf(this.asBoolean() ? ((byte)(1)) : ((byte)(0)));
			case OBJECT, ARRAY, VOID -> throw new IllegalStateException(this.getTypeInfo().toString());
		};
	}

	public default Object asJavaObject() {
		return switch (this.getTypeInfo().getSort()) {
			case BYTE    -> Byte     .valueOf(this.asByte   ());
			case SHORT   -> Short    .valueOf(this.asShort  ());
			case INT     -> Integer  .valueOf(this.asInt    ());
			case LONG    -> Long     .valueOf(this.asLong   ());
			case FLOAT   -> Float    .valueOf(this.asFloat  ());
			case DOUBLE  -> Double   .valueOf(this.asDouble ());
			case CHAR    -> Character.valueOf(this.asChar   ());
			case BOOLEAN -> Boolean  .valueOf(this.asBoolean());
			case OBJECT, ARRAY, VOID -> throw new IllegalStateException(this.getTypeInfo().toString());
		};
	}

	public default Object asAsmObject() {
		return switch (this.getTypeInfo().getSort()) {
			case BYTE, SHORT, INT, CHAR, BOOLEAN -> Integer.valueOf(this.asInt());
			case LONG   -> Long  .valueOf(this.asLong  ());
			case FLOAT  -> Float .valueOf(this.asFloat ());
			case DOUBLE -> Double.valueOf(this.asDouble());
			case OBJECT, ARRAY, VOID -> throw new IllegalStateException(this.getTypeInfo().toString());
		};
	}

	public default boolean isConstant() {
		return true;
	}

	public default boolean isConstantOrDynamic() {
		return this.isConstant();
	}

	@Override
	public abstract void emitBytecode(MethodCompileContext method);

	public static class NonConstantValue implements ConstantValue {

		public static final NonConstantValue INSTANCE = new NonConstantValue();

		@Override public byte asByte() { throw new UnsupportedOperationException(); }
		@Override public short asShort() { throw new UnsupportedOperationException(); }
		@Override public int asInt() { throw new UnsupportedOperationException(); }
		@Override public long asLong() { throw new UnsupportedOperationException(); }
		@Override public float asFloat() { throw new UnsupportedOperationException(); }
		@Override public double asDouble() { throw new UnsupportedOperationException(); }
		@Override public char asChar() { throw new UnsupportedOperationException(); }
		@Override public boolean asBoolean() { throw new UnsupportedOperationException(); }
		@Override public TypeInfo getTypeInfo() { throw new UnsupportedOperationException(); }
		@Override public Number asNumber() { throw new UnsupportedOperationException(); }
		@Override public Object asJavaObject() { throw new UnsupportedOperationException(); }
		@Override public Object asAsmObject() { throw new UnsupportedOperationException(); }
		@Override public void emitBytecode(MethodCompileContext method) { throw new UnsupportedOperationException(); }

		@Override
		public boolean isConstant() {
			return false;
		}

		@Override
		public String toString() {
			return "NonConstantValue";
		}
	}

	public static class IntConstantValue implements ConstantValue {

		public final int value;
		public final TypeInfo type; //may be BOOLEAN, BYTE, CHAR, SHORT, or INT.

		public IntConstantValue(int value, TypeInfo type) {
			this.value = value;
			this.type = type;
		}

		public IntConstantValue(boolean value) { this(value ? 1 : 0, TypeInfos.BOOLEAN); }
		public IntConstantValue(byte value) { this(value, TypeInfos.BYTE); }
		public IntConstantValue(char value) { this(value, TypeInfos.CHAR); }
		public IntConstantValue(short value) { this(value, TypeInfos.SHORT); }
		public IntConstantValue(int value) { this(value, TypeInfos.INT); }

		@Override public byte asByte() { return toByte(this.value); }
		@Override public short asShort() { return toShort(this.value); }
		@Override public int asInt() { return this.value; }
		@Override public long asLong() { return this.value; }
		@Override public float asFloat() { return this.value; }
		@Override public double asDouble() { return this.value; }
		@Override public char asChar() { return toChar(this.value); }
		@Override public boolean asBoolean() { return this.value != 0; }

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			int intValue = this.value;
			if (intValue >= -1 && intValue <= 5) {
				method.node.visitInsn(intValue - -1 + ICONST_M1);
			}
			else if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
				method.node.visitIntInsn(BIPUSH, intValue);
			}
			else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
				method.node.visitIntInsn(SIPUSH, intValue);
			}
			else {
				method.node.visitLdcInsn(intValue);
			}
		}

		@Override
		public String toString() {
			return this.value + " of type " + this.getTypeInfo().getClassName();
		}
	}

	public static class LongConstantValue implements ConstantValue {

		public final long value;

		public LongConstantValue(long value) {
			this.value = value;
		}

		@Override public byte asByte() { return toByte(toInt(this.value)); }
		@Override public short asShort() { return toShort(toInt(this.value)); }
		@Override public int asInt() { return toInt(this.value); }
		@Override public long asLong() { return this.value; }
		@Override public float asFloat() { return this.value; }
		@Override public double asDouble() { return this.value; }
		@Override public char asChar() { return toChar(toInt(this.value)); }
		@Override public boolean asBoolean() { return this.value != 0L; }

		@Override
		public TypeInfo getTypeInfo() {
			return TypeInfos.LONG;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			long longValue = this.value;
			if (longValue == 0L) {
				method.node.visitInsn(LCONST_0);
			}
			else if (longValue == 1L) {
				method.node.visitInsn(LCONST_1);
			}
			else {
				method.node.visitLdcInsn(longValue);
			}
		}

		@Override
		public String toString() {
			return this.value + " of type " + this.getTypeInfo().getClassName();
		}
	}

	public static class FloatConstantValue implements ConstantValue {

		public final float value;

		public FloatConstantValue(float value) {
			this.value = value;
		}

		@Override public byte asByte() { return toByte(toInt(this.value)); }
		@Override public short asShort() { return toShort(toInt(this.value)); }
		@Override public int asInt() { return toInt(this.value); }
		@Override public long asLong() { return toLong(this.value); }
		@Override public float asFloat() { return this.value; }
		@Override public double asDouble() { return this.value; }
		@Override public char asChar() { return toChar(toInt(this.value)); }
		@Override public boolean asBoolean() { return CastingSupport.F2Z(this.value); }

		@Override
		public TypeInfo getTypeInfo() {
			return TypeInfos.FLOAT;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			float floatValue = this.value;
			if (Float.floatToRawIntBits(floatValue) == 0) {
				method.node.visitInsn(FCONST_0);
			}
			else if (floatValue == 1.0F) {
				method.node.visitInsn(FCONST_1);
			}
			else if (floatValue == 2.0F) {
				method.node.visitInsn(FCONST_2);
			}
			else {
				method.node.visitLdcInsn(floatValue);
			}
		}

		@Override
		public String toString() {
			return this.value + " of type " + this.getTypeInfo().getClassName();
		}
	}

	public static class DoubleConstantValue implements ConstantValue {

		public final double value;

		public DoubleConstantValue(double value) {
			this.value = value;
		}

		@Override public byte asByte() { return toByte(toInt(this.value)); }
		@Override public short asShort() { return toShort(toInt(this.value)); }
		@Override public int asInt() { return toInt(this.value); }
		@Override public long asLong() { return toLong(this.value); }
		@Override public float asFloat() { return (float)(this.value); }
		@Override public double asDouble() { return this.value; }
		@Override public char asChar() { return toChar(toInt(this.value)); }
		@Override public boolean asBoolean() { return CastingSupport.D2Z(this.value); }

		@Override
		public TypeInfo getTypeInfo() {
			return TypeInfos.DOUBLE;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			double doubleValue = this.value;
			if (Double.doubleToRawLongBits(doubleValue) == 0L) {
				method.node.visitInsn(DCONST_0);
			}
			else if (doubleValue == 1.0D) {
				method.node.visitInsn(DCONST_1);
			}
			else {
				method.node.visitLdcInsn(doubleValue);
			}
		}

		@Override
		public String toString() {
			return this.value + " of type " + this.getTypeInfo().getClassName();
		}
	}

	public static class NullConstantValue implements ConstantValue {

		public final TypeInfo type;

		public NullConstantValue(TypeInfo type) {
			this.type = type;
		}

		@Override public byte asByte() { return 0; }
		@Override public short asShort() { return 0; }
		@Override public int asInt() { return 0; }
		@Override public long asLong() { return 0; }
		@Override public float asFloat() { return 0; }
		@Override public double asDouble() { return 0; }
		@Override public char asChar() { return 0; }
		@Override public boolean asBoolean() { return false; }

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public Number asNumber() {
			return Integer.valueOf(0);
		}

		@Override
		public Object asJavaObject() {
			return null;
		}

		@Override
		public Object asAsmObject() {
			throw new UnsupportedOperationException("Must use ACONST_NULL, not LDC NULL.");
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			method.node.visitInsn(ACONST_NULL);
		}

		@Override
		public String toString() {
			return "null of type " + this.getTypeInfo().getClassName();
		}
	}

	public static class StringConstantValue implements ConstantValue {

		public final String value;

		public StringConstantValue(String value) {
			this.value = value;
		}

		@Override public byte asByte() { throw new ClassCastException("Not a number (is a String)"); }
		@Override public short asShort() { throw new ClassCastException("Not a number (is a String)"); }
		@Override public int asInt() { throw new ClassCastException("Not a number (is a String)"); }
		@Override public long asLong() { throw new ClassCastException("Not a number (is a String)"); }
		@Override public float asFloat() { throw new ClassCastException("Not a number (is a String)"); }
		@Override public double asDouble() { throw new ClassCastException("Not a number (is a String)"); }
		@Override public char asChar() { throw new ClassCastException("Not a number (is a String)"); }
		@Override public boolean asBoolean() { throw new ClassCastException("Not a number (is a String)"); }
		@Override public Number asNumber() { throw new ClassCastException("Not a number (is a String)"); }

		@Override
		public TypeInfo getTypeInfo() {
			return TypeInfos.STRING;
		}

		@Override
		public Object asJavaObject() {
			return this.value;
		}

		@Override
		public Object asAsmObject() {
			return this.value;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			method.node.visitLdcInsn(this.value);
		}

		@Override
		public String toString() {
			return '"' + this.value + "\" of type " + this.getTypeInfo().getClassName();
		}
	}

	public static class ClassConstantValue implements ConstantValue {

		public final TypeInfo value;

		public ClassConstantValue(TypeInfo value) {
			this.value = value;
		}

		@Override public byte asByte() { throw new ClassCastException("Not a number (is a Class)"); }
		@Override public short asShort() { throw new ClassCastException("Not a number (is a Class)"); }
		@Override public int asInt() { throw new ClassCastException("Not a number (is a Class)"); }
		@Override public long asLong() { throw new ClassCastException("Not a number (is a Class)"); }
		@Override public float asFloat() { throw new ClassCastException("Not a number (is a Class)"); }
		@Override public double asDouble() { throw new ClassCastException("Not a number (is a Class)"); }
		@Override public char asChar() { throw new ClassCastException("Not a number (is a Class)"); }
		@Override public boolean asBoolean() { throw new ClassCastException("Not a number (is a Class)"); }
		@Override public Number asNumber() { throw new ClassCastException("Not a number (is a Class)"); }

		@Override
		public TypeInfo getTypeInfo() {
			return TypeInfos.CLASS;
		}

		@Override
		public Object asJavaObject() {
			return this.value;
		}

		@Override
		public Object asAsmObject() {
			return this.value.toAsmType();
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			method.node.visitLdcInsn(this.asAsmObject());
		}

		@Override
		public String toString() {
			return this.value.getClassName();
		}
	}

	public static class DynamicConstantValue extends NonConstantValue {

		public final TypeInfo type;
		public final MethodInfo bootstrapMethod;
		public final ConstantValue[] bootstrapArgs;
		public final ConstantDynamic dynamic;

		public DynamicConstantValue(TypeInfo type, MethodInfo bootstrapMethod, ConstantValue... bootstrapArgs) {
			this.type = type;
			this.bootstrapMethod = bootstrapMethod;
			this.bootstrapArgs = bootstrapArgs;
			this.dynamic = new ConstantDynamic(
				bootstrapMethod.name,
				type.getDescriptor(),
				new Handle(
					H_INVOKESTATIC,
					bootstrapMethod.owner.getInternalName(),
					bootstrapMethod.name,
					bootstrapMethod.getDescriptor(),
					bootstrapMethod.isInterface()
				),
				Arrays.stream(bootstrapArgs)
				.map(ConstantValue::asAsmObject)
				.toArray()
			);
		}

		@Override
		public Object asAsmObject() {
			return this.dynamic;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			method.node.visitLdcInsn(this.dynamic);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public boolean isConstantOrDynamic() {
			return true;
		}

		@Override
		public String toString() {
			return this.dynamic.toString();
		}
	}

	public static class ManualConstantValue extends NonConstantValue {

		public final Object value;
		public final TypeInfo type;

		public ManualConstantValue(Object value, TypeInfo type) {
			this.value = value;
			this.type = type;
		}

		@Override
		public Object asJavaObject() {
			return this.value;
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.type;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			method.clazz.newConstant(this.value, this.type).emitBytecode(method);
		}

		@Override
		public boolean isConstantOrDynamic() {
			return true;
		}

		@Override
		public String toString() {
			return this.value + " of type " + this.type;
		}
	}

	public static byte toByte(int value) {
		byte b = (byte)(value);
		if (value == b) return b;
		else throw new ClassCastException("Value is outside of byte range: " + value);
	}

	public static short toShort(int value) {
		short s = (short)(value);
		if (value == s) return s;
		else throw new ClassCastException("Value is outside of short range: " + value);
	}

	public static char toChar(int value) {
		char c = (char)(value);
		if (value == c) return c;
		else throw new ClassCastException("Value is outside of char range: " + value);
	}

	public static int toInt(long value) {
		int i = (int)(value);
		if (value == i) return i;
		else throw new ClassCastException("Value is outside of int range: " + value);
	}

	public static int toInt(float value) {
		int i = (int)(value);
		if (value == i) return i;
		else throw new ClassCastException("Value is outside of int range: " + value);
	}

	public static int toInt(double value) {
		int i = (int)(value);
		if (value == i) return i;
		else throw new ClassCastException("Value is outside of int range: " + value);
	}

	public static long toLong(float value) {
		long i = (long)(value);
		if (value == i) return i;
		else throw new ClassCastException("Value is outside of int range: " + value);
	}

	public static long toLong(double value) {
		long i = (long)(value);
		if (value == i) return i;
		else throw new ClassCastException("Value is outside of int range: " + value);
	}
}