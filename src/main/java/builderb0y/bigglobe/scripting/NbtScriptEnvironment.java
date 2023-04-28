package builderb0y.bigglobe.scripting;

import net.minecraft.nbt.*;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.NamedValues;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.NamedValues.NamedValue;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NbtScriptEnvironment {

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
		NBT_ENVIRONMENT_TYPE = type(NbtScriptEnvironment.class);

	public static final MethodInfo
		NBT_BYTE_ARRAY_CONSTRUCTOR = method(ACC_PUBLIC | ACC_STATIC, NbtScriptEnvironment.class, "nbtByteArray", NbtByteArray.class, byte[].class),
		NBT_INT_ARRAY_CONSTRUCTOR  = method(ACC_PUBLIC | ACC_STATIC, NbtScriptEnvironment.class, "nbtIntArray",  NbtIntArray .class, int[] .class),
		NBT_LONG_ARRAY_CONSTRUCTOR = method(ACC_PUBLIC | ACC_STATIC, NbtScriptEnvironment.class, "nbtLongArray", NbtLongArray.class, long[].class);

	public static final MethodInfo
		GET_MEMBER  = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "getMember", NBT_ELEMENT_TYPE, NBT_ELEMENT_TYPE, TypeInfos.STRING),
		SET_MEMBER  = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "setMember", TypeInfos.VOID, NBT_ELEMENT_TYPE, TypeInfos.STRING, NBT_ELEMENT_TYPE),
		GET_ELEMENT = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "getElement", NBT_ELEMENT_TYPE, NBT_ELEMENT_TYPE, TypeInfos.INT),
		SET_ELEMENT = method(ACC_PUBLIC | ACC_STATIC, NBT_ENVIRONMENT_TYPE, "setElement", TypeInfos.VOID, NBT_ELEMENT_TYPE, TypeInfos.INT, NBT_ELEMENT_TYPE);

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
		.addType("Nbt",           NBT_ELEMENT_TYPE)
		.addType("NbtByte",       NBT_BYTE_TYPE)
		.addType("NbtShort",      NBT_SHORT_TYPE)
		.addType("NbtInt",        NBT_INT_TYPE)
		.addType("NbtLong",       NBT_LONG_TYPE)
		.addType("NbtFloat",      NBT_FLOAT_TYPE)
		.addType("NbtDouble",     NBT_DOUBLE_TYPE)
		.addType("NbtNumber",     NBT_NUMBER_TYPE)
		.addType("NbtByteArray",  NBT_BYTE_ARRAY_TYPE)
		.addType("NbtIntArray",   NBT_INT_ARRAY_TYPE)
		.addType("NbtLongArray",  NBT_LONG_ARRAY_TYPE)
		.addType("NbtList",       NBT_LIST_TYPE)
		.addType("NbtCollection", NBT_COLLECTION_TYPE)
		.addType("NbtString",     NBT_STRING_TYPE)
		.addType("NbtCompound",   NBT_COMPOUND_TYPE)

		.addCastInvokeStatic(NbtScriptEnvironment.class, "nbtBoolean", true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "nbtByte",    true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "nbtShort",   true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "nbtInt",     true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "nbtLong",    true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "nbtFloat",   true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "nbtDouble",  true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "nbtString",  true)

		.addCastInvokeStatic(NbtScriptEnvironment.class, "asBoolean",  true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "asByte",     true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "asShort",    true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "asInt",      true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "asLong",     true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "asFloat",    true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "asDouble",   true)
		.addCastInvokeStatic(NbtScriptEnvironment.class, "asString",   true)

		.addFunctionInvokeStatics(NbtScriptEnvironment.class, "nbtBoolean", "nbtByte", "nbtShort", "nbtInt", "nbtLong", "nbtFloat", "nbtDouble", "nbtString")
		.addFunction("nbtByteArray", array(NBT_BYTE_ARRAY_CONSTRUCTOR))
		.addFunction("nbtIntArray", array(NBT_INT_ARRAY_CONSTRUCTOR))
		.addFunction("nbtLongArray", array(NBT_LONG_ARRAY_CONSTRUCTOR))
		.addFunction("nbtList", (parser, name, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, repeat(NBT_ELEMENT_TYPE, arguments.length), CastMode.IMPLICIT_THROW, arguments);
			return new CastResult(new ListBuilderInsnTree(castArguments), arguments != castArguments);
		})
		.addKeyword("nbtCompound", (parser, name) -> {
			NamedValues namedValues = NamedValues.parse(parser, NBT_ELEMENT_TYPE);
			return namedValues.maybeWrap(new CompoundBuilderInsnTree(namedValues.values()));
		})

		.addMethodInvokeStatics(NbtScriptEnvironment.class, "asBoolean", "asByte", "asShort", "asInt", "asLong", "asFloat", "asDouble", "asString")
		.addMethod(NBT_ELEMENT_TYPE, "", (parser, receiver, name, arguments) -> {
			if (arguments.length != 1) {
				throw new ScriptParsingException("Wrong number of arguments: expected 1, got " + arguments.length, parser.input);
			}
			InsnTree nameOrIndex = arguments[0];
			if (nameOrIndex.getTypeInfo().simpleEquals(TypeInfos.STRING)) {
				return new CastResult(new GetMemberInsnTree(receiver, nameOrIndex, GET_MEMBER, SET_MEMBER), false);
			}
			else if (nameOrIndex.getTypeInfo().isSingleWidthInt()) {
				return new CastResult(new GetMemberInsnTree(receiver, nameOrIndex, GET_ELEMENT, SET_ELEMENT), false);
			}
			else {
				throw new ScriptParsingException("Indexing an NBT element requires a String or int as the key", parser.input);
			}
		})

		.addField(NBT_ELEMENT_TYPE, null, (parser, receiver, name) -> {
			return new GetMemberInsnTree(receiver, ldc(name), GET_MEMBER, SET_MEMBER);
		})
	);

	public static NbtByte      nbtBoolean  (boolean value) { return NbtByte  .of(value); }
	public static NbtByte      nbtByte     (byte    value) { return NbtByte  .of(value); }
	public static NbtShort     nbtShort    (short   value) { return NbtShort .of(value); }
	public static NbtInt       nbtInt      (int     value) { return NbtInt   .of(value); }
	public static NbtLong      nbtLong     (long    value) { return NbtLong  .of(value); }
	public static NbtFloat     nbtFloat    (float   value) { return NbtFloat .of(value); }
	public static NbtDouble    nbtDouble   (double  value) { return NbtDouble.of(value); }
	public static NbtString    nbtString   (String  value) { return value == null ? null : NbtString.of(value); }
	public static NbtByteArray nbtByteArray(byte[]  value) { return value == null ? null : new NbtByteArray(value); }
	public static NbtIntArray  nbtIntArray (int []  value) { return value == null ? null : new NbtIntArray (value); }
	public static NbtLongArray nbtLongArray(long[]  value) { return value == null ? null : new NbtLongArray(value); }

	public static boolean asBoolean(NbtElement element) { return element instanceof AbstractNbtNumber number && number.byteValue  () != 0; }
	public static byte    asByte   (NbtElement element) { return element instanceof AbstractNbtNumber number ?  number.byteValue  () :  0; }
	public static short   asShort  (NbtElement element) { return element instanceof AbstractNbtNumber number ?  number.shortValue () :  0; }
	public static int     asInt    (NbtElement element) { return element instanceof AbstractNbtNumber number ?  number.intValue   () :  0; }
	public static long    asLong   (NbtElement element) { return element instanceof AbstractNbtNumber number ?  number.longValue  () :  0; }
	public static float   asFloat  (NbtElement element) { return element instanceof AbstractNbtNumber number ?  number.floatValue () :  0; }
	public static double  asDouble (NbtElement element) { return element instanceof AbstractNbtNumber number ?  number.doubleValue() :  0; }
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

	public static FunctionHandler array(MethodInfo method) {
		return (parser, name, arguments) -> {
			return new CastResult(
				invokeStatic(
					method,
					arguments.length == 1 && arguments[0].getTypeInfo().simpleEquals(method.paramTypes[0])
					? arguments
					: new InsnTree[] { newArrayWithContents(parser, method.paramTypes[0], arguments) }
				),
				false
			);
		};
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
			if (element != null) compound.put(name, element);
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