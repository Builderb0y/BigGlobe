package builderb0y.scripting.bytecode;

import java.util.*;
import java.util.stream.Stream;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Table;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.unary.CastInsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class CastingSupport {

	public static final MultiCastProvider PRIMITIVE_CASTING = (
		new MultiCastProvider()
		.append(
			new LookupCastProvider()

			.append(TypeInfos.BOOLEAN, TypeInfos.BYTE,    false, Caster.identity())
			.append(TypeInfos.BOOLEAN, TypeInfos.CHAR,    false, Caster.identity())
			.append(TypeInfos.BYTE,    TypeInfos.SHORT,   true,  Caster.identity())
			.append(TypeInfos.SHORT,   TypeInfos.INT,     true,  Caster.identity())

			.append(TypeInfos.INT,     TypeInfos.BYTE,    false, Caster.opcode(I2B))
			.append(TypeInfos.INT,     TypeInfos.CHAR,    false, Caster.opcode(I2C))
			.append(TypeInfos.INT,     TypeInfos.SHORT,   false, Caster.opcode(I2S))

			.append(TypeInfos.INT,     TypeInfos.LONG,    true,  Caster.opcode(I2L))
			.append(TypeInfos.INT,     TypeInfos.FLOAT,   true,  Caster.opcode(I2F))
			.append(TypeInfos.INT,     TypeInfos.DOUBLE,  true,  Caster.opcode(I2D))
			.append(TypeInfos.LONG,    TypeInfos.INT,     false, Caster.opcode(L2I))
			.append(TypeInfos.LONG,    TypeInfos.FLOAT,   true,  Caster.opcode(L2F))
			.append(TypeInfos.LONG,    TypeInfos.DOUBLE,  true,  Caster.opcode(L2D))
			.append(TypeInfos.FLOAT,   TypeInfos.INT,     false, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(CastingSupport.class), "F2I", TypeInfos.INT,  TypeInfos.FLOAT)))
			.append(TypeInfos.FLOAT,   TypeInfos.LONG,    false, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(CastingSupport.class), "F2L", TypeInfos.LONG, TypeInfos.FLOAT)))
			.append(TypeInfos.FLOAT,   TypeInfos.DOUBLE,  true,  Caster.opcode(F2D))
			.append(TypeInfos.DOUBLE,  TypeInfos.INT,     false, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(CastingSupport.class), "D2I", TypeInfos.INT,  TypeInfos.DOUBLE)))
			.append(TypeInfos.DOUBLE,  TypeInfos.LONG,    false, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(CastingSupport.class), "D2L", TypeInfos.LONG, TypeInfos.DOUBLE)))
			.append(TypeInfos.DOUBLE,  TypeInfos.FLOAT,   false, Caster.opcode(D2F))

			.append(TypeInfos.INT,     TypeInfos.BOOLEAN, false, Caster.toBoolean(Opcodes.IFNE))
			.append(TypeInfos.LONG,    TypeInfos.BOOLEAN, false, Caster.allOf(Caster.opcode(Opcodes.LCMP), Caster.toBoolean(Opcodes.IFNE)))
			.append(TypeInfos.FLOAT,   TypeInfos.BOOLEAN, false, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(CastingSupport.class), "F2Z", TypeInfos.BOOLEAN, TypeInfos.FLOAT)))
			.append(TypeInfos.DOUBLE,  TypeInfos.BOOLEAN, false, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(CastingSupport.class), "D2Z", TypeInfos.BOOLEAN, TypeInfos.DOUBLE)))
		)
		.append(PopCastProvider.INSTANCE)
	);

	public static final MultiCastProvider BUILTIN_CAST_PROVIDERS = (
		new MultiCastProvider()
		.append(PRIMITIVE_CASTING)
		.append(
			new LookupCastProvider()

			.append(TypeInfos.BYTE,            TypeInfos.BYTE_WRAPPER,    true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(Byte     .class), "valueOf", TypeInfos.   BYTE_WRAPPER, TypeInfos.BYTE   )))
			.append(TypeInfos.SHORT,           TypeInfos.SHORT_WRAPPER,   true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(Short    .class), "valueOf", TypeInfos.  SHORT_WRAPPER, TypeInfos.SHORT  )))
			.append(TypeInfos.INT,             TypeInfos.INT_WRAPPER,     true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(Integer  .class), "valueOf", TypeInfos.    INT_WRAPPER, TypeInfos.INT    )))
			.append(TypeInfos.LONG,            TypeInfos.LONG_WRAPPER,    true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(Long     .class), "valueOf", TypeInfos.   LONG_WRAPPER, TypeInfos.LONG   )))
			.append(TypeInfos.FLOAT,           TypeInfos.FLOAT_WRAPPER,   true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(Float    .class), "valueOf", TypeInfos.  FLOAT_WRAPPER, TypeInfos.FLOAT  )))
			.append(TypeInfos.DOUBLE,          TypeInfos.DOUBLE_WRAPPER,  true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(Double   .class), "valueOf", TypeInfos. DOUBLE_WRAPPER, TypeInfos.DOUBLE )))
			.append(TypeInfos.CHAR,            TypeInfos.CHAR_WRAPPER,    true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(Character.class), "valueOf", TypeInfos.   CHAR_WRAPPER, TypeInfos.CHAR   )))
			.append(TypeInfos.BOOLEAN,         TypeInfos.BOOLEAN_WRAPPER, true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE, TypeInfo.of(Boolean  .class), "valueOf", TypeInfos.BOOLEAN_WRAPPER, TypeInfos.BOOLEAN)))

			.append(TypeInfos.BYTE_WRAPPER,    TypeInfos.BYTE,            true, Caster.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.   BYTE_WRAPPER, "byteValue",    TypeInfos.BYTE)))
			.append(TypeInfos.SHORT_WRAPPER,   TypeInfos.SHORT,           true, Caster.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.  SHORT_WRAPPER, "shortValue",   TypeInfos.SHORT)))
			.append(TypeInfos.INT_WRAPPER,     TypeInfos.INT,             true, Caster.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.    INT_WRAPPER, "intValue",     TypeInfos.INT)))
			.append(TypeInfos.LONG_WRAPPER,    TypeInfos.LONG,            true, Caster.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.   LONG_WRAPPER, "longValue",    TypeInfos.LONG)))
			.append(TypeInfos.FLOAT_WRAPPER,   TypeInfos.FLOAT,           true, Caster.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.  FLOAT_WRAPPER, "floatValue",   TypeInfos.FLOAT)))
			.append(TypeInfos.DOUBLE_WRAPPER,  TypeInfos.DOUBLE,          true, Caster.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos. DOUBLE_WRAPPER, "doubleValue",  TypeInfos.DOUBLE)))
			.append(TypeInfos.CHAR_WRAPPER,    TypeInfos.CHAR,            true, Caster.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.   CHAR_WRAPPER, "charValue",    TypeInfos.CHAR)))
			.append(TypeInfos.BOOLEAN_WRAPPER, TypeInfos.BOOLEAN,         true, Caster.invokeVirtual(method(ACC_PUBLIC | ExtendedOpcodes.ACC_PURE, TypeInfos.BOOLEAN_WRAPPER, "booleanValue", TypeInfos.BOOLEAN)))
		)
		.append(ObjectToPrimitiveProvider.INSTANCE)
		.append(   DirectCastCastProvider.INSTANCE)
		.append(   AconstNullCastProvider.INSTANCE)
		.append(       StringCastProvider.INSTANCE)
	);

	public static InsnTree primitiveCast(InsnTree value, TypeInfo type, CastMode mode) {
		if (value.getTypeInfo().simpleEquals(type)) return value;
		CasterData[] steps = PRIMITIVE_CASTING.search(value.getTypeInfo(), type, mode);
		if (steps == null) return null;
		return new CastInsnTree(value, type, steps);
	}

	public static boolean F2Z(float value) {
		return value != 0.0F && value == value;
	}

	public static boolean D2Z(double value) {
		return value != 0.0D && value == value;
	}

	public static int F2I(float value) {
		int i = (int)(value);
		return i != Integer.MIN_VALUE && ((float)(i)) > value ? i - 1 : i;
	}

	public static long F2L(float value) {
		long l = (long)(value);
		return l != Long.MIN_VALUE && ((float)(l)) > value ? l - 1 : l;
	}

	public static int D2I(double value) {
		int i = (int)(value);
		return i != Integer.MIN_VALUE && ((double)(i)) > value ? i - 1 : i;
	}

	public static long D2L(double value) {
		long l = (long)(value);
		return l != Long.MIN_VALUE && ((double)(l)) > value ? l - 1 : l;
	}

	public static class CasterData {

		public static final ObjectArrayFactory<CasterData> ARRAY_FACTORY = new ObjectArrayFactory<>(CasterData.class);

		public final TypeInfo from, to;
		public final boolean implicit;
		public final Caster caster;

		public CasterData(TypeInfo from, TypeInfo to, boolean implicit, Caster caster) {
			this.from     = from;
			this.to       = to;
			this.implicit = implicit;
			this.caster   = caster;
		}
	}

	public static interface Caster extends BytecodeEmitter {

		public static final ObjectArrayFactory<Caster> ARRAY_FACTORY = new ObjectArrayFactory<>(Caster.class);

		public static final Caster
			ACONST_NULL = opcode(Opcodes.ACONST_NULL),
			ICONST_0    = opcode(Opcodes.ICONST_0),
			LCONST_0    = opcode(Opcodes.LCONST_0),
			FCONST_0    = opcode(Opcodes.FCONST_0),
			DCONST_0    = opcode(Opcodes.DCONST_0),
			POP         = opcode(Opcodes.POP),
			POP2        = opcode(Opcodes.POP2);

		@Override
		public abstract void emitBytecode(MethodCompileContext method);

		public static Caster opcode(int opcode) {
			return method -> method.node.visitInsn(opcode);
		}

		public static Caster invokeStatic(MethodInfo info) {
			return method -> info.emit(method, INVOKESTATIC);
		}

		public static Caster invokeVirtual(MethodInfo info) {
			return method -> info.emit(method, INVOKEVIRTUAL);
		}

		public static Caster identity() {
			return method -> {};
		}

		public static Caster toBoolean(int oneCondition) {
			return method -> {
				Label one = new Label();
				Label end = new Label();
				method.node.visitJumpInsn(oneCondition, one);
				method.node.visitInsn(Opcodes.ICONST_0);
				method.node.visitJumpInsn(GOTO, end);
				method.node.visitLabel(one);
				method.node.visitInsn(Opcodes.ICONST_1);
				method.node.visitLabel(end);
			};
		}

		public static Caster allOf(CasterData... casters) {
			return allOf(
				Arrays
				.stream(casters)
				.map(caster -> caster.caster)
				.toArray(Caster.ARRAY_FACTORY)
			);
		}

		public static Caster allOf(Caster... casters) {
			return switch (casters.length) {
				case 0 -> identity();
				case 1 -> casters[0];
				default -> method -> {
					for (Caster caster : casters) {
						caster.emitBytecode(method);
					}
				};
			};
		}
	}

	public static class ConstantCaster implements Caster {

		public ConstantFactory factory;

		public ConstantCaster(ConstantFactory factory) {
			this.factory = factory;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.factory.variableMethod.emit(method, INVOKESTATIC);
		}
	}

	public static interface CastProvider {

		public static final ObjectArrayFactory<CastProvider> ARRAY_FACTORY = new ObjectArrayFactory<>(CastProvider.class);

		/**
		attempts to create any CasterData's which may be useful in casting from "from" to "to".
		each CasterData returned must match the provided "from" type, but need not match the "to" type.
		this is necessary to support automatic cast chaining.
		for example, when converting from type Integer to type Double,
		a provider may provide Number::doubleValue to convert to type double.
		then, a different caster may provide Double::valueOf to convert to type Double.
		*/
		public abstract Stream<CasterData> provide(TypeInfo from, TypeInfo to);
	}

	public static class LookupCastProvider implements CastProvider {

		public Table<TypeInfo, TypeInfo, CasterData> lookup = HashBasedTable.create();

		public void add(CasterData caster) {
			this.lookup.put(caster.from, caster.to, caster);
		}

		public void add(TypeInfo from, TypeInfo to, boolean implicit, Caster impl) {
			this.lookup.put(from, to, new CasterData(from, to, implicit, impl));
		}

		public LookupCastProvider append(CasterData caster) {
			this.lookup.put(caster.from, caster.to, caster);
			return this;
		}

		public LookupCastProvider append(TypeInfo from, TypeInfo to, boolean implicit, Caster impl) {
			this.lookup.put(from, to, new CasterData(from, to, implicit, impl));
			return this;
		}

		@Override
		public Stream<CasterData> provide(TypeInfo from, TypeInfo to) {
			CasterData direct = this.lookup.get(from, to);
			if (direct != null) return Stream.of(direct);
			//else return all the things we could potentially cast "from" to.
			return from.getAllAssignableTypes().stream().flatMap(newFrom -> this.lookup.row(newFrom).values().stream());
		}
	}

	public static class PopCastProvider implements CastProvider {

		public static final PopCastProvider INSTANCE = new PopCastProvider();

		@Override
		public Stream<CasterData> provide(TypeInfo from, TypeInfo to) {
			if (to.isVoid()) {
				return Stream.of(new CasterData(from, to, true, from.isSingleWidth() ? Caster.POP : Caster.POP2));
			}
			else {
				return Stream.empty();
			}
		}
	}

	public static class AconstNullCastProvider implements CastProvider {

		public static final AconstNullCastProvider INSTANCE = new AconstNullCastProvider();

		@Override
		public Stream<CasterData> provide(TypeInfo from, TypeInfo to) {
			if (from.isVoid() && !to.isVoid()) {
				return Stream.of(new CasterData(from, to, false, switch (to.getSort()) {
					case BOOLEAN, BYTE, CHAR, SHORT, INT -> Caster.ICONST_0;
					case LONG -> Caster.LCONST_0;
					case FLOAT -> Caster.FCONST_0;
					case DOUBLE -> Caster.DCONST_0;
					case OBJECT, ARRAY -> Caster.ACONST_NULL;
					case VOID -> throw new AssertionError("I JUST checked for non-void");
				}));
			}
			else {
				return Stream.empty();
			}
		}
	}

	public static class DirectCastCastProvider implements CastProvider {

		public static final DirectCastCastProvider INSTANCE = new DirectCastCastProvider();

		@Override
		public Stream<CasterData> provide(TypeInfo from, TypeInfo to) {
			if (from.isObject() && to.isObject()) {
				if (from.extendsOrImplements(to)) {
					return Stream.of(new CasterData(from, to, true, Caster.identity()));
				}
				if (to.extendsOrImplements(from) || to.type == ClassType.INTERFACE) {
					return Stream.of(new CasterData(from, to, false, method -> method.node.visitTypeInsn(Opcodes.CHECKCAST, to.getInternalName())));
				}
			}
			return Stream.empty();
		}
	}

	public static class StringCastProvider implements CastProvider {

		public static final StringCastProvider INSTANCE = new StringCastProvider();

		@Override
		public Stream<CasterData> provide(TypeInfo from, TypeInfo to) {
			if (to.equals(TypeInfos.STRING)) {
				if (from.equals(TypeInfos.STRING)) {
					return Stream.of(new CasterData(from, to, true, Caster.identity()));
				}
				return Stream.of(new CasterData(from, to, true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, TypeInfos.STRING, "valueOf", TypeInfos.STRING, from.isPrimitive() ? TypeInfos.widenToInt(from) : TypeInfos.OBJECT))));
			}
			return Stream.empty();
		}
	}

	public static class ObjectToPrimitiveProvider implements CastProvider {

		public static final ObjectToPrimitiveProvider INSTANCE = new ObjectToPrimitiveProvider();

		@Override
		public Stream<CasterData> provide(TypeInfo from, TypeInfo to) {
			if (to.isPrimitiveValue() && from.isObject()) {
				TypeInfo wrapped = TypeInfos.box(to);
				if (wrapped.extendsOrImplements(from)) {
					return Stream.of(new CasterData(from, wrapped, false, method -> method.node.visitTypeInsn(Opcodes.CHECKCAST, wrapped.getInternalName())));
				}
			}
			return Stream.empty();
		}
	}

	public static class MultiCastProvider implements CastProvider {

		public List<CastProvider> providers;

		public MultiCastProvider() {
			this.providers = new ArrayList<>(8);
		}

		public MultiCastProvider(MultiCastProvider from) {
			this.providers = new ArrayList<>(from.providers);
		}

		public void add(CastProvider provider) {
			this.providers.add(provider);
		}

		public MultiCastProvider append(CastProvider provider) {
			this.add(provider);
			return this;
		}

		@Override
		public Stream<CasterData> provide(TypeInfo from, TypeInfo to) {
			return this.providers.stream().flatMap(provider -> provider.provide(from, to));
		}

		public CasterData[] search(TypeInfo from, TypeInfo to, CastMode mode) {
			Map<TypeInfo, CasterData[]> progress = new HashMap<>(8);
			Deque<TypeInfo> todo = new ArrayDeque<>(8);
			progress.put(from, CasterData.ARRAY_FACTORY.empty());
			todo.add(from);
			while (!todo.isEmpty()) {
				TypeInfo current = todo.removeFirst();
				CasterData[] prev = progress.get(current);
				for (Iterator<CasterData> iterator = this.provide(current, to).iterator(); iterator.hasNext();) {
					CasterData next = iterator.next();
					CasterData[] resultArray = ObjectArrays.concat(prev, next);
					if (next.to.equals(to)) {
						if (!mode.implicit || Arrays.stream(resultArray).allMatch(data -> data.implicit)) {
							return resultArray;
						}
					}
					else if (progress.putIfAbsent(next.to, resultArray) == null) {
						todo.addLast(next.to);
					}
				}
			}
			return mode.handleFailure(from, to);
		}
	}
}