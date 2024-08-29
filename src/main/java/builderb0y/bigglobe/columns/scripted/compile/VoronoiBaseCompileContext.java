package builderb0y.bigglobe.columns.scripted.compile;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.scripted.ColumnValueHolder.UnresolvedColumnValueInfo;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareZeroConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.ConditionToBooleanInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.BitwiseAndInsnTree;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class VoronoiBaseCompileContext extends AbstractVoronoiDataCompileContext {

	public VoronoiBaseCompileContext(ColumnCompileContext parent, String name, boolean exportsAnything) {
		super(parent);
		this.flagsIndex = 3;
		this.parent = parent;
		this.mainClass = parent.mainClass.newInnerClass(
			exportsAnything
			? ACC_PUBLIC | ACC_SYNTHETIC | ACC_ABSTRACT
			: ACC_PUBLIC | ACC_SYNTHETIC,
			Type.getInternalName(VoronoiDataBase.class) + "$Generated$Base_" + name + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			type(VoronoiDataBase.class),
			new TypeInfo[0]
		);
		FieldCompileContext columnField = this.mainClass.newField(ACC_PUBLIC | ACC_FINAL, "column", parent.columnType());

		{
			LazyVarInfo column, cell;
			this.constructor = this.mainClass.newMethod(
				ACC_PUBLIC,
				"<init>",
				TypeInfos.VOID,
				column = new LazyVarInfo("column", parent.columnType()),
				cell = new LazyVarInfo("cell", type(VoronoiDiagram2D.Cell.class))
			);
			LazyVarInfo self = new LazyVarInfo("this", this.constructor.clazz.info);
			invokeInstance(
				load(self),
				new MethodInfo(
					ACC_PUBLIC,
					type(VoronoiDataBase.class),
					"<init>",
					TypeInfos.VOID,
					type(VoronoiDiagram2D.Cell.class)
				),
				load(cell)
			)
			.emitBytecode(this.constructor);
			putField(load(self), columnField.info, load(column)).emitBytecode(this.constructor);
		}

		{
			MethodCompileContext column = this.mainClass.newMethod(ACC_PUBLIC, "column", type(ScriptedColumn.class) /* do not use synthetic subclass, because we want to override. */);
			LazyVarInfo self = new LazyVarInfo("this", column.clazz.info);
			return_(
				getField(
					load(self),
					columnField.info
				)
			)
			.emitBytecode(column);
			column.endCode();
		}
	}

	@Override
	public Map<ColumnEntry, ColumnEntryMemory> getMemories() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InsnTree loadColumn() {
		return getField(
			this.loadSelf(),
			new FieldInfo(
				ACC_PUBLIC,
				type(VoronoiDataBase.class),
				"column",
				this.root().columnType()
			)
		);
	}

	@Override
	public InsnTree loadSeed(@Nullable InsnTree salt) {
		return (
			salt != null
			? VoronoiDataBase.INFO.salted_seed(this.loadSelf(), salt)
			: VoronoiDataBase.INFO.unsalted_seed(this.loadSelf())
		);
	}

	@Override
	public FieldInfo flagsField(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void prepareForCompile() {
		if ((this.mainClass.node.access & Opcodes.ACC_ABSTRACT) == 0) {
			this.addHolderMethods();
		}
		super.prepareForCompile();
	}

	public void addHolderMethods() {
		{
			MethodCompileContext id = this.mainClass.newMethod(ACC_PUBLIC, "id", TypeInfos.STRING);
			return_(ldc("")).emitBytecode(id);
			id.endCode();
		}

		{
			MethodCompileContext isColumnValuePresent = this.mainClass.newMethod(ACC_PUBLIC, "isColumnValuePresent", TypeInfos.BOOLEAN, new LazyVarInfo("name", TypeInfos.STRING));
			this.emitSwitchCases(new Int2ObjectAVLTreeMap<>(), Stream.empty(), isColumnValuePresent, PreprocessMethod.IS_COLUMN_VALUE_PRESENT);
		}

		{
			MethodCompileContext getColumnValue = this.mainClass.newMethod(ACC_PUBLIC, "getColumnValue", TypeInfos.OBJECT, new LazyVarInfo("name", TypeInfos.STRING), new LazyVarInfo("y", TypeInfos.INT));
			this.emitSwitchCases(new Int2ObjectAVLTreeMap<>(), Stream.empty(), getColumnValue, PreprocessMethod.GET_COLUMN_VALUE);
		}

		{
			MethodCompileContext setColumnValue = this.mainClass.newMethod(ACC_PUBLIC, "setColumnValue", TypeInfos.VOID, new LazyVarInfo("name", TypeInfos.STRING), new LazyVarInfo("y", TypeInfos.INT), new LazyVarInfo("value", TypeInfos.OBJECT));
			this.emitSwitchCases(new Int2ObjectAVLTreeMap<>(), Stream.empty(), setColumnValue, PreprocessMethod.SET_COLUMN_VALUE);
		}

		{
			MethodCompileContext preComputeColumnValue = this.mainClass.newMethod(ACC_PUBLIC, "preComputeColumnValue", TypeInfos.VOID, new LazyVarInfo("name", TypeInfos.STRING));
			this.emitSwitchCases(new Int2ObjectAVLTreeMap<>(), Stream.empty(), preComputeColumnValue, PreprocessMethod.PRE_COMPUTE_COLUMN_VALUE);
		}

		{
			MethodCompileContext getColumnValues = this.mainClass.newMethod(ACC_PUBLIC, "getColumnValues", type(List.class));
			return_(
				ldc(
					UnresolvedColumnValueInfo.RESOLVE,
					getColumnValues.clazz.newConstant(
						this.preprocessColumnValueInfos(Stream.empty())
						.sorted(Comparator.comparing(UnresolvedColumnValueInfo::name))
						.toArray(UnresolvedColumnValueInfo[]::new),
						type(UnresolvedColumnValueInfo[].class)
					)
				)
			)
			.emitBytecode(getColumnValues);
			getColumnValues.endCode();
		}
	}
}