package builderb0y.bigglobe.columns.scripted.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.dependencies.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.BooleanToConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareZeroConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfInsnTree;
import builderb0y.scripting.bytecode.tree.flow.SwitchInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ConditionToBooleanInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.BitwiseAndInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.IdentityCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class DataCompileContext {

	public DataCompileContext parent;
	public List<DataCompileContext> children;
	public ClassCompileContext mainClass;
	public MethodCompileContext constructor;
	public int flagsIndex;

	public DataCompileContext(DataCompileContext parent) {
		this.parent = parent;
		this.children = new ArrayList<>(8);
		if (parent != null) parent.children.add(this);
	}

	public abstract Map<ColumnEntry, ColumnEntryMemory> getMemories();

	public TypeInfo selfType() {
		return this.mainClass.info;
	}

	public ColumnCompileContext root() {
		DataCompileContext context = this;
		for (DataCompileContext next; (next = context.parent) != null; context = next);
		return (ColumnCompileContext)(context);
	}

	public InsnTree loadSelf() {
		return load("this", this.mainClass.info);
	}

	public abstract InsnTree loadColumn();

	public abstract InsnTree loadSeed(InsnTree salt);

	public abstract FieldInfo flagsField(int index);

	public void prepareForCompile() {
		this.constructor.node.visitInsn(RETURN);
		this.constructor.endCode();
		this.children.forEach(DataCompileContext::prepareForCompile);
	}

	public static String internalName(Identifier selfID, int fieldIndex) {
		StringBuilder builder = (
			new StringBuilder(selfID.getNamespace().length() + selfID.getPath().length() + 16)
			.append(selfID.getNamespace())
			.append('_')
			.append(selfID.getPath())
		);
		for (int index = 0, length = builder.length(); index < length; index++) {
			char old = builder.charAt(index);
			if (!((old >= 'a' && old <= 'z') || (old >= '0' && old <= '9'))) {
				builder.setCharAt(index, '_');
			}
		}
		return builder.append('_').append(fieldIndex).toString();
	}

	public static int flagsFieldBitmask(int index) {
		//note: *because java*, this is equivalent to 1 << (index & 31).
		//this is one of the very few places where such a weird rule is actually useful.
		return 1 << index;
	}

	public void setMethodCode(
		MethodCompileContext method,
		ScriptUsage script,
		boolean includeY,
		MutableDependencyView dependencies
	)
	throws ScriptParsingException {
		new ScriptColumnEntryParser(script, this.mainClass, method)
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.addEnvironment(MinecraftScriptEnvironment.create())
		.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
		.addEnvironment(ScriptedColumn.baseEnvironment(this.loadColumn()))
		.configureEnvironment((MutableScriptEnvironment environment) -> {
			if (includeY) {
				environment.addVariableLoad("y", TypeInfos.INT);
				this.root().registry.setupInternalEnvironment(environment, this, load("y", TypeInfos.INT), dependencies);
			}
			else {
				this.root().registry.setupInternalEnvironment(environment, this, null, dependencies);
			}
		})
		.parseEntireInput()
		.emitBytecode(method);
		method.endCode();
	}

	public void addGenericGetterAndPreComputer(Map<ColumnEntry, ColumnEntryMemory> memoryMap) {
		{
			MethodCompileContext isColumnValuePresent = this.mainClass.newMethod(ACC_PUBLIC, "isColumnValuePresent", TypeInfos.BOOLEAN, new LazyVarInfo("name", TypeInfos.STRING));
			Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
			Stream.Builder<ColumnEntryMemory> streamBuilder = Stream.builder();
			for (Map.Entry<ColumnEntry, ColumnEntryMemory> entry : memoryMap.entrySet()) {
				if (entry.getKey().hasField()) {
					String id = entry.getValue().getTyped(ColumnEntryMemory.ACCESSOR_ID).toString();
					int flagIndex = entry.getValue().getTyped(ColumnEntryMemory.FLAGS_INDEX);
					FieldInfo field = this.flagsField(flagIndex);
					int mask = flagsFieldBitmask(flagIndex);
					InsnTree case_ = new ConditionToBooleanInsnTree(
						new IntCompareZeroConditionTree(
							new BitwiseAndInsnTree(
								getField(
									this.loadSelf(),
									field
								),
								ldc(mask),
								IAND
							),
							IFNE
						)
					);
					case_ = guard(case_, id);
					cases.merge(id.hashCode(), case_, InsnTrees::seq);
					streamBuilder.accept(entry.getValue());
				}
			}
			this.emitSwitchCases(cases, streamBuilder.build(), isColumnValuePresent);
		}

		{
			MethodCompileContext getColumnValue = this.mainClass.newMethod(ACC_PUBLIC, "getColumnValue", TypeInfos.OBJECT, new LazyVarInfo("name", TypeInfos.STRING), new LazyVarInfo("y", TypeInfos.INT));
			Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
			for (Map.Entry<ColumnEntry, ColumnEntryMemory> entry : memoryMap.entrySet()) {
				InsnTree getter = entry.getKey().createGenericGetter(entry.getValue(), this);
				String id = entry.getValue().getTyped(ColumnEntryMemory.ACCESSOR_ID).toString();
				getter = toObject(getter, id);
				getter = guard(getter, id);
				cases.merge(id.hashCode(), getter, InsnTrees::seq);
			}
			this.emitSwitchCases(cases, memoryMap.values().stream(), getColumnValue);
		}

		{
			MethodCompileContext setColumnValue = this.mainClass.newMethod(ACC_PUBLIC, "setColumnValue", TypeInfos.VOID, new LazyVarInfo("name", TypeInfos.STRING), new LazyVarInfo("y", TypeInfos.INT), new LazyVarInfo("value", TypeInfos.OBJECT));
			Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
			Stream.Builder<ColumnEntryMemory> streamBuilder = Stream.builder();
			for (Map.Entry<ColumnEntry, ColumnEntryMemory> entry : memoryMap.entrySet()) {
				if (entry.getKey().isSettable()) {
					String id = entry.getValue().getTyped(ColumnEntryMemory.ACCESSOR_ID).toString();
					InsnTree setter = fromObject(load("value", TypeInfos.OBJECT), entry.getValue().getTyped(ColumnEntryMemory.ACCESS_CONTEXT).exposedType(), id);
					setter = entry.getKey().createGenericSetter(entry.getValue(), this, setter);
					setter = guard(setter, id);
					cases.merge(id.hashCode(), setter, InsnTrees::seq);
					streamBuilder.accept(entry.getValue());
				}
			}
			this.emitSwitchCases(cases, streamBuilder.build(), setColumnValue);
		}

		{
			MethodCompileContext preComputeColumnValue = this.mainClass.newMethod(ACC_PUBLIC, "preComputeColumnValue", TypeInfos.VOID, new LazyVarInfo("name", TypeInfos.STRING));
			Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
			Stream.Builder<ColumnEntryMemory> streamBuilder = Stream.builder();
			for (Map.Entry<ColumnEntry, ColumnEntryMemory> entry : memoryMap.entrySet()) {
				if (entry.getKey().hasField()) {
					InsnTree preComputer = entry.getKey().createGenericPreComputer(entry.getValue(), this);
					String id = entry.getValue().getTyped(ColumnEntryMemory.ACCESSOR_ID).toString();
					if (!preComputer.getTypeInfo().isVoid()) {
						throw new IllegalStateException("Column value " + id + " produced a non-void pre-computer: " + preComputer.describe());
					}
					preComputer = guard(preComputer, id);
					cases.merge(id.hashCode(), preComputer, InsnTrees::seq);
					streamBuilder.accept(entry.getValue());
				}
			}
			this.emitSwitchCases(cases, streamBuilder.build(), preComputeColumnValue);
		}
	}

	public void emitSwitchCases(Int2ObjectSortedMap<InsnTree> cases, Stream<ColumnEntryMemory> memoryMap, MethodCompileContext method) {
		if (!cases.isEmpty()) {
			new SwitchInsnTree(
				invokeInstance(
					load("name", TypeInfos.STRING),
					MethodInfo.getMethod(String.class, "hashCode")
				),
				cases,
				TypeInfos.VOID
			)
			.emitBytecode(method);
		}
		this.errorOnInvalidColumnValue(memoryMap).emitBytecode(method);
		method.endCode();
	}

	public static InsnTree guard(InsnTree tree, String id) {
		return new IfInsnTree(
			new BooleanToConditionTree(
				invokeInstance(
					ldc(id),
					MethodInfo.getMethod(String.class, "equals"),
					load("name", TypeInfos.STRING)
				)
			),
			return_(tree)
		);
	}

	public InsnTree errorOnInvalidColumnValue(Stream<ColumnEntryMemory> memories) {
		return throw_(
			newInstance(
				MethodInfo.findConstructor(IllegalArgumentException.class, String.class),
				concat(
					memories
					.map((ColumnEntryMemory memory) -> memory.getTyped(ColumnEntryMemory.ACCESSOR_ID).toString())
					.sorted()
					.collect(Collectors.joining(", ", "Invalid column value \u0001 on \u0001. Valid column values are: ", "")),
					load("name", TypeInfos.STRING),
					invokeInstance(
						this.loadSelf(),
						new MethodInfo(ACC_PUBLIC, this.selfType(), "getClass", TypeInfos.CLASS)
					)
				)
			)
		);
	}

	public static InsnTree toObject(InsnTree getter, String id) {
		return switch (getter.getTypeInfo().getSort()) {
			case VOID    -> throw new IllegalStateException("Column value " + id + " produced a void-typed getter: " + getter.describe());
			case BOOLEAN -> new IdentityCastInsnTree(invokeStatic(MethodInfo.findMethod(Boolean  .class, "valueOf", Boolean  .class, boolean.class), getter), TypeInfos.OBJECT);
			case BYTE    -> new IdentityCastInsnTree(invokeStatic(MethodInfo.findMethod(Byte     .class, "valueOf", Byte     .class, byte   .class), getter), TypeInfos.OBJECT);
			case CHAR    -> new IdentityCastInsnTree(invokeStatic(MethodInfo.findMethod(Character.class, "valueOf", Character.class, char   .class), getter), TypeInfos.OBJECT);
			case SHORT   -> new IdentityCastInsnTree(invokeStatic(MethodInfo.findMethod(Short    .class, "valueOf", Short    .class, short  .class), getter), TypeInfos.OBJECT);
			case INT     -> new IdentityCastInsnTree(invokeStatic(MethodInfo.findMethod(Integer  .class, "valueOf", Integer  .class, int    .class), getter), TypeInfos.OBJECT);
			case LONG    -> new IdentityCastInsnTree(invokeStatic(MethodInfo.findMethod(Long     .class, "valueOf", Long     .class, long   .class), getter), TypeInfos.OBJECT);
			case FLOAT   -> new IdentityCastInsnTree(invokeStatic(MethodInfo.findMethod(Float    .class, "valueOf", Float    .class, float  .class), getter), TypeInfos.OBJECT);
			case DOUBLE  -> new IdentityCastInsnTree(invokeStatic(MethodInfo.findMethod(Double   .class, "valueOf", Double   .class, double .class), getter), TypeInfos.OBJECT);
			case OBJECT  -> getter.getTypeInfo().equals(TypeInfos.OBJECT) ? getter : new IdentityCastInsnTree(getter, TypeInfos.OBJECT);
			case ARRAY   -> new IdentityCastInsnTree(getter, TypeInfos.OBJECT);
		};
	}

	public static InsnTree fromObject(InsnTree value, TypeInfo type, String id) {
		return switch (type.getSort()) {
			case VOID -> throw new IllegalArgumentException("Attempt to cast " + id + " (" + value.describe() + ") to void.");
			case BOOLEAN -> invokeInstance(new DirectCastInsnTree(value, TypeInfos.BOOLEAN_WRAPPER), MethodInfo.getMethod(Boolean.class, "booleanValue"));
			case BYTE -> invokeInstance(new DirectCastInsnTree(value, TypeInfos.BYTE_WRAPPER), MethodInfo.getMethod(Byte.class, "byteValue"));
			case CHAR -> invokeInstance(new DirectCastInsnTree(value, TypeInfos.CHAR_WRAPPER), MethodInfo.getMethod(Character.class, "charValue"));
			case SHORT -> invokeInstance(new DirectCastInsnTree(value, TypeInfos.SHORT_WRAPPER), MethodInfo.getMethod(Short.class, "shortValue"));
			case INT -> invokeInstance(new DirectCastInsnTree(value, TypeInfos.INT_WRAPPER), MethodInfo.getMethod(Integer.class, "intValue"));
			case LONG -> invokeInstance(new DirectCastInsnTree(value, TypeInfos.LONG_WRAPPER), MethodInfo.getMethod(Long.class, "longValue"));
			case FLOAT -> invokeInstance(new DirectCastInsnTree(value, TypeInfos.FLOAT_WRAPPER), MethodInfo.getMethod(Float.class, "floatValue"));
			case DOUBLE -> invokeInstance(new DirectCastInsnTree(value, TypeInfos.DOUBLE_WRAPPER), MethodInfo.getMethod(Double.class, "doubleValue"));
			case OBJECT -> new DirectCastInsnTree(value, type);
			case ARRAY -> throw new IllegalArgumentException("Attempt to cast " + id + " (" + value.describe() + ") to array.");
		};
	}
}