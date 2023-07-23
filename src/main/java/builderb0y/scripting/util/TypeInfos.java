package builderb0y.scripting.util;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;

public class TypeInfos {

	public static final @NotNull TypeInfo
		OBJECT          = TypeInfo.of(Object    .class),
		ENUM            = TypeInfo.of(Enum      .class),
		RECORD          = TypeInfo.of(Record    .class),
		STRING          = TypeInfo.of(String    .class),
		THROWABLE       = TypeInfo.of(Throwable .class),
		NUMBER          = TypeInfo.of(Number    .class),
		COMPARABLE      = TypeInfo.of(Comparable.class),
		CLASS           = TypeInfo.of(Class     .class),
		ITERABLE        = TypeInfo.of(Iterable  .class),
		ITERATOR        = TypeInfo.of(Iterator  .class),
		MAP             = TypeInfo.of(Map       .class),
		VOID            = TypeInfo.of(void      .class),
		BOOLEAN         = TypeInfo.of(boolean   .class),
		BYTE            = TypeInfo.of(byte      .class),
		CHAR            = TypeInfo.of(char      .class),
		SHORT           = TypeInfo.of(short     .class),
		INT             = TypeInfo.of(int       .class),
		LONG            = TypeInfo.of(long      .class),
		FLOAT           = TypeInfo.of(float     .class),
		DOUBLE          = TypeInfo.of(double    .class),
		VOID_WRAPPER    = TypeInfo.of(Void      .class),
		BOOLEAN_WRAPPER = TypeInfo.of(Boolean   .class),
		BYTE_WRAPPER    = TypeInfo.of(Byte      .class),
		CHAR_WRAPPER    = TypeInfo.of(Character .class),
		SHORT_WRAPPER   = TypeInfo.of(Short     .class),
		INT_WRAPPER     = TypeInfo.of(Integer   .class),
		LONG_WRAPPER    = TypeInfo.of(Long      .class),
		FLOAT_WRAPPER   = TypeInfo.of(Float     .class),
		DOUBLE_WRAPPER  = TypeInfo.of(Double    .class);

	public static void checkNumber(TypeInfo type) {
		if (!type.isNumber()) {
			throw new InvalidOperandException("Expected number type, got " + type);
		}
	}

	public static TypeInfo widenUntilSame(TypeInfo first, TypeInfo second) {
		checkNumber(first);
		checkNumber(second);
		return Sort.VALUES[Math.max(first.getSort().ordinal(), second.getSort().ordinal())].canonicalInstance;
	}

	public static TypeInfo widenUntilSame(Stream<TypeInfo> stream) {
		return Sort.VALUES[stream.peek(TypeInfos::checkNumber).mapToInt(type -> type.getSort().ordinal()).max().orElseThrow()].canonicalInstance;
	}

	public static TypeInfo widenUntilSameInt(TypeInfo first, TypeInfo second) {
		checkNumber(first);
		checkNumber(second);
		return Sort.VALUES[Math.max(Math.max(first.getSort().ordinal(), second.getSort().ordinal()), Sort.INT.ordinal())].canonicalInstance;
	}

	public static TypeInfo widenUntilSameInt(Stream<TypeInfo> stream) {
		return Sort.VALUES[Math.max(stream.peek(TypeInfos::checkNumber).mapToInt(type -> type.getSort().ordinal()).max().orElseThrow(), Sort.INT.ordinal())].canonicalInstance;
	}

	public static TypeInfo widenToInt(TypeInfo type) {
		checkNumber(type);
		return Sort.VALUES[Math.max(type.getSort().ordinal(), Sort.INT.ordinal())].canonicalInstance;
	}

	public static final Map<TypeInfo, TypeInfo> BOXING = Map.of(
		VOID,    VOID_WRAPPER,
		BOOLEAN, BOOLEAN_WRAPPER,
		BYTE,    BYTE_WRAPPER,
		CHAR,    CHAR_WRAPPER,
		SHORT,   SHORT_WRAPPER,
		INT,     INT_WRAPPER,
		LONG,    LONG_WRAPPER,
		FLOAT,   FLOAT_WRAPPER,
		DOUBLE,  DOUBLE_WRAPPER
	);

	public static final Map<TypeInfo, TypeInfo> UNBOXING = Map.of(
		VOID_WRAPPER,    VOID,
		BOOLEAN_WRAPPER, BOOLEAN,
		BYTE_WRAPPER,    BYTE,
		CHAR_WRAPPER,    CHAR,
		SHORT_WRAPPER,   SHORT,
		INT_WRAPPER,     INT,
		LONG_WRAPPER,    LONG,
		FLOAT_WRAPPER,   FLOAT,
		DOUBLE_WRAPPER,  DOUBLE
	);

	public static TypeInfo box(TypeInfo info) {
		return BOXING.getOrDefault(info, info);
	}

	public static TypeInfo unbox(TypeInfo info) {
		return UNBOXING.getOrDefault(info, info);
	}
}