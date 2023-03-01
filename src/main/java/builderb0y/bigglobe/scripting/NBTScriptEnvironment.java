package builderb0y.bigglobe.scripting;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;

import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.CastingSupport.CastProvider;
import builderb0y.scripting.bytecode.CastingSupport.Caster;
import builderb0y.scripting.bytecode.CastingSupport.LookupCastProvider;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.NamedValues;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.NamedValues.NamedValue;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NBTScriptEnvironment implements ScriptEnvironment {

	public static final NBTScriptEnvironment INSTANCE = new NBTScriptEnvironment();

	public static final TypeInfo
		NBT_ELEMENT_TYPE     = type(NbtElement          .class),
		NBT_BYTE_TYPE        = type(NbtByte             .class),
		NBT_SHORT_TYPE       = type(NbtShort            .class),
		NBT_INT_TYPE         = type(NbtInt              .class),
		NBT_LONG_TYPE        = type(NbtLong             .class),
		NBT_FLOAT_TYPE       = type(NbtFloat            .class),
		NBT_DOUBLE_TYPE      = type(NbtDouble           .class),
		NBT_NUMBER_TYPE      = type(AbstractNbtNumber   .class),
		NBT_BYTE_ARRAY_TYPE  = type(NbtByteArray        .class),
		NBT_INT_ARRAY_TYPE   = type(NbtIntArray         .class),
		NBT_LONG_ARRAY_TYPE  = type(NbtLongArray        .class),
		NBT_LIST_TYPE        = type(NbtList             .class),
		NBT_COLLECTION_TYPE  = type(AbstractNbtList     .class),
		NBT_STRING_TYPE      = type(NbtString           .class),
		NBT_COMPOUND_TYPE    = type(NbtCompound         .class),
		NBT_ENVIRONMENT_TYPE = type(NBTScriptEnvironment.class);

	public static final CastProvider NBT_CASTS = (
		new LookupCastProvider()

		.append(TypeInfos.BYTE,    NBT_BYTE_TYPE,    true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "nbtByte",    NBT_BYTE_TYPE,     TypeInfos.BYTE   )))
		.append(TypeInfos.SHORT,   NBT_SHORT_TYPE,   true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "nbtShort",   NBT_SHORT_TYPE,    TypeInfos.SHORT  )))
		.append(TypeInfos.INT,     NBT_INT_TYPE,     true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "nbtInt",     NBT_INT_TYPE,      TypeInfos.INT    )))
		.append(TypeInfos.LONG,    NBT_LONG_TYPE,    true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "nbtLong",    NBT_LONG_TYPE,     TypeInfos.LONG   )))
		.append(TypeInfos.FLOAT,   NBT_FLOAT_TYPE,   true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "nbtFloat",   NBT_FLOAT_TYPE,    TypeInfos.FLOAT  )))
		.append(TypeInfos.DOUBLE,  NBT_DOUBLE_TYPE,  true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "nbtDouble",  NBT_DOUBLE_TYPE,   TypeInfos.DOUBLE )))
		.append(TypeInfos.BOOLEAN, NBT_BYTE_TYPE,    true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "nbtBoolean", NBT_BYTE_TYPE,     TypeInfos.BOOLEAN)))
		.append(TypeInfos.STRING,  NBT_STRING_TYPE,  true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "nbtString",  NBT_STRING_TYPE,   TypeInfos.STRING )))

		.append(NBT_ELEMENT_TYPE, TypeInfos.BYTE,    true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "asByte",     TypeInfos.BYTE,    NBT_ELEMENT_TYPE )))
		.append(NBT_ELEMENT_TYPE, TypeInfos.SHORT,   true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "asShort",    TypeInfos.SHORT,   NBT_ELEMENT_TYPE )))
		.append(NBT_ELEMENT_TYPE, TypeInfos.INT,     true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "asInt",      TypeInfos.INT,     NBT_ELEMENT_TYPE )))
		.append(NBT_ELEMENT_TYPE, TypeInfos.LONG,    true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "asLong",     TypeInfos.LONG,    NBT_ELEMENT_TYPE )))
		.append(NBT_ELEMENT_TYPE, TypeInfos.FLOAT,   true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "asFloat",    TypeInfos.FLOAT,   NBT_ELEMENT_TYPE )))
		.append(NBT_ELEMENT_TYPE, TypeInfos.DOUBLE,  true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "asDouble",   TypeInfos.DOUBLE,  NBT_ELEMENT_TYPE )))
		.append(NBT_ELEMENT_TYPE, TypeInfos.BOOLEAN, true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "asBoolean",  TypeInfos.BOOLEAN, NBT_ELEMENT_TYPE )))
		.append(NBT_ELEMENT_TYPE, TypeInfos.STRING,  true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, NBT_ENVIRONMENT_TYPE, "asString",   TypeInfos.STRING,  NBT_ELEMENT_TYPE )))
	);

	@Override
	public @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		return switch (name) {
			case "Nbt"           -> NBT_ELEMENT_TYPE;
			case "NbtByte"       -> NBT_BYTE_TYPE;
			case "NbtShort"      -> NBT_SHORT_TYPE;
			case "NbtInt"        -> NBT_INT_TYPE;
			case "NbtLong"       -> NBT_LONG_TYPE;
			case "NbtFloat"      -> NBT_FLOAT_TYPE;
			case "NbtDouble"     -> NBT_DOUBLE_TYPE;
			case "NbtNumber"     -> NBT_NUMBER_TYPE;
			case "NbtByteArray"  -> NBT_BYTE_ARRAY_TYPE;
			case "NbtIntArray"   -> NBT_INT_ARRAY_TYPE;
			case "NbtLongArray"  -> NBT_LONG_ARRAY_TYPE;
			case "NbtList"       -> NBT_LIST_TYPE;
			case "NbtCollection" -> NBT_COLLECTION_TYPE;
			case "NbtString"     -> NBT_STRING_TYPE;
			case "NbtCompound"   -> NBT_COMPOUND_TYPE;
			default              -> null;
		};
	}

	public static final MethodInfo
		NBT_BYTE_CONSTRUCTOR       = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtByte",      NbtByte     .class, byte  .class),
		NBT_SHORT_CONSTRUCTOR      = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtShort",     NbtShort    .class, short .class),
		NBT_INT_CONSTRUCTOR        = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtInt",       NbtInt      .class, int   .class),
		NBT_LONG_CONSTRUCTOR       = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtLong",      NbtLong     .class, long  .class),
		NBT_FLOAT_CONSTRUCTOR      = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtFloat",     NbtFloat    .class, float .class),
		NBT_DOUBLE_CONSTRUCTOR     = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtDouble",    NbtDouble   .class, double.class),
		NBT_BYTE_ARRAY_CONSTRUCTOR = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtByteArray", NbtByteArray.class, byte[].class),
		NBT_INT_ARRAY_CONSTRUCTOR  = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtIntArray",  NbtIntArray .class, int[] .class),
		NBT_LONG_ARRAY_CONSTRUCTOR = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtLongArray", NbtLongArray.class, long[].class),
		NBT_STRING_CONSTRUCTOR     = method(ACC_PUBLIC | ACC_STATIC, NBTScriptEnvironment.class, "nbtString",    NbtString   .class, String.class);

	public static NbtByte      nbtBoolean  (boolean value) { return NbtByte  .of(value); }
	public static NbtByte      nbtByte     (byte    value) { return NbtByte  .of(value); }
	public static NbtShort     nbtShort    (short   value) { return NbtShort .of(value); }
	public static NbtInt       nbtInt      (int     value) { return NbtInt   .of(value); }
	public static NbtLong      nbtLong     (long    value) { return NbtLong  .of(value); }
	public static NbtFloat     nbtFloat    (float   value) { return NbtFloat .of(value); }
	public static NbtDouble    nbtDouble   (double  value) { return NbtDouble.of(value); }
	public static NbtString    nbtString   (String  value) { return NbtString.of(value); }
	public static NbtByteArray nbtByteArray(byte[]  value) { return new NbtByteArray(value); }
	public static NbtIntArray  nbtIntArray (int []  value) { return new NbtIntArray (value); }
	public static NbtLongArray nbtLongArray(long[]  value) { return new NbtLongArray(value); }

	public static InsnTree array(ExpressionParser parser, MethodInfo method, InsnTree... arguments) {
		return invokeStatic(
			method,
			arguments.length == 1 && arguments[0].getTypeInfo().simpleEquals(method.paramTypes[0])
			? arguments
			: new InsnTree[] { newArrayWithContents(parser, method.paramTypes[0], arguments) }
		);
	}

	public static InsnTree nonArray(ExpressionParser parser, MethodInfo method, InsnTree... arguments) throws ScriptParsingException {
		if (arguments.length != 1) {
			throw new ScriptParsingException("Wrong number of arguments for " + method.name + ": expected 1, got " + arguments.length, parser.input);
		}
		InsnTree from = arguments[0];
		InsnTree to = from.cast(parser, method.paramTypes[0], CastMode.IMPLICIT_THROW);
		return invokeStatic(method, from == to ? arguments : new InsnTree[] { to });
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		return switch (name) {
			case "nbtByte"      -> nonArray(parser, NBT_BYTE_CONSTRUCTOR,       arguments);
			case "nbtShort"     -> nonArray(parser, NBT_SHORT_CONSTRUCTOR,      arguments);
			case "nbtInt"       -> nonArray(parser, NBT_INT_CONSTRUCTOR,        arguments);
			case "nbtLong"      -> nonArray(parser, NBT_LONG_CONSTRUCTOR,       arguments);
			case "nbtFloat"     -> nonArray(parser, NBT_FLOAT_CONSTRUCTOR,      arguments);
			case "nbtDouble"    -> nonArray(parser, NBT_DOUBLE_CONSTRUCTOR,     arguments);
			case "nbtString"    -> nonArray(parser, NBT_STRING_CONSTRUCTOR,     arguments);
			case "nbtByteArray" -> array   (parser, NBT_BYTE_ARRAY_CONSTRUCTOR, arguments);
			case "nbtIntArray"  -> array   (parser, NBT_INT_ARRAY_CONSTRUCTOR,  arguments);
			case "nbtLongArray" -> array   (parser, NBT_LONG_ARRAY_CONSTRUCTOR, arguments);
			case "nbtList"      -> new ListBuilderInsnTree(ScriptEnvironment.castArguments(parser, "nbtList", repeat(NBT_ELEMENT_TYPE, arguments.length), CastMode.IMPLICIT_THROW, arguments));
			default -> null;
		};
	}

	@Override
	public @Nullable InsnTree parseKeyword(ExpressionParser parser, String name) throws ScriptParsingException {
		if (name.equals("nbtCompound")) {
			NamedValues namedValues = NamedValues.parse(parser, NBT_ELEMENT_TYPE);
			return namedValues.maybeWrap(new CompoundBuilderInsnTree(namedValues.values()));
		}
		else {
			return null;
		}
	}

	public static final MethodInfo
		AS_BOOLEAN = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "asBoolean", TypeInfos.BOOLEAN, NBT_ELEMENT_TYPE),
		AS_BYTE    = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "asByte",    TypeInfos.BYTE, NBT_ELEMENT_TYPE),
		AS_SHORT   = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "asShort",   TypeInfos.SHORT, NBT_ELEMENT_TYPE),
		AS_INT     = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "asInt",     TypeInfos.INT, NBT_ELEMENT_TYPE),
		AS_LONG    = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "asLong",    TypeInfos.LONG, NBT_ELEMENT_TYPE),
		AS_FLOAT   = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "asFloat",   TypeInfos.FLOAT, NBT_ELEMENT_TYPE),
		AS_DOUBLE  = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "asDouble",  TypeInfos.DOUBLE, NBT_ELEMENT_TYPE),
		AS_STRING  = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "asString",  TypeInfos.STRING, NBT_ELEMENT_TYPE),
		GET_MEMBER = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "getMember", NBT_ELEMENT_TYPE, NBT_ELEMENT_TYPE, TypeInfos.STRING),
		SET_MEMBER = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "setMember", TypeInfos.VOID, NBT_ELEMENT_TYPE, TypeInfos.STRING, NBT_ELEMENT_TYPE),
		GET_ELEMENT = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "getElement", NBT_ELEMENT_TYPE, NBT_ELEMENT_TYPE, TypeInfos.INT),
		SET_ELEMENT = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "setElement", TypeInfos.VOID, NBT_ELEMENT_TYPE, TypeInfos.INT, NBT_ELEMENT_TYPE);

	public static boolean asBoolean(NbtElement element) { return element instanceof AbstractNbtNumber number && CastingSupport.D2Z(number.doubleValue()); }
	public static byte    asByte   (NbtElement element) { return element instanceof AbstractNbtNumber number ? number.byteValue  () : 0; }
	public static short   asShort  (NbtElement element) { return element instanceof AbstractNbtNumber number ? number.shortValue () : 0; }
	public static int     asInt    (NbtElement element) { return element instanceof AbstractNbtNumber number ? number.intValue   () : 0; }
	public static long    asLong   (NbtElement element) { return element instanceof AbstractNbtNumber number ? number.longValue  () : 0; }
	public static float   asFloat  (NbtElement element) { return element instanceof AbstractNbtNumber number ? number.floatValue () : 0; }
	public static double  asDouble (NbtElement element) { return element instanceof AbstractNbtNumber number ? number.doubleValue() : 0; }
	public static String  asString (NbtElement element) { return element != null ? element.asString() : "null"; }

	public static NbtElement getMember(NbtElement element, String name) {
		return element instanceof NbtCompound compound ? compound.get(name) : null;
	}

	public static void setMember(NbtElement element, String name, NbtElement value) {
		if (element instanceof NbtCompound compound) {
			if (value != null) compound.put(name, value);
			else compound.remove(name);
		}
		else {
			throw new IllegalArgumentException("Can't set member named " + name + " on " + element + " to " + value);
		}
	}

	public static NbtElement getElement(NbtElement element, int index) {
		if (element instanceof AbstractNbtList<?> list) {
			if (index >= 0 && index < list.size()) {
				return list.get(index);
			}
		}
		return null;
	}

	public static void setElement(NbtElement element, int index, NbtElement value) {
		if (value == null) {
			throw new NullPointerException("Can't set index " + index + " on " + element + " to a null value");
		}
		if (element instanceof AbstractNbtList<?> list) {
			list.setElement(index, value);
		}
		else {
			throw new IllegalArgumentException("Can't set element at index " + index + " on " + element + " to " + value);
		}
	}

	@Override
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		if (receiver.getTypeInfo().extendsOrImplements(NBT_ELEMENT_TYPE)) {
			return new GetMemberInsnTree(receiver, ldc(name), GET_MEMBER, SET_MEMBER);
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		if (receiver.getTypeInfo().extendsOrImplements(NBT_ELEMENT_TYPE)) {
			return switch (name) {
				case "" -> {
					if (arguments.length != 1) {
						throw new ScriptParsingException("Wrong number of arguments: expected 1, got " + arguments.length, parser.input);
					}
					InsnTree nameOrIndex = arguments[0];
					if (nameOrIndex.getTypeInfo().simpleEquals(TypeInfos.STRING)) {
						yield new GetMemberInsnTree(receiver, nameOrIndex, GET_MEMBER, SET_MEMBER);
					}
					else if (nameOrIndex.getTypeInfo().isSingleWidthInt()) {
						yield new GetMemberInsnTree(receiver, nameOrIndex, GET_ELEMENT, SET_ELEMENT);
					}
					else {
						throw new ScriptParsingException("Indexing an NBT element requires a String or int as the key", parser.input);
					}
				}
				case "asBoolean" -> invokeWrapped(receiver, AS_BOOLEAN, arguments);
				case "asByte"    -> invokeWrapped(receiver, AS_BYTE,    arguments);
				case "asShort"   -> invokeWrapped(receiver, AS_SHORT,   arguments);
				case "asInt"     -> invokeWrapped(receiver, AS_INT,     arguments);
				case "asLong"    -> invokeWrapped(receiver, AS_LONG,    arguments);
				case "asFloat"   -> invokeWrapped(receiver, AS_FLOAT,   arguments);
				case "asDouble"  -> invokeWrapped(receiver, AS_DOUBLE,  arguments);
				case "asString"  -> invokeWrapped(receiver, AS_STRING,  arguments);
				//todo: finish add and remove.
				case "add" -> null;
				case "remove" -> null;
				default -> null;
			};
		}
		return null;
	}

	public static class ListBuilderInsnTree implements InsnTree {

		public static final MethodInfo
			CONSTRUCT = method(ACC_PUBLIC, NbtList.class, "<init>", void.class),
			BUILD_LIST = method(ACC_PUBLIC | ACC_STATIC, ListBuilderInsnTree.class, "buildList", NbtList.class, NbtList.class, NbtElement.class);

		public InsnTree[] values;

		public ListBuilderInsnTree(InsnTree[] values) {
			this.values = values;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			newInstance(CONSTRUCT).emitBytecode(method);
			for (InsnTree value : this.values) {
				//constructed instance already on stack.
				value.emitBytecode(method);
				BUILD_LIST.emit(method, INVOKESTATIC);
				//BUILD_LIST returns constructed instance, so it's still on the stack.
			}
		}

		@Override
		public TypeInfo getTypeInfo() {
			return NBT_LIST_TYPE;
		}

		public static NbtList buildList(NbtList list, NbtElement element) {
			list.add(element);
			return list;
		}
	}

	public static class CompoundBuilderInsnTree implements InsnTree {

		public static final MethodInfo
			CONSTRUCT      = method(ACC_PUBLIC, NbtCompound.class, "<init>", void.class),
			BUILD_COMPOUND = method(ACC_PUBLIC | ACC_STATIC, CompoundBuilderInsnTree.class, "buildCompound", NbtCompound.class, NbtCompound.class, String.class, NbtElement.class);

		public NamedValue[] pairs;

		public CompoundBuilderInsnTree(NamedValue[] pairs) {
			this.pairs = pairs;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			newInstance(CONSTRUCT).emitBytecode(method);
			for (NamedValue pair : this.pairs) {
				//constructed instance already on stack.
				constant(pair.name()).emitBytecode(method);
				pair.value().emitBytecode(method);
				BUILD_COMPOUND.emit(method, INVOKESTATIC);
				//BUILD_COMPOUND returns constructed instance, so it's still on the stack.
			}
		}

		@Override
		public TypeInfo getTypeInfo() {
			return NBT_COMPOUND_TYPE;
		}

		public static NbtCompound buildCompound(NbtCompound compound, String name, NbtElement element) {
			compound.put(name, element);
			return compound;
		}
	}

	public static class GetMemberInsnTree implements InsnTree {

		public InsnTree receiver, nameOrIndex;
		public MethodInfo getterMethod, setterMethod;

		public GetMemberInsnTree(InsnTree receiver, InsnTree nameOrIndex, MethodInfo getterMethod, MethodInfo setterMethod) {
			this.receiver = receiver;
			this.nameOrIndex = nameOrIndex;
			this.getterMethod = getterMethod;
			this.setterMethod = setterMethod;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.receiver.emitBytecode(method);
			this.nameOrIndex.emitBytecode(method);
			this.getterMethod.emit(method, INVOKESTATIC);
		}

		@Override
		public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
			if (op == UpdateOp.ASSIGN) {
				return new SetMemberInsnTree(this.receiver, this.nameOrIndex, rightValue.cast(parser, NBT_ELEMENT_TYPE, CastMode.IMPLICIT_THROW), this.setterMethod);
			}
			throw new ScriptParsingException("Updating NBT data on compound not yet implemented", parser.input);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return NBT_ELEMENT_TYPE;
		}
	}

	public static class SetMemberInsnTree implements InsnTree {

		public InsnTree receiver, nameOrIndex, value;
		public MethodInfo setterMethod;

		public SetMemberInsnTree(InsnTree receiver, InsnTree nameOrIndex, InsnTree value, MethodInfo setterMethod) {
			this.receiver = receiver;
			this.nameOrIndex = nameOrIndex;
			this.value = value;
			this.setterMethod = setterMethod;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.receiver.emitBytecode(method);
			this.nameOrIndex.emitBytecode(method);
			this.value.emitBytecode(method);
			this.setterMethod.emit(method, INVOKESTATIC);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return TypeInfos.VOID;
		}

		@Override
		public boolean canBeStatement() {
			return true;
		}
	}
}