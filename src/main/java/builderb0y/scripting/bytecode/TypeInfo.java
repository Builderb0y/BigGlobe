package builderb0y.scripting.bytecode;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.scripting.util.ArrayBuilder;
import builderb0y.scripting.util.CollectionTransformer;
import builderb0y.scripting.util.TypeInfos;

@SuppressWarnings("deprecation")
public class TypeInfo {

	public static final ClassValue<TypeInfo> CACHE = new ClassValue<>() {

		@Override
		protected TypeInfo computeValue(Class<?> type) {
			return convert(type);
		}
	};
	public static final ObjectArrayFactory<TypeInfo> ARRAY_FACTORY = new ObjectArrayFactory<>(TypeInfo.class);
	public static final TypeInfo[]
		ANNOTATION_INTERFACES = allOf(Annotation.class),
		ARRAY_INTERFACES      = allOf(Cloneable.class, Serializable.class);

	public final @NotNull ClassType type;
	@Deprecated //may eventually replace with a different abstraction.
	public final @NotNull Type name;
	public final @Nullable TypeInfo superClass;
	public final @NotNull TypeInfo @NotNull [] superInterfaces;
	public final @Nullable TypeInfo componentType;
	public final boolean isGeneric;

	public Set<TypeInfo> allAssignableTypes, allCastableTypes;

	public TypeInfo(
		@NotNull ClassType type,
		@NotNull Type name,
		@Nullable TypeInfo superClass,
		@NotNull TypeInfo @NotNull [] superInterfaces,
		@Nullable TypeInfo componentType,
		boolean isGeneric
	) {
		this.type = type;
		this.name = name;
		this.superClass = superClass;
		this.superInterfaces = superInterfaces;
		this.componentType = componentType;
		this.isGeneric = isGeneric;
	}

	@Contract("!null -> !null")
	public static TypeInfo of(@Nullable Class<?> clazz) {
		return clazz == null ? null : CACHE.get(clazz);
	}

	public static TypeInfo[] allOf(@NotNull Class<?> @NotNull ... classes) {
		return CollectionTransformer.convertArray(classes, ARRAY_FACTORY, TypeInfo::of);
	}

	public static TypeInfo[] allOf(java.lang.reflect.@NotNull Type @NotNull ... types) {
		return CollectionTransformer.convertArray(types, ARRAY_FACTORY, TypeInfo::of);
	}

	public static record ParsedTypeInfo(TypeInfo type, int endIndex) {}

	public static ParsedTypeInfo parseNext(CharSequence input, int start) {
		char first = input.charAt(start);
		return switch (first) {
			case 'B' -> new ParsedTypeInfo(TypeInfos.BYTE,    start + 1);
			case 'S' -> new ParsedTypeInfo(TypeInfos.SHORT,   start + 1);
			case 'I' -> new ParsedTypeInfo(TypeInfos.INT,     start + 1);
			case 'J' -> new ParsedTypeInfo(TypeInfos.LONG,    start + 1);
			case 'F' -> new ParsedTypeInfo(TypeInfos.FLOAT,   start + 1);
			case 'D' -> new ParsedTypeInfo(TypeInfos.DOUBLE,  start + 1);
			case 'C' -> new ParsedTypeInfo(TypeInfos.CHAR,    start + 1);
			case 'Z' -> new ParsedTypeInfo(TypeInfos.BOOLEAN, start + 1);
			case 'L' -> {
				int end = ++start;
				while (input.charAt(end) != ';') end++;
				char[] chars = new char[end - start];
				for (int index = start; index < end; index++) {
					char c = input.charAt(index);
					chars[index - start] = c == '/' ? '.' : c;
				}
				try {
					yield new ParsedTypeInfo(
						of(Class.forName(new String(chars))),
						end + 1
					);
				}
				catch (ClassNotFoundException exception) {
					throw new IllegalArgumentException("Cannot find class " + input, exception);
				}
			}
			case '[' -> {
				ParsedTypeInfo result = parseNext(input, start + 1);
				yield new ParsedTypeInfo(makeArray(result.type), result.endIndex);
			}
			default -> throw new IllegalArgumentException(input.toString());
		};
	}

	public static TypeInfo parse(CharSequence input) {
		return parseNext(input, 0).type;
	}

	public static TypeInfo[] parseAll(CharSequence input) {
		ArrayBuilder<TypeInfo> array = new ArrayBuilder<>();
		for (int index = 0; index < input.length();) {
			ParsedTypeInfo next = parseNext(input, index);
			array.add(next.type);
			index = next.endIndex;
		}
		return array.toArray(TypeInfo.ARRAY_FACTORY);
	}

	public static TypeInfo of(java.lang.reflect.Type type) {
		if (type instanceof Class<?> clazz) {
			return of(clazz);
		}
		else if (type instanceof TypeVariable<?> variable) {
			return of(variable.getBounds()[0]).generic();
		}
		else if (type instanceof ParameterizedType parameterized) {
			return of(parameterized.getRawType());
		}
		else if (type instanceof GenericArrayType array) {
			return makeArray(of(array.getGenericComponentType())).generic();
		}
		else if (type instanceof WildcardType wildcard) {
			return of(wildcard.getUpperBounds()[0]).generic();
		}
		else {
			throw new IllegalArgumentException("Unknown type: " + type);
		}
	}

	public static TypeInfo convert(Class<?> clazz) {
		Type name = Type.getType(clazz);
		if (clazz.isPrimitive()) return makePrimitive(name);
		if (clazz.isArray()) return makeArray(name, of(clazz.getComponentType()));
		if (clazz.isAnnotation()) return makeAnnotation(name);
		if (clazz.isInterface()) return makeInterface(name, allOf(clazz.getInterfaces()));
		if (clazz.isEnum()) return makeEnum(name, allOf(clazz.getInterfaces()));
		if (clazz.isRecord()) return makeRecord(name, allOf(clazz.getInterfaces()));
		return makeClass(name, of(clazz.getSuperclass()), allOf(clazz.getInterfaces()));
	}

	public TypeInfo generic() {
		return this.isGeneric ? this : new TypeInfo(this.type, this.name, this.superClass, this.superInterfaces, this.componentType, true);
	}

	public TypeInfo notGeneric() {
		return !this.isGeneric ? this : new TypeInfo(this.type, this.name, this.superClass, this.superInterfaces, this.componentType, false);
	}

	public Sort getSort() {
		return switch (this.name.getSort()) {
			case Type.VOID    -> Sort.VOID;
			case Type.BOOLEAN -> Sort.BOOLEAN;
			case Type.BYTE    -> Sort.BYTE;
			case Type.CHAR    -> Sort.CHAR;
			case Type.SHORT   -> Sort.SHORT;
			case Type.INT     -> Sort.INT;
			case Type.LONG    -> Sort.LONG;
			case Type.FLOAT   -> Sort.FLOAT;
			case Type.DOUBLE  -> Sort.DOUBLE;
			case Type.OBJECT  -> Sort.OBJECT;
			case Type.ARRAY   -> Sort.ARRAY;
			default -> throw new AssertionError(this.name);
		};
	}

	public Class<?> toClass() {
		return switch (this.getSort()) {
			case BYTE          -> byte   .class;
			case SHORT         -> short  .class;
			case INT           -> int    .class;
			case LONG          -> long   .class;
			case FLOAT         -> float  .class;
			case DOUBLE        -> double .class;
			case CHAR          -> char   .class;
			case BOOLEAN       -> boolean.class;
			case VOID          -> void   .class;
			case OBJECT, ARRAY -> {
				try {
					yield Class.forName(this.getClassName());
				}
				catch (ClassNotFoundException exception) {
					throw new IllegalArgumentException(this + " does not correspond to a currently defined class.", exception);
				}
			}
		};
	}

	public Type toAsmType() {
		return this.name;
	}

	public String getInternalName() {
		return this.name.getInternalName();
	}

	public String getClassName() {
		return this.name.getClassName();
	}

	public String getDescriptor() {
		return this.name.getDescriptor();
	}

	public String getSimpleName() {
		String name = this.getInternalName();
		int start = name.length();
		for (char c; --start >= 0 && (c = name.charAt(start)) != '/' && c != '$';);
		return name.substring(start + 1);
	}

	public int getOpcode(int base) {
		return this.name.getOpcode(base);
	}

	public int getSize() {
		return this.name.getSize();
	}

	public static TypeInfo makeAnnotation(Type name) {
		return new TypeInfo(ClassType.ANNOTATION, name, TypeInfos.OBJECT, ANNOTATION_INTERFACES, null, false);
	}

	public static TypeInfo makeArray(Type name, TypeInfo componentType) {
		return new TypeInfo(ClassType.ARRAY, name, TypeInfos.OBJECT, ARRAY_INTERFACES, componentType, componentType.isGeneric);
	}

	public static TypeInfo makeArray(TypeInfo componentType) {
		return makeArray(Type.getType('[' + componentType.name.getDescriptor()), componentType);
	}

	public static TypeInfo makeClass(Type name, TypeInfo superClass, TypeInfo[] superInterfaces) {
		return new TypeInfo(ClassType.CLASS, name, superClass, superInterfaces, null, false);
	}

	public static TypeInfo makeEnum(Type name, TypeInfo[] superInterfaces) {
		return new TypeInfo(ClassType.ENUM, name, TypeInfos.ENUM, superInterfaces, null, false);
	}

	public static TypeInfo makeInterface(Type name, TypeInfo[] superInterfaces) {
		return new TypeInfo(ClassType.INTERFACE, name, TypeInfos.OBJECT, superInterfaces, null, false);
	}

	public static TypeInfo makePrimitive(Type name) {
		return new TypeInfo(ClassType.PRIMITIVE, name, null, ARRAY_FACTORY.empty(), null, false);
	}

	public static TypeInfo makeRecord(Type name, TypeInfo[] superInterfaces) {
		return new TypeInfo(ClassType.CLASS, name, TypeInfos.RECORD, superInterfaces, null, false);
	}

	public Set<TypeInfo> getAllAssignableTypes() {
		Set<TypeInfo> set = this.allAssignableTypes;
		if (set == null) {
			set = new LinkedHashSet<>(8);
			this.recursiveAddTypes(set, false);
			this.allAssignableTypes = set;
		}
		return set;
	}

	@Deprecated
	public Set<TypeInfo> getAllCastableTypes() {
		Set<TypeInfo> set = this.allCastableTypes;
		if (set == null) {
			set = new HashSet<>(8);
			this.recursiveAddTypes(set, true);
			this.allCastableTypes = set;
		}
		return set;
	}

	@SuppressWarnings("fallthrough")
	public void recursiveAddTypes(Set<TypeInfo> set, boolean autoCast) {
		if (!set.add(this)) return;
		if (this.superClass != null) this.superClass.recursiveAddTypes(set, autoCast);
		for (TypeInfo superInterface : this.superInterfaces) {
			superInterface.recursiveAddTypes(set, autoCast);
		}
		if (this.componentType != null) {
			this
			.componentType
			.getAllAssignableTypes()
			.stream()
			.map(TypeInfo::makeArray)
			.forEach(set::add);
		}
		if (autoCast) {
			switch (this.getSort()) {
				case BOOLEAN: set.add(TypeInfos.BYTE);
				case BYTE:    set.add(TypeInfos.CHAR);
				case CHAR:    set.add(TypeInfos.SHORT);
				case SHORT:   set.add(TypeInfos.INT);
				case INT:     set.add(TypeInfos.LONG);
				case LONG:    set.add(TypeInfos.FLOAT);
				case FLOAT:   set.add(TypeInfos.DOUBLE);
				case DOUBLE:  this.box().recursiveAddTypes(set, true);
					break;
				case OBJECT:
					if (this.isWrapper()) {
						this.unbox().recursiveAddTypes(set, true);
					}
					break;
				case ARRAY, VOID:
					break;
				default:
					break;
			}
			set.add(TypeInfos.VOID);
		}
	}

	public boolean extendsOrImplements(TypeInfo that) {
		return this.getAllAssignableTypes().contains(that);
	}

	@Deprecated
	public boolean canBeCastTo(TypeInfo that) {
		return this.getAllCastableTypes().contains(that);
	}

	@Override
	public int hashCode() {
		//we expect that a unique name implies a unique TypeInfo,
		//however this is not guaranteed.
		//for performance reasons, hashCode() will only use the name,
		//but equals() will check all the other properties.
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return this == object || (
			object instanceof TypeInfo that &&
			this.type == that.type &&
			this.name.equals(that.name) &&
			Objects.equals(this.superClass, that.superClass) &&
			Arrays.equals(this.superInterfaces, that.superInterfaces) &&
			Objects.equals(this.componentType, that.componentType)
		);
	}

	public boolean simpleEquals(TypeInfo that) {
		return this == that || (
			that != null &&
			this.name.equals(that.name)
		);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(64).append(this.type.name().toLowerCase(Locale.ROOT)).append(' ').append(this.name.getInternalName());
		if (this.superClass != null) builder.append(" extends ").append(this.superClass.name.getInternalName());
		if (this.superInterfaces.length != 0) builder.append(" implements ").append(Arrays.stream(this.superInterfaces).map(anInterface -> anInterface.name.getInternalName()).collect(Collectors.joining(", ")));
		if (this.componentType != null) builder.append(" arrayof ").append(this.componentType.name.getInternalName());
		if (this.isGeneric) builder.append(" (generic)");
		return builder.toString();
	}

	public boolean isVoid() {
		return this.getSort() == Sort.VOID;
	}

	public boolean isValue() {
		return this.getSort() != Sort.VOID;
	}

	public boolean isPrimitive() {
		return this.getSort().ordinal() <= Sort.DOUBLE.ordinal();
	}

	public boolean isPrimitiveValue() {
		int ordinal = this.getSort().ordinal();
		return ordinal >= Sort.BOOLEAN.ordinal() && ordinal <= Sort.DOUBLE.ordinal();
	}

	public boolean isNumber() {
		int ordinal = this.getSort().ordinal();
		return ordinal >= Sort.BYTE.ordinal() && ordinal <= Sort.DOUBLE.ordinal();
	}

	public boolean isInteger() {
		int ordinal = this.getSort().ordinal();
		return ordinal >= Sort.BYTE.ordinal() && ordinal <= Sort.LONG.ordinal();
	}

	public boolean isSingleWidthInt() {
		int ordinal = this.getSort().ordinal();
		return ordinal >= Sort.BYTE.ordinal() && ordinal <= Sort.INT.ordinal();
	}

	public boolean isFloat() {
		int ordinal = this.getSort().ordinal();
		return ordinal >= Sort.FLOAT.ordinal() && ordinal <= Sort.DOUBLE.ordinal();
	}

	public boolean isSingleWidth() {
		Sort sort = this.getSort();
		return sort != Sort.LONG && sort != Sort.DOUBLE;
	}

	public boolean isDoubleWidth() {
		Sort sort = this.getSort();
		return sort == Sort.LONG || sort == Sort.DOUBLE;
	}

	public boolean isObject() {
		return this.getSort().ordinal() >= Sort.OBJECT.ordinal();
	}

	public boolean isArray() {
		return this.componentType != null;
	}

	public boolean isWrapper() {
		return TypeInfos.UNBOXING.containsKey(this);
	}

	public TypeInfo box() {
		return TypeInfos.box(this);
	}

	public TypeInfo unbox() {
		return TypeInfos.unbox(this);
	}

	public static enum Sort {
		VOID   (TypeInfos.VOID),
		BOOLEAN(TypeInfos.BOOLEAN),
		BYTE   (TypeInfos.BYTE),
		CHAR   (TypeInfos.CHAR),
		SHORT  (TypeInfos.SHORT),
		INT    (TypeInfos.INT),
		LONG   (TypeInfos.LONG),
		FLOAT  (TypeInfos.FLOAT),
		DOUBLE (TypeInfos.DOUBLE),
		OBJECT (null),
		ARRAY  (null);

		public static final Sort[] VALUES = values();

		public final TypeInfo canonicalInstance;

		Sort(TypeInfo canonicalInstance) {
			this.canonicalInstance = canonicalInstance;
		}
	}
}