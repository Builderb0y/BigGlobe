package builderb0y.bigglobe.scripting.environments;

import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.nbt.*;

import builderb0y.bigglobe.mixinInterfaces.NbtCompoundRemoveAndReturnAccess;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.collections.NormalListMapGetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.CommonMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
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
		NBT_BYTE_ARRAY_CONSTRUCTOR = MethodInfo.getMethod(NbtScriptEnvironment.class, "nbtByteArray"),
		NBT_INT_ARRAY_CONSTRUCTOR  = MethodInfo.getMethod(NbtScriptEnvironment.class, "nbtIntArray"),
		NBT_LONG_ARRAY_CONSTRUCTOR = MethodInfo.getMethod(NbtScriptEnvironment.class, "nbtLongArray");

	public static final MethodInfo
		GET_MEMBER  = MethodInfo.getMethod(NbtScriptEnvironment.class, "getMember"),
		SET_MEMBER  = MethodInfo.getMethod(NbtScriptEnvironment.class, "setMember"),
		GET_ELEMENT = MethodInfo.getMethod(NbtScriptEnvironment.class, "getElement"),
		SET_ELEMENT = MethodInfo.getMethod(NbtScriptEnvironment.class, "setElement");

	public static final MutableScriptEnvironment COMMON = (
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
		.addFunction("nbtIntArray",  array(NBT_INT_ARRAY_CONSTRUCTOR))
		.addFunction("nbtLongArray", array(NBT_LONG_ARRAY_CONSTRUCTOR))
		.addFunction("nbtList", (ExpressionParser parser, String name, InsnTree... arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, repeat(NBT_ELEMENT_TYPE, arguments.length), CastMode.IMPLICIT_THROW, arguments);
			return new CastResult(new ListBuilderInsnTree(castArguments), arguments != castArguments);
		})
		.addKeyword("nbtCompound", (ExpressionParser parser, String name) -> {
			NamedValues namedValues = NamedValues.parse(parser, NBT_ELEMENT_TYPE, null);
			return namedValues.maybeWrap(new CompoundBuilderInsnTree(namedValues.values()));
		})

		.addMethodInvokeStatics(NbtScriptEnvironment.class, "asBoolean", "asByte", "asShort", "asInt", "asLong", "asFloat", "asDouble", "asString")
	);

	public static Consumer<MutableScriptEnvironment> createCommon() {
		return (MutableScriptEnvironment environment) -> environment.addAll(COMMON);
	}

	public static Consumer<MutableScriptEnvironment> createImmutable() {
		return (MutableScriptEnvironment environment) -> {
			environment
			.configure(createCommon())
			.addMethod(NBT_ELEMENT_TYPE, "", (ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
				if (arguments.length != 1) {
					throw new ScriptParsingException("Wrong number of arguments: expected 1, got " + arguments.length, parser.input);
				}
				InsnTree nameOrIndex = arguments[0];
				if (nameOrIndex.getTypeInfo().equals(TypeInfos.STRING)) {
					return new CastResult(invokeStatic(GET_MEMBER, receiver, nameOrIndex), false);
				}
				else if (nameOrIndex.getTypeInfo().isSingleWidthInt()) {
					return new CastResult(invokeStatic(GET_ELEMENT, receiver, nameOrIndex.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW)), false);
				}
				else {
					throw new ScriptParsingException("Indexing an NBT element requires a String or int as the key", parser.input);
				}
			})
			.addField(NBT_ELEMENT_TYPE, null, (ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
				return invokeStatic(GET_MEMBER, receiver, ldc(name));
			})
			;
		};
	}

	public static Consumer<MutableScriptEnvironment> createMutable() {
		return (MutableScriptEnvironment environment) -> {
			environment
			.configure(createCommon())
			.addMethod(NBT_ELEMENT_TYPE, "", (ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
				if (arguments.length != 1) {
					throw new ScriptParsingException("Wrong number of arguments: expected 1, got " + arguments.length, parser.input);
				}
				InsnTree nameOrIndex = arguments[0];
				if (nameOrIndex.getTypeInfo().equals(TypeInfos.STRING)) {
					return new CastResult(NormalListMapGetterInsnTree.from(receiver, GET_MEMBER, nameOrIndex, SET_MEMBER, "NbtElement", mode), false);
				}
				else if (nameOrIndex.getTypeInfo().isSingleWidthInt()) {
					return new CastResult(NormalListMapGetterInsnTree.from(receiver, GET_ELEMENT, nameOrIndex.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW), SET_ELEMENT, "NbtElement", mode), false);
				}
				else {
					throw new ScriptParsingException("Indexing an NBT element requires a String or int as the key", parser.input);
				}
			})
			.addField(NBT_ELEMENT_TYPE, null, (ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
				return NormalListMapGetterInsnTree.from(receiver, GET_MEMBER, ldc(name), SET_MEMBER, "NbtElement", switch (mode) {
					case NORMAL -> GetMethodMode.NORMAL;
					case NULLABLE -> GetMethodMode.NULLABLE;
					case RECEIVER -> GetMethodMode.RECEIVER;
					case NULLABLE_RECEIVER -> GetMethodMode.NULLABLE_RECEIVER;
				});
			})
			;
		};
	}

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

	public static NbtElement setMember(NbtElement element, String name, NbtElement value) {
		Objects.requireNonNull(name, "key is null.");
		if (element instanceof NbtCompound compound) {
			if (value != null) return compound.put(name, value);
			else return ((NbtCompoundRemoveAndReturnAccess)(compound)).bigglobe_remove(name);
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static NbtElement setElement(NbtElement element, int index, NbtElement value) {
		if (value == null) {
			throw new NullPointerException("Can't set index " + index + " on " + element + " to a null value");
		}
		if (element instanceof AbstractNbtList list) {
			return list.set(index, value);
		}
		else {
			throw new IllegalArgumentException("Can't set element at index " + index + " on " + element + " to " + value);
		}
	}

	public static FunctionHandler array(MethodInfo method) {
		return (ExpressionParser parser, String name, InsnTree... arguments) -> {
			return new CastResult(
				invokeStatic(
					method,
					arguments.length == 1 && arguments[0].getTypeInfo().equals(method.paramTypes[0])
					? arguments
					: new InsnTree[] { newArrayWithContents(parser, method.paramTypes[0], arguments) }
				),
				false
			);
		};
	}

	public static class ListBuilderInsnTree implements InsnTree {

		public static final MethodInfo
			CONSTRUCT = MethodInfo.findConstructor(NbtList.class),
			BUILD_LIST = MethodInfo.getMethod(ListBuilderInsnTree.class, "buildList");

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
			list.add(Objects.requireNonNull(element, "element"));
			return list;
		}
	}

	public static class CompoundBuilderInsnTree implements InsnTree {

		public static final MethodInfo
			CONSTRUCT      = MethodInfo.findConstructor(NbtCompound.class),
			BUILD_COMPOUND = MethodInfo.getMethod(CompoundBuilderInsnTree.class, "buildCompound");

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
}